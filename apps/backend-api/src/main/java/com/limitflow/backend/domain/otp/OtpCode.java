package com.limitflow.backend.domain.otp;

import com.limitflow.backend.domain.limitrequest.LimitRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtpCode {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "limit_request_id", nullable = false)
    private LimitRequest limitRequest;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OtpCode(LimitRequest limitRequest, String codeHash, Instant expiresAt) {
        this.limitRequest = limitRequest;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }
}
