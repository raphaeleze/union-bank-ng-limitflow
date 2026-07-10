package com.limitflow.backend.domain.limitrequest;

import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "limit_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LimitRequest {

    @Id
    private UUID id = UUID.randomUUID();

    // Eager for the same reason as Account.user: always needed for display DTOs, and
    // open-in-view is disabled so lazy loading would fail outside the service call.
    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "current_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentLimit;

    @Column(name = "requested_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedLimit;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "known_device", nullable = false)
    private boolean knownDevice = true;

    @Column(name = "otp_verified_at")
    private Instant otpVerifiedAt;

    @Column(name = "biometric_verified_at")
    private Instant biometricVerifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public LimitRequest(Account account, BigDecimal currentLimit, BigDecimal requestedLimit,
                         String reason, boolean knownDevice) {
        this.account = account;
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
