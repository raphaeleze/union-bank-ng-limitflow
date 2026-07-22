package com.limitflow.backend.domain.limitrequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LimitRequestRepository {

    <S extends LimitRequest> Mono<S> save(S limitRequest);

    Mono<LimitRequest> findById(UUID id);

    Flux<LimitRequest> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Flux<LimitRequest> findByStatusInOrderByCreatedAtAsc(List<RequestStatus> statuses);

    Flux<LimitRequest> findByRiskLevelAndStatusInOrderByCreatedAtAsc(RiskLevel riskLevel, List<RequestStatus> statuses);

    Mono<Boolean> existsByAccountIdAndStatusIn(UUID accountId, Collection<RequestStatus> statuses);

    Mono<Long> countByAccountIdAndCreatedAtAfter(UUID accountId, Instant createdAt);
}
