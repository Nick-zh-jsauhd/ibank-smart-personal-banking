package com.bank.service;

import com.bank.util.AiConfig;

public final class AssistantLlmClientFactory {
    private AssistantLlmClientFactory() {
    }

    public static AssistantLlmClient create() {
        String provider = AiConfig.provider();
        if ("deepseek".equalsIgnoreCase(provider)) {
            return new DeepSeekAssistantClient();
        }
        return new LocalOllamaAssistantClient();
    }
}
