package com.limitflow.backend.domain.audit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AuditLogRepository {

    <S extends AuditLog> Mono<S> save(S auditLog);

    Flux<AuditLog> findAllByOrderByCreatedAtDesc();
}
