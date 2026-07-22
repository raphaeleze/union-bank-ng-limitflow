package com.limitflow.backend.application.auth;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.domain.exception.InvalidCredentialsException;
import com.limitflow.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditService auditService;

    public Mono<AuthResult> login(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid email or password")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                        return Mono.error(new InvalidCredentialsException("Invalid email or password"));
                    }
                    String token = tokenService.generateToken(user);
                    return auditService.record(user, "LOGGED_IN", "User", user.getId().toString())
                            .thenReturn(new AuthResult(token, user));
                });
    }
}
