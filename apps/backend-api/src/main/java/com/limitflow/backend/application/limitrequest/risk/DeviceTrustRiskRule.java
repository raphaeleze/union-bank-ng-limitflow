package com.limitflow.backend.application.limitrequest.risk;

import com.limitflow.backend.domain.limitrequest.RiskLevel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Requests coming from a device the customer hasn't used before are treated as elevated risk.
 */
@Component
@Order(3)
public class DeviceTrustRiskRule implements RiskRule {

    @Override
    public RiskLevel evaluate(RiskContext context) {
        return context.knownDevice() ? RiskLevel.LOW : RiskLevel.MEDIUM;
    }
}
