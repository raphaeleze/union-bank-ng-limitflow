package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.customer.CustomerService;
import com.limitflow.backend.application.limitrequest.LimitRequestService;
import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/limits")
@RequiredArgsConstructor
@Tag(name = "Transfer Limit Requests")
@PreAuthorize("hasRole('CUSTOMER')")
public class LimitRequestController {

    private static final Set<RequestStatus> ACTIVE_STATUSES = Set.of(
            RequestStatus.PENDING, RequestStatus.OTP_PENDING,
            RequestStatus.BIOMETRIC_PENDING, RequestStatus.UNDER_REVIEW);

    private final LimitRequestService limitRequestService;
    private final CustomerService customerService;

    @GetMapping("/current")
    public CurrentLimitResponse current(@AuthenticationPrincipal User user) {
        Account account = customerService.primaryAccount(user);
        LimitRequestResponse activeRequest = limitRequestService.history(user, account.getId()).stream()
                .filter(r -> ACTIVE_STATUSES.contains(r.getStatus()))
                .findFirst()
                .map(LimitRequestResponse::from)
                .orElse(null);
        return CurrentLimitResponse.from(account, activeRequest);
    }

    @PostMapping("/request")
    public LimitRequestResponse submit(@AuthenticationPrincipal User user, @Valid @RequestBody LimitRequestSubmitRequest request) {
        LimitRequest limitRequest = limitRequestService.submitRequest(
                user, request.accountId(), request.requestedLimit(), request.reason(), request.knownDevice());
        return LimitRequestResponse.from(limitRequest);
    }

    @PostMapping("/{id}/otp/verify")
    public LimitRequestResponse verifyOtp(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                           @Valid @RequestBody OtpVerifyRequest request) {
        return LimitRequestResponse.from(limitRequestService.verifyOtp(user, id, request.code()));
    }

    @PostMapping("/{id}/biometric/verify")
    public LimitRequestResponse verifyBiometric(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                                 @Valid @RequestBody BiometricVerifyRequest request) {
        return LimitRequestResponse.from(limitRequestService.verifyBiometric(user, id, request.success()));
    }

    @GetMapping("/history")
    public List<LimitRequestResponse> history(@AuthenticationPrincipal User user) {
        Account account = customerService.primaryAccount(user);
        return limitRequestService.history(user, account.getId()).stream()
                .map(LimitRequestResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public LimitRequestResponse get(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return LimitRequestResponse.from(limitRequestService.get(user, id));
    }
}
