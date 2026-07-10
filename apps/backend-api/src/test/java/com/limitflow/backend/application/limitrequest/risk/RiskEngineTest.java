package com.limitflow.backend.application.limitrequest.risk;

import com.limitflow.backend.domain.limitrequest.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskEngineTest {

    private static final BigDecimal HIGH_THRESHOLD = BigDecimal.valueOf(1_000_000);

    private RiskEngine riskEngine;

    @BeforeEach
    void setUp() {
        riskEngine = new RiskEngine(List.of(
                new AmountThresholdRiskRule(HIGH_THRESHOLD),
                new MultiplierRiskRule(),
                new DeviceTrustRiskRule(),
                new SuspiciousActivityRiskRule()));
    }

    @Test
    void smallIncreaseOnKnownDeviceIsLowRisk() {
        RiskContext context = new RiskContext(
                BigDecimal.valueOf(200_000), BigDecimal.valueOf(300_000), true, false);

        assertThat(riskEngine.assess(context)).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void doublingTheLimitIsMediumRisk() {
        RiskContext context = new RiskContext(
                BigDecimal.valueOf(200_000), BigDecimal.valueOf(500_000), true, false);

        assertThat(riskEngine.assess(context)).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void unknownDeviceIsMediumRisk() {
        RiskContext context = new RiskContext(
                BigDecimal.valueOf(200_000), BigDecimal.valueOf(250_000), false, false);

        assertThat(riskEngine.assess(context)).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void requestAboveHardCeilingIsHighRiskRegardlessOfOtherSignals() {
        RiskContext context = new RiskContext(
                BigDecimal.valueOf(200_000), BigDecimal.valueOf(1_500_000), true, false);

        assertThat(riskEngine.assess(context)).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void suspiciousActivityIsHighRiskEvenForASmallIncrease() {
        RiskContext context = new RiskContext(
                BigDecimal.valueOf(200_000), BigDecimal.valueOf(210_000), true, true);

        assertThat(riskEngine.assess(context)).isEqualTo(RiskLevel.HIGH);
    }
}
