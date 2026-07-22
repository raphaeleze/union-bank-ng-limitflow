package com.limitflow.backend.presentation.dto.device;

import jakarta.validation.constraints.NotBlank;

public record PushTokenRequest(
        @NotBlank String expoPushToken,
        @NotBlank String platform
) {
}
