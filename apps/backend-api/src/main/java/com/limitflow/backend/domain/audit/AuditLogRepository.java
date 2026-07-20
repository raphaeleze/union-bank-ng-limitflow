package com.limitflow.backend.domain.audit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AuditLogRepository {

    Mono<AuditLog> save(AuditLog auditLog);

    Flux<AuditLog> findAllByOrderByCreatedAtDesc();
}
