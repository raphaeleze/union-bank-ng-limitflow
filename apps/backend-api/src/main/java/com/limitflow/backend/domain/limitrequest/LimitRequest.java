package com.limitflow.backend.domain.limitrequest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("limit_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LimitRequest {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("account_id")
    private UUID accountId;

    @Column("current_limit")
    private BigDecimal currentLimit;

    @Column("requested_limit")
    private BigDecimal requestedLimit;

    private String reason;

    private RequestStatus status = RequestStatus.PENDING;

    @Column("risk_level")
    private RiskLevel riskLevel;

    @Column("known_device")
    private boolean knownDevice = true;

    @Column("otp_verified_at")
    private Instant otpVerifiedAt;

    @Column("biometric_verified_at")
    private Instant biometricVerifiedAt;

    @Column("resolved_by")
    private UUID resolvedByUserId;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Column("updated_at")
    private Instant updatedAt = Instant.now();

    public LimitRequest(UUID accountId, BigDecimal currentLimit, BigDecimal requestedLimit,
                         String reason, boolean knownDevice) {
        this.accountId = accountId;
        this.currentLimit = currentLimit;
        this.requestedLimit = requestedLimit;
        this.reason = reason;
        this.knownDevice = knownDevice;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void transitionTo(RequestStatus newStatus) {
        this.status = newStatus;
        touch();
    }

    public boolean isFullyVerified() {
        return otpVerifiedAt != null && biometricVerifiedAt != null;
    }
}
