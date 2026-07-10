package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.domain.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<User, UUID>, UserRepository {
}
