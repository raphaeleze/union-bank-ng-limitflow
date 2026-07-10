package com.limitflow.backend.application.support;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.application.otp.OtpService;
import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.exception.ValidationException;
import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.limitrequest.RiskLevel;
import com.limitflow.backend.domain.notification.NotificationType;
import com.limitflow.backend.domain.support.SupportNote;
import com.limitflow.backend.domain.support.SupportNoteRepository;
import com.limitflow.backend.domain.user.Role;
import com.limitflow.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportReviewService {

    private static final List<RequestStatus> REVIEWABLE = List.of(RequestStatus.UNDER_REVIEW);

    private final LimitRequestRepository limitRequestRepository;
    private final AccountRepository accountRepository;
    private final SupportNoteRepository supportNoteRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final OtpService otpService;

    /** Support agents see MEDIUM-risk requests; managers see the HIGH-risk escalations. */
    public List<LimitRequest> queueFor(Role role) {
        RiskLevel scope = role == Role.MANAGER ? RiskLevel.HIGH : RiskLevel.MEDIUM;
        return limitRequestRepository.findByRiskLevelAndStatusInOrderByCreatedAtAsc(scope, REVIEWABLE);
    }

    /** Unlike {@link #reviewable}, this isn't limited to UNDER_REVIEW — staff can look
     * up a request's detail regardless of whether it's already been decided. */
    public LimitRequest getForReview(UUID requestId) {
        return limitRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Limit request not found"));
    }

    public LimitRequest approve(User staff, UUID requestId, String note) {
        LimitRequest limitRequest = reviewable(requestId);
        Account account = limitRequest.getAccount();
        account.applyNewLimit(limitRequest.getRequestedLimit());
        accountRepository.save(account);

        limitRequest.setResolvedBy(staff);
        limitRequest.transitionTo(RequestStatus.APPROVED);
        limitRequestRepository.save(limitRequest);

        recordNote(staff, limitRequest, note);
        auditService.record(staff, "MANUAL_APPROVED", "LimitRequest", limitRequest.getId().toString());
        notificationService.send(account.getUser(), NotificationType.LIMIT_APPROVED, "Limit increased",
                "Your daily transfer limit is now " + limitRequest.getRequestedLimit());
        return limitRequest;
    }

    public LimitRequest reject(User staff, UUID requestId, String note) {
        LimitRequest limitRequest = reviewable(requestId);
        limitRequest.setResolvedBy(staff);
        limitRequest.transitionTo(RequestStatus.REJECTED);
        limitRequestRepository.save(limitRequest);

        recordNote(staff, limitRequest, note);
        auditService.record(staff, "MANUAL_REJECTED", "LimitRequest", limitRequest.getId().toString());
        notificationService.send(limitRequest.getAccount().getUser(), NotificationType.LIMIT_REJECTED,
                "Request declined", note != null && !note.isBlank() ? note : "Your limit increase request was declined.");
        return limitRequest;
    }

    public LimitRequest requestAdditionalVerification(User staff, UUID requestId, String note) {
        LimitRequest limitRequest = reviewable(requestId);
        limitRequest.transitionTo(RequestStatus.OTP_PENDING);
        limitRequestRepository.save(limitRequest);

        recordNote(staff, limitRequest, note);
        auditService.record(staff, "VERIFICATION_REQUESTED", "LimitRequest", limitRequest.getId().toString());

        String code = otpService.issue(limitRequest);
        notificationService.send(limitRequest.getAccount().getUser(), NotificationType.VERIFICATION_REQUESTED,
                "Additional verification needed", "We need to re-verify this request. Your new code is " + code + ".");
        return limitRequest;
    }

    public SupportNote addStaffNote(User staff, UUID requestId, String note) {
        LimitRequest limitRequest = limitRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Limit request not found"));
        SupportNote supportNote = recordNote(staff, limitRequest, note);
        if (supportNote == null) {
            throw new ValidationException("Note text is required");
        }
        return supportNote;
    }

    public List<SupportNote> notesFor(UUID requestId) {
        return supportNoteRepository.findByLimitRequestIdOrderByCreatedAtAsc(requestId);
    }

    private SupportNote recordNote(User staff, LimitRequest limitRequest, String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        SupportNote supportNote = supportNoteRepository.save(new SupportNote(limitRequest, staff, note));
        notificationService.send(limitRequest.getAccount().getUser(), NotificationType.SUPPORT_COMMENT,
                "New message from support", note);
        return supportNote;
    }

    private LimitRequest reviewable(UUID requestId) {
        LimitRequest limitRequest = limitRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Limit request not found"));
        if (limitRequest.getStatus() != RequestStatus.UNDER_REVIEW) {
            throw new ValidationException("Request is not awaiting review");
        }
        return limitRequest;
    }
}
