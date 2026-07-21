package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.customer.CustomerService;
import com.limitflow.backend.application.limitrequest.LimitRequestService;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.BiometricVerifyRequest;
import com.limitflow.backend.presentation.dto.limitrequest.CurrentLimitResponse;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestSubmitRequest;
import com.limitflow.backend.presentation.dto.limitrequest.OtpVerifyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class LimitRequestController implements LimitRequestApi {

    private final LimitRequestService limitRequestService;
    private final CustomerService customerService;

    @Override
    public Mono<CurrentLimitResponse> current(User user) {
        return customerService.primaryAccount(user)
                .flatMap(account -> limitRequestService.history(user, account.getId())
                        .filter(r -> RequestStatus.ACTIVE.contains(r.getStatus()))
                        .next()
                        .map(LimitRequestResponse::from)
                        .map(activeRequest -> CurrentLimitResponse.from(account, activeRequest))
                        .switchIfEmpty(Mono.fromSupplier(() -> CurrentLimitResponse.from(account, null))));
    }

    @Override
    public Mono<LimitRequestResponse> submit(User user, LimitRequestSubmitRequest request) {
        return limitRequestService.submitRequest(user, request.accountId(), request.requestedLimit(),
                        request.reason(), request.knownDevice())
                .map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> verifyOtp(User user, UUID id, OtpVerifyRequest request) {
        return limitRequestService.verifyOtp(user, id, request.code()).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> verifyBiometric(User user, UUID id, BiometricVerifyRequest request) {
        return limitRequestService.verifyBiometric(user, id, request.success()).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> cancel(User user, UUID id) {
        return limitRequestService.cancel(user, id).map(LimitRequestResponse::from);
    }

    @Override
    public Flux<LimitRequestResponse> history(User user) {
        return customerService.primaryAccount(user)
                .flatMapMany(account -> limitRequestService.history(user, account.getId()))
                .map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> get(User user, UUID id) {
        return limitRequestService.get(user, id).map(LimitRequestResponse::from);
    }
}
