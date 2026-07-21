package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.domain.user.UserRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface UserR2dbcRepository extends R2dbcRepository<User, UUID>, UserRepository {
}
