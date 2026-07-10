package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LimitRequestJpaRepository extends JpaRepository<LimitRequest, UUID>, LimitRequestRepository {
}
