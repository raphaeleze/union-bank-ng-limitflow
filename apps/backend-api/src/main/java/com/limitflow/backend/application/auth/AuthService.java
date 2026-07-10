package com.limitflow.backend.application.auth;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.domain.exception.InvalidCredentialsException;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditService auditService;

    public AuthResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = tokenService.generateToken(user);
        auditService.record(user, "LOGGED_IN", "User", user.getId().toString());
        return new AuthResult(token, user);
    }
}
