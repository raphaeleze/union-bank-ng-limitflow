package com.limitflow.backend.domain.otp;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OtpCodeRepository {

    Mono<OtpCode> save(OtpCode otpCode);

    Mono<OtpCode> findTopByLimitRequestIdOrderByCreatedAtDesc(UUID limitRequestId);
}
