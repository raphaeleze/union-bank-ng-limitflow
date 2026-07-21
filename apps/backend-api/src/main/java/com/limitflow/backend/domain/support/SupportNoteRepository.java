package com.limitflow.backend.domain.support;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SupportNoteRepository {

    <S extends SupportNote> Mono<S> save(S supportNote);

    Flux<SupportNote> findByLimitRequestIdOrderByCreatedAtAsc(UUID limitRequestId);
}
