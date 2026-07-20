package com.limitflow.backend.domain.account;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AccountRepository {

    Mono<Account> save(Account account);

    Mono<Account> findById(UUID id);

    Flux<Account> findByUserId(UUID userId);
}
