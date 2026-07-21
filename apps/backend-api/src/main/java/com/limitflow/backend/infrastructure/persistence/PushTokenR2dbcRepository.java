package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.push.PushToken;
import com.limitflow.backend.domain.push.PushTokenRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PushTokenR2dbcRepository extends R2dbcRepository<PushToken, UUID>, PushTokenRepository {

    @Override
    Mono<Void> deleteByUserIdAndExpoPushToken(UUID userId, String expoPushToken);
}
