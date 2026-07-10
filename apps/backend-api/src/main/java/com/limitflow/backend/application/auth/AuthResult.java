package com.limitflow.backend.application.auth;

import com.limitflow.backend.domain.user.User;

public record AuthResult(String token, User user) {
}
