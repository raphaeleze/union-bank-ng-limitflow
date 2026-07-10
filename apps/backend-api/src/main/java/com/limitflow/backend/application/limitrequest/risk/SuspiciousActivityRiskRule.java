package com.limitflow.backend.application.limitrequest.risk;

import com.limitflow.backend.domain.limitrequest.RiskLevel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Any recent suspicious activity flag on the account escalates straight to manager review.
 */
@Component
@Order(4)
public class SuspiciousActivityRiskRule implements RiskRule {

    @Override
    public RiskLevel evaluate(RiskContext context) {
        return context.suspiciousActivity() ? RiskLevel.HIGH : RiskLevel.LOW;
    }
}
