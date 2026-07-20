package com.limitflow.backend.application.limitrequest;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.limitrequest.risk.RiskContext;
import com.limitflow.backend.application.limitrequest.risk.RiskEngine;
import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.application.otp.OtpDeliveryService;
import com.limitflow.backend.application.otp.OtpService;
import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.ForbiddenException;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.exception.ValidationException;
import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.limitrequest.RiskLevel;
import com.limitflow.backend.domain.notification.NotificationType;
import com.limitflow.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LimitRequestService {

    private final LimitRequestRepository limitRequestRepository;
    private final AccountRepository accountRepository;
    private final OtpService otpService;
    private final OtpDeliveryService otpDeliveryService;
    private final RiskEngine riskEngine;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public LimitRequest submitRequest(User customer, UUID accountId, BigDecimal requestedLimit,
                                       String reason, boolean knownDevice) {
        Account account = ownedAccount(customer, accountId);

        if (requestedLimit.compareTo(account.getDailyLimit()) <= 0) {
            throw new ValidationException("Requested limit must be greater than the current limit");
        }

        if (limitRequestRepository.existsByAccountIdAndStatusIn(account.getId(), RequestStatus.ACTIVE)) {
            throw new ValidationException("You already have a limit increase request in progress");
        }

        LimitRequest limitRequest = new LimitRequest(account, account.getDailyLimit(), requestedLimit, reason, knownDevice);
        limitRequest.transitionTo(RequestStatus.OTP_PENDING);
        limitRequest = limitRequestRepository.save(limitRequest);

        auditService.record(customer, "LIMIT_REQUESTED", "LimitRequest", limitRequest.getId().toString());
        sendOtp(customer, limitRequest);
        return limitRequest;
    }

    public LimitRequest verifyOtp(User customer, UUID requestId, String code) {
        LimitRequest limitRequest = ownedRequest(customer, requestId);
        requireStatus(limitRequest, RequestStatus.OTP_PENDING);

        if (!otpService.verify(limitRequest, code)) {
            throw new ValidationException("Invalid or expired verification code");
        }

        limitRequest.setOtpVerifiedAt(Instant.now());
        limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);
        limitRequestRepository.save(limitRequest);
        auditService.record(customer, "OTP_VERIFIED", "LimitRequest", limitRequest.getId().toString());
        return limitRequest;
    }

    @Transactional
    public LimitRequest verifyBiometric(User customer, UUID requestId, boolean success) {
        LimitRequest limitRequest = ownedRequest(customer, requestId);
        requireStatus(limitRequest, RequestStatus.BIOMETRIC_PENDING);

        if (!success) {
            throw new ValidationException("Biometric confirmation failed");
        }

        limitRequest.setBiometricVerifiedAt(Instant.now());
        limitRequestRepository.save(limitRequest);
        auditService.record(customer, "BIOMETRIC_VERIFIED", "LimitRequest", limitRequest.getId().toString());
        notificationService.send(customer, NotificationType.VERIFICATION_COMPLETED, "Verification complete",
                "Identity verification complete. We're assessing your request now.");

        return assessRisk(customer, limitRequest);
    }

    public List<LimitRequest> history(User customer, UUID accountId) {
        Account account = ownedAccount(customer, accountId);
        return limitRequestRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
    }

    public LimitRequest get(User requester, UUID requestId) {
        // Reached only via GET /api/limits/{id}, which is customer-only (see
        // LimitRequestController's class-level @PreAuthorize) — staff use the separate
        // GET /api/support/requests/{id}, which has no ownership check of its own.
        return ownedRequest(requester, requestId);
    }

    private LimitRequest assessRisk(User customer, LimitRequest limitRequest) {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        long recentRequests = limitRequestRepository.countByAccountIdAndCreatedAtAfter(
                limitRequest.getAccount().getId(), since);
        boolean suspiciousActivity = recentRequests > 2;

        RiskContext context = new RiskContext(
                limitRequest.getCurrentLimit(),
                limitRequest.getRequestedLimit(),
                limitRequest.isKnownDevice(),
                suspiciousActivity);
        RiskLevel risk = riskEngine.assess(context);
        limitRequest.setRiskLevel(risk);
        auditService.record(customer, "RISK_ASSESSED", "LimitRequest", limitRequest.getId().toString(), risk.name());

        if (risk == RiskLevel.LOW) {
            approveAutomatically(customer, limitRequest);
        } else {
            limitRequest.transitionTo(RequestStatus.UNDER_REVIEW);
            limitRequestRepository.save(limitRequest);
            notificationService.send(customer, NotificationType.VERIFICATION_COMPLETED, "Under review",
                    "Your request needs a quick manual review. We'll notify you as soon as it's decided.");
        }
        return limitRequest;
    }

    private void approveAutomatically(User customer, LimitRequest limitRequest) {
        Account account = limitRequest.getAccount();
        account.applyNewLimit(limitRequest.getRequestedLimit());
        accountRepository.save(account);

        limitRequest.transitionTo(RequestStatus.APPROVED);
        limitRequestRepository.save(limitRequest);

        auditService.record(customer, "LIMIT_APPROVED", "LimitRequest", limitRequest.getId().toString());
        notificationService.send(customer, NotificationType.LIMIT_APPROVED, "Limit increased",
                "Your daily transfer limit is now " + limitRequest.getRequestedLimit());
    }

    private void sendOtp(User customer, LimitRequest limitRequest) {
        String code = otpService.issue(limitRequest);
        otpDeliveryService.deliver(customer, code);
        notificationService.send(customer, NotificationType.OTP_SENT, "OTP sent",
                "Your verification code is " + code + ". It expires in 5 minutes. "
                        + "(Demo mode: shown here instead of SMS.)");
    }

    private Account ownedAccount(User customer, UUID accountId) {
        return accountRepository.findById(accountId)
                .filter(account -> account.getUser().getId().equals(customer.getId()))
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private LimitRequest ownedRequest(User customer, UUID requestId) {
        LimitRequest limitRequest = limitRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Limit request not found"));
        if (!limitRequest.getAccount().getUser().getId().equals(customer.getId())) {
            throw new ForbiddenException("You cannot act on this request");
        }
        return limitRequest;
    }

    private void requireStatus(LimitRequest limitRequest, RequestStatus expected) {
        if (limitRequest.getStatus() != expected) {
            throw new ValidationException("Request is not awaiting " + expected);
        }
    }
}
