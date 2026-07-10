package com.limitflow.backend.domain.otp;

import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository {

    OtpCode save(OtpCode otpCode);

    Optional<OtpCode> findTopByLimitRequestIdOrderByCreatedAtDesc(UUID limitRequestId);
}
