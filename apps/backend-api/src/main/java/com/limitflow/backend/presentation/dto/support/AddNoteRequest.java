package com.limitflow.backend.presentation.dto.support;

import jakarta.validation.constraints.NotBlank;

public record AddNoteRequest(@NotBlank String note) {
}
