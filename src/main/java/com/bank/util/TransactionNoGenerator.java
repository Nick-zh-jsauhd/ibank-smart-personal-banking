package com.bank.util;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class TransactionNoGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TransactionNoGenerator() {
    }

    public static String generate() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        int suffix = 100000 + RANDOM.nextInt(900000);
        return "TXN" + timestamp + suffix;
    }
}
