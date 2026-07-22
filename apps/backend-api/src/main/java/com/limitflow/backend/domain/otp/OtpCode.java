package com.limitflow.backend.domain.otp;

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
@Table("otp_codes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtpCode implements Persistable<UUID> {

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

    @Transient
    private boolean isNew = true;

    public OtpCode(UUID limitRequestId, String codeHash, Instant expiresAt) {
        this.limitRequestId = limitRequestId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    @PersistenceCreator
    OtpCode(UUID id, UUID limitRequestId, String codeHash, Instant expiresAt, Instant verifiedAt, Instant createdAt) {
        this.id = id;
        this.limitRequestId = limitRequestId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
        this.isNew = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }
}
