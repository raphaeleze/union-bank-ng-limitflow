package com.limitflow.backend.presentation.dto.audit;

import com.limitflow.backend.domain.audit.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String actorName,
        String action,
        String entityType,
        String entityId,
        String metadata,
        Instant createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog, String actorName) {
        return new AuditLogResponse(
                auditLog.getId(),
                actorName,
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt());
    }
}
