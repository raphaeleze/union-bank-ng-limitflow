package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.support.AddNoteRequest;
import com.limitflow.backend.presentation.dto.support.ReviewActionRequest;
import com.limitflow.backend.presentation.dto.support.SupportNoteResponse;
import com.limitflow.backend.presentation.dto.support.SupportQueueItemResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequestMapping("/api/support/requests")
@Tag(name = "Support Review")
public interface SupportApi {

    @GetMapping
    Flux<SupportQueueItemResponse> queue(@AuthenticationPrincipal User user);

    @GetMapping("/{id}")
    Mono<LimitRequestResponse> get(@PathVariable UUID id);

    @PostMapping("/{id}/approve")
    Mono<LimitRequestResponse> approve(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                        @RequestBody(required = false) ReviewActionRequest request);

    @PostMapping("/{id}/reject")
    Mono<LimitRequestResponse> reject(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                       @RequestBody(required = false) ReviewActionRequest request);

    @PostMapping("/{id}/request-verification")
    Mono<LimitRequestResponse> requestVerification(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                                     @RequestBody(required = false) ReviewActionRequest request);

    @PostMapping("/{id}/notes")
    Mono<SupportNoteResponse> addNote(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                       @Valid @RequestBody AddNoteRequest request);

    @GetMapping("/{id}/notes")
    Flux<SupportNoteResponse> notes(@AuthenticationPrincipal User user, @PathVariable UUID id);
}
