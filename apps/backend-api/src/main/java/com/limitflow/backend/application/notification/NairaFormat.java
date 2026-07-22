package com.limitflow.backend.application.notification;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class NairaFormat {

    private NairaFormat() {
    }

    public static String format(BigDecimal amount) {
        return "₦" + new DecimalFormat("#,##0").format(amount);
    }
}
