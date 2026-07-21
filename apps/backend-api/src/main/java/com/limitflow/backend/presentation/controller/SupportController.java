package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.support.SupportReviewService;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.support.AddNoteRequest;
import com.limitflow.backend.presentation.dto.support.ReviewActionRequest;
import com.limitflow.backend.presentation.dto.support.SupportNoteResponse;
import com.limitflow.backend.presentation.dto.support.SupportQueueItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'MANAGER')")
public class SupportController implements SupportApi {

    private final SupportReviewService supportReviewService;

    @Override
    public Flux<SupportQueueItemResponse> queue(User user) {
        return supportReviewService.queueFor(user.getRole());
    }

    @Override
    public Mono<LimitRequestResponse> get(UUID id) {
        return supportReviewService.getForReview(id).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> approve(User user, UUID id, ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return supportReviewService.approve(user, id, note).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> reject(User user, UUID id, ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return supportReviewService.reject(user, id, note).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> requestVerification(User user, UUID id, ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return supportReviewService.requestAdditionalVerification(user, id, note).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<SupportNoteResponse> addNote(User user, UUID id, AddNoteRequest request) {
        return supportReviewService.addStaffNote(user, id, request.note());
    }

    @Override
    public Flux<SupportNoteResponse> notes(User user, UUID id) {
        return supportReviewService.notesFor(user, id);
    }
}
