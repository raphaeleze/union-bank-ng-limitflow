package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.auth.AuthService;
import com.limitflow.backend.presentation.dto.auth.LoginRequest;
import com.limitflow.backend.presentation.dto.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public Mono<LoginResponse> login(LoginRequest request) {
        return authService.login(request.email(), request.password()).map(LoginResponse::from);
    }
}
