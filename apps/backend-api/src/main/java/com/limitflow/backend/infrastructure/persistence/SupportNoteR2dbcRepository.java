package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.support.SupportNote;
import com.limitflow.backend.domain.support.SupportNoteRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface SupportNoteR2dbcRepository extends R2dbcRepository<SupportNote, UUID>, SupportNoteRepository {
}
