package com.limitflow.backend.application.support;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.notification.NotificationService;
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
import com.limitflow.backend.domain.support.SupportNote;
import com.limitflow.backend.domain.support.SupportNoteRepository;
import com.limitflow.backend.domain.user.Role;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.domain.user.UserRepository;
import com.limitflow.backend.presentation.dto.support.SupportNoteResponse;
import com.limitflow.backend.presentation.dto.support.SupportQueueItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportReviewService {

    private static final List<RequestStatus> REVIEWABLE = List.of(RequestStatus.UNDER_REVIEW);

    private final LimitRequestRepository limitRequestRepository;
    private final AccountRepository accountRepository;
    private final SupportNoteRepository supportNoteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final OtpService otpService;

    /** Support agents see MEDIUM-risk requests; managers see the HIGH-risk escalations. */
    public Flux<SupportQueueItemResponse> queueFor(Role role) {
        return limitRequestRepository.findByRiskLevelAndStatusInOrderByCreatedAtAsc(scopeFor(role), REVIEWABLE)
                .collectList()
                .flatMapMany(requests -> accountRepository.findAllById(
                                requests.stream().map(LimitRequest::getAccountId).distinct().toList())
                        .collectMap(Account::getId, Account::getUserId)
                        .flatMapMany(ownerIdByAccountId -> userRepository.findAllById(
                                        ownerIdByAccountId.values().stream().distinct().toList())
                                .collectMap(User::getId, User::fullName)
                                .flatMapMany(nameByUserId -> Flux.fromIterable(requests)
                                        .map(request -> SupportQueueItemResponse.from(request,
                                                nameByUserId.get(ownerIdByAccountId.get(request.getAccountId())))))));
    }

    /** Unlike {@link #reviewable}, this isn't limited to UNDER_REVIEW — staff can look
     * up a request's detail regardless of whether it's already been decided. */
    public Mono<LimitRequest> getForReview(UUID requestId) {
        return limitRequestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new NotFoundException("Limit request not found")));
    }

    @Transactional
    public Mono<LimitRequest> approve(User staff, UUID requestId, String note) {
        return reviewable(staff, requestId)
                .flatMap(limitRequest -> accountRepository.findById(limitRequest.getAccountId())
                        .flatMap(account -> {
                            account.applyNewLimit(limitRequest.getRequestedLimit());
                            return accountRepository.save(account);
                        })
                        .flatMap(account -> {
                            limitRequest.setResolvedByUserId(staff.getId());
                            limitRequest.transitionTo(RequestStatus.APPROVED);
                            return limitRequestRepository.save(limitRequest)
                                    .flatMap(saved -> recordNote(staff, saved, account.getUserId(), note)
                                            .then(auditService.record(staff, "MANUAL_APPROVED", "LimitRequest",
                                                    saved.getId().toString()))
                                            .then(notificationService.send(account.getUserId(),
                                                    NotificationType.LIMIT_APPROVED, "Limit increased",
                                                    "Your daily transfer limit is now " + saved.getRequestedLimit()))
                                            .thenReturn(saved));
                        }));
    }

    public Mono<LimitRequest> reject(User staff, UUID requestId, String note) {
        return reviewable(staff, requestId)
                .flatMap(limitRequest -> {
                    limitRequest.setResolvedByUserId(staff.getId());
                    limitRequest.transitionTo(RequestStatus.REJECTED);
                    return limitRequestRepository.save(limitRequest);
                })
                .flatMap(saved -> accountOwnerIdOf(saved)
                        .flatMap(ownerId -> recordNote(staff, saved, ownerId, note)
                                .then(auditService.record(staff, "MANUAL_REJECTED", "LimitRequest", saved.getId().toString()))
                                .then(notificationService.send(ownerId, NotificationType.LIMIT_REJECTED, "Request declined",
                                        note != null && !note.isBlank() ? note : "Your limit increase request was declined."))
                                .thenReturn(saved)));
    }

    public Mono<LimitRequest> requestAdditionalVerification(User staff, UUID requestId, String note) {
        return reviewable(staff, requestId)
                .flatMap(limitRequest -> {
                    limitRequest.transitionTo(RequestStatus.OTP_PENDING);
                    return limitRequestRepository.save(limitRequest);
                })
                .flatMap(saved -> accountOwnerIdOf(saved)
                        .flatMap(ownerId -> recordNote(staff, saved, ownerId, note)
                                .then(auditService.record(staff, "VERIFICATION_REQUESTED", "LimitRequest",
                                        saved.getId().toString()))
                                .then(otpService.issue(saved))
                                .flatMap(code -> notificationService.send(ownerId, NotificationType.VERIFICATION_REQUESTED,
                                        "Additional verification needed",
                                        "We need to re-verify this request. Your new code is " + code + "."))
                                .thenReturn(saved)));
    }

    public Mono<SupportNoteResponse> addStaffNote(User staff, UUID requestId, String note) {
        return inScope(staff, requestId)
                .flatMap(limitRequest -> accountOwnerIdOf(limitRequest)
                        .flatMap(ownerId -> recordNote(staff, limitRequest, ownerId, note)))
                .switchIfEmpty(Mono.error(new ValidationException("Note text is required")));
    }

    public Flux<SupportNoteResponse> notesFor(User staff, UUID requestId) {
        return inScope(staff, requestId)
                .flatMapMany(limitRequest -> supportNoteRepository.findByLimitRequestIdOrderByCreatedAtAsc(requestId)
                        .collectList()
                        .flatMapMany(notes -> userRepository.findAllById(
                                        notes.stream().map(SupportNote::getAuthorUserId).distinct().toList())
                                .collectMap(User::getId, User::fullName)
                                .flatMapMany(nameByUserId -> Flux.fromIterable(notes)
                                        .map(supportNote -> SupportNoteResponse.from(supportNote,
                                                nameByUserId.get(supportNote.getAuthorUserId()))))));
    }

    /** Note assembly returns the persisted note as a response DTO — {@code Mono.empty()} when
     * there's no note text, matching the previous "return null" no-op behavior. */
    private Mono<SupportNoteResponse> recordNote(User staff, LimitRequest limitRequest, UUID accountOwnerId, String note) {
        if (note == null || note.isBlank()) {
            return Mono.empty();
        }
        return supportNoteRepository.save(new SupportNote(limitRequest.getId(), staff.getId(), note))
                .flatMap(supportNote -> notificationService.send(accountOwnerId, NotificationType.SUPPORT_COMMENT,
                                "New message from support", note)
                        .thenReturn(SupportNoteResponse.from(supportNote, staff.fullName())));
    }

    private Mono<UUID> accountOwnerIdOf(LimitRequest limitRequest) {
        return accountRepository.findById(limitRequest.getAccountId()).map(Account::getUserId);
    }

    private Mono<LimitRequest> reviewable(User staff, UUID requestId) {
        return inScope(staff, requestId)
                .flatMap(limitRequest -> {
                    if (limitRequest.getStatus() != RequestStatus.UNDER_REVIEW) {
                        return Mono.<LimitRequest>error(new ValidationException("Request is not awaiting review"));
                    }
                    return Mono.just(limitRequest);
                });
    }

    /** Support agents only act on MEDIUM-risk requests; managers only on HIGH — mirrors {@link #queueFor}. */
    private Mono<LimitRequest> inScope(User staff, UUID requestId) {
        return limitRequestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new NotFoundException("Limit request not found")))
                .flatMap(limitRequest -> {
                    RiskLevel riskLevel = limitRequest.getRiskLevel();
                    if (riskLevel != null && riskLevel != scopeFor(staff.getRole())) {
                        return Mono.<LimitRequest>error(new ForbiddenException("This request is outside your review scope"));
                    }
                    return Mono.just(limitRequest);
                });
    }

    private RiskLevel scopeFor(Role role) {
        return role == Role.MANAGER ? RiskLevel.HIGH : RiskLevel.MEDIUM;
    }
}
