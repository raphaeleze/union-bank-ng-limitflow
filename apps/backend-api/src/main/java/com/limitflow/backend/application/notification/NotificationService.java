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
    private final PushNotificationService pushNotificationService;

    /** Persists the in-app notification, then best-effort pushes it to any registered mobile
     * device. A push failure (no token, Expo outage, bad token) must never fail this call —
     * every existing caller (OTP codes, status updates) already treats {@code send} as
     * unconditionally successful once the in-app notification exists. */
    public Mono<Notification> send(UUID userId, NotificationType type, String title, String message) {
        return notificationRepository.save(new Notification(userId, type, title, message))
                .flatMap(saved -> pushNotificationService.push(userId, title, message)
                        .onErrorResume(e -> Mono.empty())
                        .thenReturn(saved));
    }

    public Flux<Notification> findForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
