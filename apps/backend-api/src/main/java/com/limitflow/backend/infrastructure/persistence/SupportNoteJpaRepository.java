package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.support.SupportNote;
import com.limitflow.backend.domain.support.SupportNoteRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupportNoteJpaRepository extends JpaRepository<SupportNote, UUID>, SupportNoteRepository {
}
