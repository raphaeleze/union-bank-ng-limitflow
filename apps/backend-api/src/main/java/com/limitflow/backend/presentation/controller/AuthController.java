package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.auth.AuthResult;
import com.limitflow.backend.application.auth.AuthService;
import com.limitflow.backend.presentation.dto.auth.LoginRequest;
import com.limitflow.backend.presentation.dto.auth.LoginResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.email(), request.password());
        return LoginResponse.from(result);
    }
}
