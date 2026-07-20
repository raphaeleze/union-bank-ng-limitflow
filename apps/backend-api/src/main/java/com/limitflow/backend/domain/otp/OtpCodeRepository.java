package com.limitflow.backend.domain.otp;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OtpCodeRepository {

    <S extends OtpCode> Mono<S> save(S otpCode);

    Mono<OtpCode> findTopByLimitRequestIdOrderByCreatedAtDesc(UUID limitRequestId);
}
