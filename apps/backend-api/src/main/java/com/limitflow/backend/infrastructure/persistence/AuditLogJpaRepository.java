package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.audit.AuditLog;
import com.limitflow.backend.domain.audit.AuditLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLog, UUID>, AuditLogRepository {
}
