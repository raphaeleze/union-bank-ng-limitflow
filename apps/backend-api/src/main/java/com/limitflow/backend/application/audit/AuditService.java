package com.limitflow.backend.application.audit;

import com.limitflow.backend.domain.audit.AuditLog;
import com.limitflow.backend.domain.audit.AuditLogRepository;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.domain.user.UserRepository;
import com.limitflow.backend.presentation.dto.audit.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public Mono<AuditLog> record(User actor, String action, String entityType, String entityId) {
        return record(actor, action, entityType, entityId, null);
    }

    public Mono<AuditLog> record(User actor, String action, String entityType, String entityId, String metadata) {
        return auditLogRepository.save(new AuditLog(actor.getId(), action, entityType, entityId, metadata));
    }

    public Flux<AuditLogResponse> findAll() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc()
                .collectList()
                .flatMapMany(logs -> {
                    var actorIds = logs.stream().map(AuditLog::getActorUserId).distinct().toList();
                    return userRepository.findAllById(actorIds)
                            .collectMap(User::getId, User::fullName)
                            .flatMapMany(namesById -> Flux.fromIterable(logs)
                                    .map(log -> AuditLogResponse.from(log, namesById.get(log.getActorUserId()))));
                });
    }
}
