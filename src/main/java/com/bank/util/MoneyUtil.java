package com.bank.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {
    private MoneyUtil() {
    }

    public static BigDecimal parseAmount(String value) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException("请输入金额。");
        }
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            return amount.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("金额格式不正确。");
        }
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
