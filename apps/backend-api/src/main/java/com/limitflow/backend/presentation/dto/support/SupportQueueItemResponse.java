package com.limitflow.backend.presentation.dto.support;

import com.limitflow.backend.domain.limitrequest.LimitRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SupportQueueItemResponse(
        UUID id,
        String customerName,
        BigDecimal currentLimit,
        BigDecimal requestedLimit,
        String riskLevel,
        String status,
        Instant createdAt
) {

    public static SupportQueueItemResponse from(LimitRequest request, String customerName) {
        return new SupportQueueItemResponse(
                request.getId(),
                customerName,
                request.getCurrentLimit(),
                request.getRequestedLimit(),
                request.getRiskLevel() != null ? request.getRiskLevel().name() : null,
                request.getStatus().name(),
                request.getCreatedAt());
    }
}
