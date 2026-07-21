package com.limitflow.backend.domain.user;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository {

    <S extends User> Mono<S> save(S user);

    Mono<User> findById(UUID id);

    Mono<User> findByEmail(String email);

    Flux<User> findAllById(Iterable<UUID> ids);
}
