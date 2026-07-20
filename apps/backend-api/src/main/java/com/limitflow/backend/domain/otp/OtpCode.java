package com.limitflow.backend.domain.otp;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("otp_codes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtpCode {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("limit_request_id")
    private UUID limitRequestId;

    @Column("code_hash")
    private String codeHash;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("verified_at")
    private Instant verifiedAt;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public OtpCode(UUID limitRequestId, String codeHash, Instant expiresAt) {
        this.limitRequestId = limitRequestId;
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
