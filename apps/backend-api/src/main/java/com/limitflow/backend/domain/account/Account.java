package com.limitflow.backend.domain.account;

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

@Table("accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("user_id")
    private UUID userId;

    @Column("account_number")
    private String accountNumber;

    @Column("daily_limit")
    private BigDecimal dailyLimit;

    @Column("used_today")
    private BigDecimal usedToday = BigDecimal.ZERO;

    private AccountStatus status = AccountStatus.ACTIVE;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public Account(UUID userId, String accountNumber, BigDecimal dailyLimit, BigDecimal usedToday) {
        this.userId = userId;
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
