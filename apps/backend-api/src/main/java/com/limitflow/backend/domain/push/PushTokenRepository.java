package com.limitflow.backend.domain.push;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PushTokenRepository {

    <S extends PushToken> Mono<S> save(S pushToken);

    Flux<PushToken> findByUserId(UUID userId);

    Mono<Void> deleteByUserIdAndExpoPushToken(UUID userId, String expoPushToken);
}
