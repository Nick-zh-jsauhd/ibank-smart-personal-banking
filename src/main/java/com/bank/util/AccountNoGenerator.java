package com.bank.util;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class AccountNoGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountNoGenerator() {
    }

    public static String generate() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int suffix = 1000 + RANDOM.nextInt(9000);
        return "622" + timestamp + suffix;
    }
}
