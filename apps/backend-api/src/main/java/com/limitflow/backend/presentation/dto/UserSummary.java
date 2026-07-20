package com.limitflow.backend.presentation.dto;

import com.limitflow.backend.domain.user.User;

import java.util.UUID;

public record UserSummary(UUID id, String firstName, String lastName, String email, String role, String phone) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), user.getPhone());
    }
}
