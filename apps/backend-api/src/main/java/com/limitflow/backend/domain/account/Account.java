package com.limitflow.backend.domain.account;

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
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    private UUID id = UUID.randomUUID();

    // Eager: the owning user is always needed alongside the account for display, and
    // open-in-view is disabled, so a lazy proxy here would fail once mapped to a DTO.
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "daily_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "used_today", nullable = false, precision = 15, scale = 2)
    private BigDecimal usedToday = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Account(User user, String accountNumber, BigDecimal dailyLimit, BigDecimal usedToday) {
        this.user = user;
        this.accountNumber = accountNumber;
        this.dailyLimit = dailyLimit;
        this.usedToday = usedToday;
    }

    public BigDecimal remaining() {
        BigDecimal remaining = dailyLimit.subtract(usedToday);
        return remaining.max(BigDecimal.ZERO);
    }

    public void applyNewLimit(BigDecimal newLimit) {
        this.dailyLimit = newLimit;
    }
}
