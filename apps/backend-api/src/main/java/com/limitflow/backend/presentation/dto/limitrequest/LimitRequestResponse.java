package com.limitflow.backend.presentation.dto.limitrequest;

import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.RequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LimitRequestResponse(
        UUID id,
        UUID accountId,
        BigDecimal currentLimit,
        BigDecimal requestedLimit,
        String reason,
        String status,
        String riskLevel,
        Instant createdAt,
        Instant updatedAt,
        List<TimelineStepResponse> timeline
) {

    public static LimitRequestResponse from(LimitRequest request) {
        return new LimitRequestResponse(
                request.getId(),
                request.getAccount().getId(),
                request.getCurrentLimit(),
                request.getRequestedLimit(),
                request.getReason(),
                request.getStatus().name(),
                request.getRiskLevel() != null ? request.getRiskLevel().name() : null,
                request.getCreatedAt(),
                request.getUpdatedAt(),
                buildTimeline(request));
    }

    private static List<TimelineStepResponse> buildTimeline(LimitRequest request) {
        RequestStatus status = request.getStatus();

        String otpStatus = request.getOtpVerifiedAt() != null ? "COMPLETE"
                : status == RequestStatus.OTP_PENDING ? "CURRENT" : "PENDING";
        String biometricStatus = request.getBiometricVerifiedAt() != null ? "COMPLETE"
                : status == RequestStatus.BIOMETRIC_PENDING ? "CURRENT" : "PENDING";
        String riskStatus = request.getRiskLevel() != null ? "COMPLETE" : "PENDING";
        String decisionStatus = switch (status) {
            case APPROVED, REJECTED -> "COMPLETE";
            case UNDER_REVIEW -> "CURRENT";
            default -> "PENDING";
        };

        return List.of(
                new TimelineStepResponse("Submitted", "COMPLETE"),
                new TimelineStepResponse("OTP Verified", otpStatus),
                new TimelineStepResponse("Biometric Verified", biometricStatus),
                new TimelineStepResponse("Risk Assessment", riskStatus),
                new TimelineStepResponse(status == RequestStatus.REJECTED ? "Rejected" : "Approved", decisionStatus)
        );
    }
}
