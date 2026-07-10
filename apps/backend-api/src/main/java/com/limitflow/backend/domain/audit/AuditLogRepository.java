package com.limitflow.backend.domain.audit;

import java.util.List;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
