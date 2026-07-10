package com.limitflow.backend.presentation.dto.auth;

import com.limitflow.backend.application.auth.AuthResult;
import com.limitflow.backend.presentation.dto.UserSummary;

public record LoginResponse(String token, UserSummary user) {

    public static LoginResponse from(AuthResult result) {
        return new LoginResponse(result.token(), UserSummary.from(result.user()));
    }
}
