package com.limitflow.backend.application.limitrequest.risk;

import com.limitflow.backend.domain.limitrequest.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Runs every configured {@link RiskRule} and takes the highest severity result. Rules are
 * independent and order-agnostic; new rules can be added by registering another
 * {@link RiskRule} bean without touching this class.
 */
@Component
@RequiredArgsConstructor
public class RiskEngine {

    private final List<RiskRule> rules;

    public RiskLevel assess(RiskContext context) {
        return rules.stream()
                .map(rule -> rule.evaluate(context))
                .max(Comparator.comparingInt(RiskLevel::ordinal))
                .orElse(RiskLevel.LOW);
    }
}
