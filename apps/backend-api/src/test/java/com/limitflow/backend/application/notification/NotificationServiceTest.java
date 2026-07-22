package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.notification.Notification;
import com.limitflow.backend.domain.notification.NotificationRepository;
import com.limitflow.backend.domain.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushNotificationService pushNotificationService;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, pushNotificationService);
    }

    @Test
    void sendStillPersistsTheNotificationWhenThePushCallFails() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pushNotificationService.push(any(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Expo is down")));

        StepVerifier.create(service.send(userId, NotificationType.OTP_SENT, "Title", "Body"))
                .expectNextCount(1)
                .verifyComplete();

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendPushesAfterPersisting() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pushNotificationService.push(any(), anyString(), anyString())).thenReturn(Mono.empty());

        service.send(userId, NotificationType.OTP_SENT, "Title", "Body").block();

        verify(pushNotificationService).push(userId, "Title", "Body");
    }
}
