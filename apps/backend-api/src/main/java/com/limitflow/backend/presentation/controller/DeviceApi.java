package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.device.PushTokenRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/api/devices")
@Tag(name = "Devices")
public interface DeviceApi {

    @PostMapping("/push-token")
    Mono<Void> register(@AuthenticationPrincipal User user, @Valid @RequestBody PushTokenRequest request);

    @DeleteMapping("/push-token")
    Mono<Void> unregister(@AuthenticationPrincipal User user, @Valid @RequestBody PushTokenRequest request);
}
