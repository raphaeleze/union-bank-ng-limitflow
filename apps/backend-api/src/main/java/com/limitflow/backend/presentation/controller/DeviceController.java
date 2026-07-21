package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.push.PushToken;
import com.limitflow.backend.domain.push.PushTokenRepository;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.device.PushTokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class DeviceController implements DeviceApi {

    private final PushTokenRepository pushTokenRepository;

    @Override
    public Mono<Void> register(User user, PushTokenRequest request) {
        return pushTokenRepository
                .save(new PushToken(user.getId(), request.expoPushToken(), request.platform()))
                .then();
    }

    @Override
    public Mono<Void> unregister(User user, PushTokenRequest request) {
        return pushTokenRepository.deleteByUserIdAndExpoPushToken(user.getId(), request.expoPushToken());
    }
}
