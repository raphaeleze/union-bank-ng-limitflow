package com.limitflow.backend.application.limitrequest.risk;

import com.limitflow.backend.domain.limitrequest.RiskLevel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * A request for more than double the current limit is treated as elevated risk.
 */
@Component
@Order(2)
public class MultiplierRiskRule implements RiskRule {

    private static final BigDecimal DOUBLE = BigDecimal.valueOf(2);

    @Override
    public RiskLevel evaluate(RiskContext context) {
        return context.multiplier().compareTo(DOUBLE) >= 0 ? RiskLevel.MEDIUM : RiskLevel.LOW;
    }
}
