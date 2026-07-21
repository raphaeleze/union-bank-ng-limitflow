package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.presentation.dto.auth.LoginRequest;
import com.limitflow.backend.presentation.dto.auth.LoginResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public interface AuthApi {

    @PostMapping("/login")
    Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request);
}
