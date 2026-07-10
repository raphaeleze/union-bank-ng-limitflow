package com.limitflow.backend.application.otp;

import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.otp.OtpCode;
import com.limitflow.backend.domain.otp.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Mock OTP delivery: no real SMS/email gateway exists in this demo, so the generated code
 * is returned to the caller to be surfaced through the (also mock) notification channel.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public String issue(LimitRequest limitRequest) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpCode otpCode = new OtpCode(limitRequest, passwordEncoder.encode(code), Instant.now().plus(TTL));
        otpCodeRepository.save(otpCode);
        return code;
    }

    public boolean verify(LimitRequest limitRequest, String code) {
        return otpCodeRepository.findTopByLimitRequestIdOrderByCreatedAtDesc(limitRequest.getId())
                .filter(otp -> !otp.isExpired())
                .filter(otp -> passwordEncoder.matches(code, otp.getCodeHash()))
                .map(this::markVerified)
                .isPresent();
    }

    private OtpCode markVerified(OtpCode otpCode) {
        otpCode.setVerifiedAt(Instant.now());
        return otpCodeRepository.save(otpCode);
    }
}
