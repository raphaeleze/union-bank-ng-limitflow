package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.support.SupportReviewService;
import com.limitflow.backend.domain.support.SupportNote;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.support.AddNoteRequest;
import com.limitflow.backend.presentation.dto.support.ReviewActionRequest;
import com.limitflow.backend.presentation.dto.support.SupportNoteResponse;
import com.limitflow.backend.presentation.dto.support.SupportQueueItemResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/support/requests")
@RequiredArgsConstructor
@Tag(name = "Support Review")
@PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'MANAGER')")
public class SupportController {

    private final SupportReviewService supportReviewService;

    @GetMapping
    public List<SupportQueueItemResponse> queue(@AuthenticationPrincipal User user) {
        return supportReviewService.queueFor(user.getRole()).stream()
                .map(SupportQueueItemResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public LimitRequestResponse get(@PathVariable UUID id) {
        return LimitRequestResponse.from(supportReviewService.getForReview(id));
    }

    @PostMapping("/{id}/approve")
    public LimitRequestResponse approve(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                         @RequestBody(required = false) ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return LimitRequestResponse.from(supportReviewService.approve(user, id, note));
    }

    @PostMapping("/{id}/reject")
    public LimitRequestResponse reject(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                        @RequestBody(required = false) ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return LimitRequestResponse.from(supportReviewService.reject(user, id, note));
    }

    @PostMapping("/{id}/request-verification")
    public LimitRequestResponse requestVerification(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                                      @RequestBody(required = false) ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return LimitRequestResponse.from(supportReviewService.requestAdditionalVerification(user, id, note));
    }

    @PostMapping("/{id}/notes")
    public SupportNoteResponse addNote(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                        @Valid @RequestBody AddNoteRequest request) {
        SupportNote note = supportReviewService.addStaffNote(user, id, request.note());
        return SupportNoteResponse.from(note);
    }

    @GetMapping("/{id}/notes")
    public List<SupportNoteResponse> notes(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return supportReviewService.notesFor(user, id).stream()
                .map(SupportNoteResponse::from)
                .toList();
    }
}
