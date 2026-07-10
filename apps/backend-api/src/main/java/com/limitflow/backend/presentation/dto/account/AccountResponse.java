package com.limitflow.backend.presentation.dto.account;

import com.limitflow.backend.domain.account.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        BigDecimal dailyLimit,
        BigDecimal usedToday,
        BigDecimal remaining,
        String status
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getDailyLimit(),
                account.getUsedToday(),
                account.remaining(),
                account.getStatus().name());
    }
}
