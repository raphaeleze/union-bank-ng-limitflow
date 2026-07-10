package com.limitflow.backend.application.limitrequest.risk;

import com.limitflow.backend.domain.limitrequest.RiskLevel;

public interface RiskRule {

    RiskLevel evaluate(RiskContext context);
}
