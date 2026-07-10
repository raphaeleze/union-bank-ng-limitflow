package com.limitflow.backend.domain.limitrequest;

public enum RequestStatus {
    PENDING,
    OTP_PENDING,
    BIOMETRIC_PENDING,
    UNDER_REVIEW,
    APPROVED,
    REJECTED
}
