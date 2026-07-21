package com.limitflow.backend.domain.limitrequest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The {@code id} is client-assigned (not DB-generated), so Spring Data R2DBC can't tell new
 * from existing rows just by checking for a null id — it would otherwise emit an UPDATE for a
 * brand-new, never-persisted entity, silently affecting zero rows instead of inserting.
 * Implementing {@link Persistable} with an explicit {@code isNew} flag fixes that: the business
 * constructor leaves it {@code true}, while the {@link PersistenceCreator} constructor Spring
 * Data uses to rehydrate rows read back from the database sets it {@code false}.
 */
@Table("limit_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LimitRequest implements Persistable<UUID> {

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

    @Transient
    private boolean isNew = true;

    public LimitRequest(UUID accountId, BigDecimal currentLimit, BigDecimal requestedLimit,
                         String reason, boolean knownDevice) {
        this.accountId = accountId;
        this.currentLimit = currentLimit;
        this.requestedLimit = requestedLimit;
        this.reason = reason;
        this.knownDevice = knownDevice;
    }

    @PersistenceCreator
    LimitRequest(UUID id, UUID accountId, BigDecimal currentLimit, BigDecimal requestedLimit, String reason,
            RequestStatus status, RiskLevel riskLevel, boolean knownDevice, Instant otpVerifiedAt,
            Instant biometricVerifiedAt, UUID resolvedByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountId = accountId;
        this.currentLimit = currentLimit;
        this.requestedLimit = requestedLimit;
        this.reason = reason;
        this.status = status;
        this.riskLevel = riskLevel;
        this.knownDevice = knownDevice;
        this.otpVerifiedAt = otpVerifiedAt;
        this.biometricVerifiedAt = biometricVerifiedAt;
        this.resolvedByUserId = resolvedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isNew = false;
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
