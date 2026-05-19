package com.gymflow.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private MoneyUtils() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(SCALE, ROUNDING_MODE);
    }

    public static long toMinorUnits(BigDecimal amount) {
        return normalize(amount)
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, ROUNDING_MODE)
            .longValueExact();
    }
}
