package com.limitflow.backend.application.limitrequest.risk;

import com.limitflow.backend.domain.limitrequest.RiskLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Any request above the hard ceiling always requires manager review, regardless of the
 * other signals.
 */
@Component
@Order(1)
public class AmountThresholdRiskRule implements RiskRule {

    private final BigDecimal highRiskThreshold;

    public AmountThresholdRiskRule(@Value("${limitflow.risk.high-threshold}") BigDecimal highRiskThreshold) {
        this.highRiskThreshold = highRiskThreshold;
    }

    @Override
    public RiskLevel evaluate(RiskContext context) {
        return context.requestedLimit().compareTo(highRiskThreshold) > 0 ? RiskLevel.HIGH : RiskLevel.LOW;
    }
}
