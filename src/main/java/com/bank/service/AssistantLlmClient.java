package com.bank.service;

import com.bank.dto.ServiceResult;

public interface AssistantLlmClient {
    boolean isConfigured();

    String sourceCode();

    String providerLabel();

    String modelName();

    ServiceResult<String> chat(String systemPrompt, String userPrompt);
}
