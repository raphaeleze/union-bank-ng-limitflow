package com.limitflow.backend.domain.support;

import java.util.List;
import java.util.UUID;

public interface SupportNoteRepository {

    SupportNote save(SupportNote supportNote);

    List<SupportNote> findByLimitRequestIdOrderByCreatedAtAsc(UUID limitRequestId);
}
