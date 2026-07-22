package com.limitflow.backend.application.limitrequest;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.limitrequest.risk.RiskContext;
import com.limitflow.backend.application.limitrequest.risk.RiskEngine;
import com.limitflow.backend.application.notification.NairaFormat;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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

    public Mono<LimitRequest> submitRequest(User customer, UUID accountId, BigDecimal requestedLimit,
                                             String reason, boolean knownDevice) {
        return ownedAccount(customer, accountId)
                .flatMap(account -> {
                    if (requestedLimit.compareTo(account.getDailyLimit()) <= 0) {
                        return Mono.<LimitRequest>error(
                                new ValidationException("Requested limit must be greater than the current limit"));
                    }
                    return limitRequestRepository.existsByAccountIdAndStatusIn(account.getId(), RequestStatus.ACTIVE)
                            .flatMap(hasActive -> {
                                if (hasActive) {
                                    return Mono.<LimitRequest>error(new ValidationException(
                                            "You already have a limit increase request in progress"));
                                }
                                LimitRequest limitRequest = new LimitRequest(account.getId(), account.getDailyLimit(),
                                        requestedLimit, reason, knownDevice);
                                limitRequest.transitionTo(RequestStatus.OTP_PENDING);
                                return limitRequestRepository.save(limitRequest);
                            });
                })
                .flatMap(limitRequest -> auditService.record(customer, "LIMIT_REQUESTED", "LimitRequest",
                                limitRequest.getId().toString())
                        .then(sendOtp(customer, limitRequest))
                        .thenReturn(limitRequest));
    }

    public Mono<LimitRequest> verifyOtp(User customer, UUID requestId, String code) {
        return ownedRequest(customer, requestId)
                .flatMap(limitRequest -> {
                    requireStatus(limitRequest, RequestStatus.OTP_PENDING);
                    return otpService.verify(limitRequest, code)
                            .flatMap(verified -> {
                                if (!verified) {
                                    return Mono.<LimitRequest>error(
                                            new ValidationException("Invalid or expired verification code"));
                                }
                                limitRequest.setOtpVerifiedAt(Instant.now());
                                limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);
                                return limitRequestRepository.save(limitRequest);
                            });
                })
                .flatMap(limitRequest -> auditService.record(customer, "OTP_VERIFIED", "LimitRequest",
                                limitRequest.getId().toString())
                        .thenReturn(limitRequest));
    }

    /** Names the R2DBC manager explicitly: spring-boot-starter-jdbc (present for Flyway) and
     * spring-boot-starter-data-r2dbc both auto-configure a TransactionManager bean, so an
     * unqualified {@code @Transactional} fails to start up with "expected single matching bean
     * but found 2". */
    @Transactional("connectionFactoryTransactionManager")
    public Mono<LimitRequest> verifyBiometric(User customer, UUID requestId, boolean success) {
        return ownedRequest(customer, requestId)
                .flatMap(limitRequest -> {
                    requireStatus(limitRequest, RequestStatus.BIOMETRIC_PENDING);
                    if (!success) {
                        return Mono.<LimitRequest>error(new ValidationException("Biometric confirmation failed"));
                    }
                    limitRequest.setBiometricVerifiedAt(Instant.now());
                    return limitRequestRepository.save(limitRequest);
                })
                .flatMap(limitRequest -> auditService.record(customer, "BIOMETRIC_VERIFIED", "LimitRequest",
                                limitRequest.getId().toString())
                        .then(notificationService.send(customer.getId(), NotificationType.VERIFICATION_COMPLETED,
                                "Verification complete", "Identity verification complete. We're assessing your request now."))
                        .then(assessRisk(customer, limitRequest)));
    }

    public Mono<LimitRequest> cancel(User customer, UUID requestId) {
        return ownedRequest(customer, requestId)
                .flatMap(limitRequest -> {
                    if (!RequestStatus.ACTIVE.contains(limitRequest.getStatus())) {
                        return Mono.error(new ValidationException("This request can no longer be cancelled"));
                    }
                    limitRequest.transitionTo(RequestStatus.CANCELLED);
                    return limitRequestRepository.save(limitRequest);
                })
                .flatMap(limitRequest -> auditService.record(customer, "REQUEST_CANCELLED", "LimitRequest",
                                limitRequest.getId().toString())
                        .thenReturn(limitRequest));
    }

    public Flux<LimitRequest> history(User customer, UUID accountId) {
        return ownedAccount(customer, accountId)
                .flatMapMany(account -> limitRequestRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()));
    }

    public Mono<LimitRequest> get(User requester, UUID requestId) {
        // Reached only via GET /api/limits/{id}, which is customer-only (see
        // LimitRequestController's class-level @PreAuthorize) — staff use the separate
        // GET /api/support/requests/{id}, which has no ownership check of its own.
        return ownedRequest(requester, requestId);
    }

    private Mono<LimitRequest> assessRisk(User customer, LimitRequest limitRequest) {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        return limitRequestRepository.countByAccountIdAndCreatedAtAfter(limitRequest.getAccountId(), since)
                .flatMap(recentRequests -> {
                    boolean suspiciousActivity = recentRequests > 2;
                    RiskContext context = new RiskContext(
                            limitRequest.getCurrentLimit(),
                            limitRequest.getRequestedLimit(),
                            limitRequest.isKnownDevice(),
                            suspiciousActivity);
                    RiskLevel risk = riskEngine.assess(context);
                    limitRequest.setRiskLevel(risk);

                    Mono<LimitRequest> afterAudit = auditService.record(customer, "RISK_ASSESSED", "LimitRequest",
                                    limitRequest.getId().toString(), risk.name())
                            .thenReturn(limitRequest);

                    if (risk == RiskLevel.LOW) {
                        return afterAudit.flatMap(lr -> approveAutomatically(customer, lr));
                    }
                    return afterAudit.flatMap(lr -> {
                        lr.transitionTo(RequestStatus.UNDER_REVIEW);
                        return limitRequestRepository.save(lr)
                                .flatMap(saved -> notificationService.send(customer.getId(),
                                                NotificationType.VERIFICATION_COMPLETED, "Under review",
                                                "Your request needs a quick manual review. We'll notify you as soon as it's decided.")
                                        .thenReturn(saved));
                    });
                });
    }

    private Mono<LimitRequest> approveAutomatically(User customer, LimitRequest limitRequest) {
        return accountRepository.findById(limitRequest.getAccountId())
                .flatMap(account -> {
                    account.applyNewLimit(limitRequest.getRequestedLimit());
                    return accountRepository.save(account);
                })
                .then(Mono.defer(() -> {
                    limitRequest.transitionTo(RequestStatus.APPROVED);
                    return limitRequestRepository.save(limitRequest);
                }))
                .flatMap(saved -> auditService.record(customer, "LIMIT_APPROVED", "LimitRequest", saved.getId().toString())
                        .then(notificationService.send(customer.getId(), NotificationType.LIMIT_APPROVED, "Limit increased",
                                "Your daily transfer limit is now " + NairaFormat.format(saved.getRequestedLimit())))
                        .thenReturn(saved));
    }

    private Mono<Void> sendOtp(User customer, LimitRequest limitRequest) {
        return otpService.issue(limitRequest)
                .flatMap(code -> otpDeliveryService.deliver(customer, code)
                        .then(notificationService.send(customer.getId(), NotificationType.OTP_SENT, "OTP sent",
                                "Your verification code is " + code + ". It expires in 5 minutes. "
                                        + "(Demo mode: shown here instead of SMS.)")))
                .then();
    }

    private Mono<Account> ownedAccount(User customer, UUID accountId) {
        return accountRepository.findById(accountId)
                .filter(account -> account.getUserId().equals(customer.getId()))
                .switchIfEmpty(Mono.error(new NotFoundException("Account not found")));
    }

    private Mono<LimitRequest> ownedRequest(User customer, UUID requestId) {
        return limitRequestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new NotFoundException("Limit request not found")))
                .flatMap(limitRequest -> accountRepository.findById(limitRequest.getAccountId())
                        .flatMap(account -> {
                            if (!account.getUserId().equals(customer.getId())) {
                                return Mono.<LimitRequest>error(new ForbiddenException("You cannot act on this request"));
                            }
                            return Mono.just(limitRequest);
                        }));
    }

    private void requireStatus(LimitRequest limitRequest, RequestStatus expected) {
        if (limitRequest.getStatus() != expected) {
            throw new ValidationException("Request is not awaiting " + expected);
        }
    }
}
