package com.limitflow.backend.domain.limitrequest;

import java.util.EnumSet;
import java.util.Set;

public enum RequestStatus {
    PENDING,
    OTP_PENDING,
    BIOMETRIC_PENDING,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    CANCELLED;

    /** Statuses where a request is still awaiting some verification or decision. */
    public static final Set<RequestStatus> ACTIVE =
            EnumSet.of(PENDING, OTP_PENDING, BIOMETRIC_PENDING, UNDER_REVIEW);
}
