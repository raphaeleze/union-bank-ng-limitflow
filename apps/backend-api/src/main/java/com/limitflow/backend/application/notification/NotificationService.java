package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.notification.Notification;
import com.limitflow.backend.domain.notification.NotificationRepository;
import com.limitflow.backend.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Mono<Notification> send(UUID userId, NotificationType type, String title, String message) {
        return notificationRepository.save(new Notification(userId, type, title, message));
    }

    public Flux<Notification> findForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
