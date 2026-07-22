package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface LimitRequestR2dbcRepository extends R2dbcRepository<LimitRequest, UUID>, LimitRequestRepository {
}
