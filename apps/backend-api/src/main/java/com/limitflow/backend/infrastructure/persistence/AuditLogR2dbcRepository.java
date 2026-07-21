package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.audit.AuditLog;
import com.limitflow.backend.domain.audit.AuditLogRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface AuditLogR2dbcRepository extends R2dbcRepository<AuditLog, UUID>, AuditLogRepository {
}
