package com.limitflow.backend.presentation.dto.support;

import com.limitflow.backend.domain.support.SupportNote;

import java.time.Instant;
import java.util.UUID;

public record SupportNoteResponse(UUID id, String authorName, String note, Instant createdAt) {

    public static SupportNoteResponse from(SupportNote supportNote, String authorName) {
        return new SupportNoteResponse(
                supportNote.getId(),
                authorName,
                supportNote.getNote(),
                supportNote.getCreatedAt());
    }
}
