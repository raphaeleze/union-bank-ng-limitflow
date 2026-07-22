package com.limitflow.backend.domain.audit;

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
@Table("audit_logs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("actor_user_id")
    private UUID actorUserId;

    private String action;

    @Column("entity_type")
    private String entityType;

    @Column("entity_id")
    private String entityId;

    private String metadata;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Transient
    private boolean isNew = true;

    public AuditLog(UUID actorUserId, String action, String entityType, String entityId, String metadata) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.metadata = metadata;
    }

    @PersistenceCreator
    AuditLog(UUID id, UUID actorUserId, String action, String entityType, String entityId, String metadata,
            Instant createdAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.isNew = false;
    }
}
