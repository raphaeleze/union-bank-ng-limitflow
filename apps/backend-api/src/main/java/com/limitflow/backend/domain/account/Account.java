package com.limitflow.backend.domain.account;

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
@Table("accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account implements Persistable<UUID> {

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

    @Transient
    private boolean isNew = true;

    public Account(UUID userId, String accountNumber, BigDecimal dailyLimit, BigDecimal usedToday) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.dailyLimit = dailyLimit;
        this.usedToday = usedToday;
    }

    @PersistenceCreator
    Account(UUID id, UUID userId, String accountNumber, BigDecimal dailyLimit, BigDecimal usedToday,
            AccountStatus status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.dailyLimit = dailyLimit;
        this.usedToday = usedToday;
        this.status = status;
        this.createdAt = createdAt;
        this.isNew = false;
    }

    public BigDecimal remaining() {
        BigDecimal remaining = dailyLimit.subtract(usedToday);
        return remaining.max(BigDecimal.ZERO);
    }

    public void applyNewLimit(BigDecimal newLimit) {
        this.dailyLimit = newLimit;
    }
}
