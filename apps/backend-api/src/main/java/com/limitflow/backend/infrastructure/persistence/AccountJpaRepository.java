package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<Account, UUID>, AccountRepository {
}
