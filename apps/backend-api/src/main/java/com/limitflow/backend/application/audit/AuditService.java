package com.limitflow.backend.application.audit;

import com.limitflow.backend.domain.audit.AuditLog;
import com.limitflow.backend.domain.audit.AuditLogRepository;
import com.limitflow.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(User actor, String action, String entityType, String entityId) {
        record(actor, action, entityType, entityId, null);
    }

    public void record(User actor, String action, String entityType, String entityId, String metadata) {
        auditLogRepository.save(new AuditLog(actor, action, entityType, entityId, metadata));
    }

    public List<AuditLog> findAll() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
}
