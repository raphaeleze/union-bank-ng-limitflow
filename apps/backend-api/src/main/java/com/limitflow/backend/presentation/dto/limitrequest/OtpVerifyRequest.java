package com.limitflow.backend.presentation.dto.limitrequest;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(@NotBlank String code) {
}
