package com.limitflow.backend.presentation.dto.limitrequest;

import com.limitflow.backend.domain.account.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record CurrentLimitResponse(
        UUID accountId,
        BigDecimal dailyLimit,
        BigDecimal usedToday,
        BigDecimal remaining,
        LimitRequestResponse activeRequest
) {

    public static CurrentLimitResponse from(Account account, LimitRequestResponse activeRequest) {
        return new CurrentLimitResponse(
                account.getId(),
                account.getDailyLimit(),
                account.getUsedToday(),
                account.remaining(),
                activeRequest);
    }
}
