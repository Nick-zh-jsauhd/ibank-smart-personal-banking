package com.bank.util;

import java.io.InputStream;
import java.util.Properties;

public final class AiConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = AiConfig.class.getClassLoader().getResourceAsStream("ai.properties")) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
            }
        } catch (Exception ignored) {
        }
    }

    private AiConfig() {
    }

    public static String provider() {
        return value("ai.provider", "AI_PROVIDER", "local");
    }

    public static boolean localLlmEnabled() {
        return booleanValue("localLlm.enabled", true);
    }

    public static String localLlmBaseUrl() {
        return trimTrailingSlash(value("localLlm.baseUrl", "LOCAL_LLM_BASE_URL", "http://127.0.0.1:11434"));
    }

    public static String localLlmModel() {
        return value("localLlm.model", "LOCAL_LLM_MODEL", "qwen3:8b");
    }

    public static int localLlmTimeoutMs() {
        return intValue("localLlm.timeoutMs", "LOCAL_LLM_TIMEOUT_MS", 15000);
    }

    public static int localLlmMaxTokens() {
        return intValue("localLlm.maxTokens", "LOCAL_LLM_MAX_TOKENS", 160);
    }

    public static String localLlmTemperature() {
        return value("localLlm.temperature", "LOCAL_LLM_TEMPERATURE", "0.2");
    }

    public static boolean deepSeekEnabled() {
        return booleanValue("deepseek.enabled", false);
    }

    public static String deepSeekApiKey() {
        return value("deepseek.apiKey", "DEEPSEEK_API_KEY", "");
    }

    public static String deepSeekBaseUrl() {
        return trimTrailingSlash(value("deepseek.baseUrl", "DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
    }

    public static String deepSeekModel() {
        return value("deepseek.model", "DEEPSEEK_MODEL", "deepseek-v4-flash");
    }

    public static int deepSeekTimeoutMs() {
        return intValue("deepseek.timeoutMs", "DEEPSEEK_TIMEOUT_MS", 10000);
    }

    public static int deepSeekMaxTokens() {
        return intValue("deepseek.maxTokens", "DEEPSEEK_MAX_TOKENS", 900);
    }

    public static String deepSeekTemperature() {
        return value("deepseek.temperature", "DEEPSEEK_TEMPERATURE", "0.2");
    }

    public static String deepSeekThinkingType() {
        return value("deepseek.thinkingType", "DEEPSEEK_THINKING_TYPE", "disabled");
    }

    private static boolean booleanValue(String key, boolean defaultValue) {
        String value = value(key, envKey(key), String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private static int intValue(String propertyKey, String envKey, int defaultValue) {
        String raw = value(propertyKey, envKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String value(String propertyKey, String envKey, String defaultValue) {
        String systemValue = System.getProperty(propertyKey);
        if (hasText(systemValue)) {
            return systemValue.trim();
        }
        String envValue = System.getenv(envKey);
        if (hasText(envValue)) {
            return envValue.trim();
        }
        String propertyValue = PROPERTIES.getProperty(propertyKey);
        if (hasText(propertyValue)) {
            return propertyValue.trim();
        }
        return defaultValue;
    }

    private static String envKey(String propertyKey) {
        return propertyKey.toUpperCase().replace('.', '_');
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
