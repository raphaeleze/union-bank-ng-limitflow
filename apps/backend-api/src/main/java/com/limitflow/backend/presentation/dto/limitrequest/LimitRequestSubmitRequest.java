package com.limitflow.backend.presentation.dto.limitrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record LimitRequestSubmitRequest(
        @NotNull UUID accountId,
        @NotNull @Positive BigDecimal requestedLimit,
        @NotBlank String reason,
        boolean knownDevice
) {
}
