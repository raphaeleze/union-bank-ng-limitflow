package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.push.PushTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * The Expo push delivery channel. Every send is fire-and-forget from the caller's
 * perspective — see {@link NotificationService#send} for why failures here must never
 * propagate.
 */
@Service
public class PushNotificationService {

    private final PushTokenRepository pushTokenRepository;
    private final WebClient expoPushClient;

    public PushNotificationService(PushTokenRepository pushTokenRepository,
            @Qualifier("expoPushClient") WebClient expoPushClient) {
        this.pushTokenRepository = pushTokenRepository;
        this.expoPushClient = expoPushClient;
    }

    public Mono<Void> push(UUID userId, String title, String body) {
        return pushTokenRepository.findByUserId(userId)
                .flatMap(token -> expoPushClient.post()
                        .bodyValue(Map.of("to", token.getExpoPushToken(), "title", title, "body", body))
                        .retrieve()
                        .toBodilessEntity())
                .then();
    }
}
