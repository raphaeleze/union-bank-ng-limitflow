package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface AccountR2dbcRepository extends R2dbcRepository<Account, UUID>, AccountRepository {
}
