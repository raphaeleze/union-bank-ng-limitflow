package com.limitflow.backend.application.auth;

import com.limitflow.backend.domain.user.User;

import java.util.Optional;
import java.util.UUID;

public interface TokenService {

    String generateToken(User user);

    Optional<UUID> extractUserId(String token);
}
