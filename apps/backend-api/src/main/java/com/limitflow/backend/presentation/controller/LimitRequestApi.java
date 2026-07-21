package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.BiometricVerifyRequest;
import com.limitflow.backend.presentation.dto.limitrequest.CurrentLimitResponse;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestSubmitRequest;
import com.limitflow.backend.presentation.dto.limitrequest.OtpVerifyRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequestMapping("/api/limits")
@Tag(name = "Transfer Limit Requests")
public interface LimitRequestApi {

    @GetMapping("/current")
    Mono<CurrentLimitResponse> current(@AuthenticationPrincipal User user);

    @PostMapping("/request")
    Mono<LimitRequestResponse> submit(@AuthenticationPrincipal User user,
                                       @Valid @RequestBody LimitRequestSubmitRequest request);

    @PostMapping("/{id}/otp/verify")
    Mono<LimitRequestResponse> verifyOtp(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                         @Valid @RequestBody OtpVerifyRequest request);

    @PostMapping("/{id}/biometric/verify")
    Mono<LimitRequestResponse> verifyBiometric(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                                @Valid @RequestBody BiometricVerifyRequest request);

    @PostMapping("/{id}/cancel")
    Mono<LimitRequestResponse> cancel(@AuthenticationPrincipal User user, @PathVariable UUID id);

    @GetMapping("/history")
    Flux<LimitRequestResponse> history(@AuthenticationPrincipal User user);

    @GetMapping("/{id}")
    Mono<LimitRequestResponse> get(@AuthenticationPrincipal User user, @PathVariable UUID id);
}
