package com.limitflow.backend.domain.limitrequest;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitRequestRepository {

    LimitRequest save(LimitRequest limitRequest);

    Optional<LimitRequest> findById(UUID id);

    List<LimitRequest> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<LimitRequest> findByStatusInOrderByCreatedAtAsc(List<RequestStatus> statuses);

    List<LimitRequest> findByRiskLevelAndStatusInOrderByCreatedAtAsc(RiskLevel riskLevel, List<RequestStatus> statuses);

    boolean existsByAccountIdAndStatusIn(UUID accountId, Collection<RequestStatus> statuses);

    long countByAccountIdAndCreatedAtAfter(UUID accountId, Instant createdAt);
}
