package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.otp.OtpCode;
import com.limitflow.backend.domain.otp.OtpCodeRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OtpCodeJpaRepository extends JpaRepository<OtpCode, UUID>, OtpCodeRepository {
}
