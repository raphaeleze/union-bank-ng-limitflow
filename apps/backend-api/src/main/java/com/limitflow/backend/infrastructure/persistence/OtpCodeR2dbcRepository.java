package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.otp.OtpCode;
import com.limitflow.backend.domain.otp.OtpCodeRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface OtpCodeR2dbcRepository extends R2dbcRepository<OtpCode, UUID>, OtpCodeRepository {
}
