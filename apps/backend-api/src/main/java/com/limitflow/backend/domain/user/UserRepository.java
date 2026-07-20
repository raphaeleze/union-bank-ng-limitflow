package com.limitflow.backend.domain.user;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository {

    Mono<User> save(User user);

    Mono<User> findById(UUID id);

    Mono<User> findByEmail(String email);
}
