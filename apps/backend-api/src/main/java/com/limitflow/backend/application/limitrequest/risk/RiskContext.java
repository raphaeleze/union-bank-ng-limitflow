package com.limitflow.backend.application.limitrequest.risk;

import java.math.BigDecimal;

public record RiskContext(
        BigDecimal currentLimit,
        BigDecimal requestedLimit,
        boolean knownDevice,
        boolean suspiciousActivity
) {

    public BigDecimal multiplier() {
        if (currentLimit.signum() == 0) {
            return requestedLimit;
        }
        return requestedLimit.divide(currentLimit, 4, java.math.RoundingMode.HALF_UP);
    }
}
