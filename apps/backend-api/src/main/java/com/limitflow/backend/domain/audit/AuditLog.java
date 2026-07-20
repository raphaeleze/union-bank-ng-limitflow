package com.limitflow.backend.domain.audit;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("audit_logs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

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

    public AuditLog(UUID actorUserId, String action, String entityType, String entityId, String metadata) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.metadata = metadata;
    }
}
