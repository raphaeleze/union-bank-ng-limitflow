package com.limitflow.backend.domain.notification;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface NotificationRepository {

    Mono<Notification> save(Notification notification);

    Flux<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
