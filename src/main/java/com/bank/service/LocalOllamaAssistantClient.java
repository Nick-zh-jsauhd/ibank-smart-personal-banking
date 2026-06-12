package com.bank.service;

import com.bank.dto.ServiceResult;
import com.bank.util.AiConfig;
import com.bank.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LocalOllamaAssistantClient implements AssistantLlmClient {
    @Override
    public boolean isConfigured() {
        return AiConfig.localLlmEnabled()
                && hasText(AiConfig.localLlmBaseUrl())
                && hasText(AiConfig.localLlmModel());
    }

    @Override
    public String sourceCode() {
        return "LOCAL_LLM";
    }

    @Override
    public String providerLabel() {
        return "本地模型增强";
    }

    @Override
    public String modelName() {
        return AiConfig.localLlmModel();
    }

    @Override
    public ServiceResult<String> chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return ServiceResult.failure("本地模型未配置，已使用本地业务解释。");
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(AiConfig.localLlmBaseUrl() + "/api/chat");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(AiConfig.localLlmTimeoutMs());
            connection.setReadTimeout(AiConfig.localLlmTimeoutMs());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            byte[] body = requestBody(systemPrompt, userPrompt).getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int status = connection.getResponseCode();
            String response = read(status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            if (status < 200 || status >= 300) {
                return ServiceResult.failure("本地模型调用失败，HTTP " + status + "：" + truncate(response, 180));
            }
            String content = extractContent(response);
            if (!hasText(content)) {
                return ServiceResult.failure("本地模型返回内容为空，已使用本地业务解释。");
            }
            return ServiceResult.success("本地模型已生成回答。", content.trim());
        } catch (Exception e) {
            return ServiceResult.failure("本地模型调用异常：" + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String requestBody(String systemPrompt, String userPrompt) {
        StringBuilder json = new StringBuilder(2048);
        json.append("{");
        json.append("\"model\":").append(JsonUtil.quote(AiConfig.localLlmModel()));
        json.append(",\"messages\":[");
        json.append("{\"role\":\"system\",\"content\":").append(JsonUtil.quote(systemPrompt)).append("}");
        json.append(",{\"role\":\"user\",\"content\":").append(JsonUtil.quote(userPrompt)).append("}");
        json.append("]");
        json.append(",\"stream\":false");
        json.append(",\"options\":{");
        json.append("\"temperature\":").append(AiConfig.localLlmTemperature());
        json.append(",\"num_predict\":").append(AiConfig.localLlmMaxTokens());
        json.append("}");
        json.append("}");
        return json.toString();
    }

    private String read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private String extractContent(String json) {
        int message = json.indexOf("\"message\"");
        int content = message < 0 ? -1 : json.indexOf("\"content\"", message);
        if (content < 0) {
            content = json.indexOf("\"response\"");
        }
        if (content < 0) {
            return "";
        }
        int colon = json.indexOf(':', content);
        if (colon < 0) {
            return "";
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = start + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaping) {
                appendEscaped(builder, ch, json, i);
                if (ch == 'u') {
                    i += 4;
                }
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else if (ch == '"') {
                return builder.toString();
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private void appendEscaped(StringBuilder builder, char ch, String json, int index) {
        switch (ch) {
            case '"':
                builder.append('"');
                break;
            case '\\':
                builder.append('\\');
                break;
            case '/':
                builder.append('/');
                break;
            case 'b':
                builder.append('\b');
                break;
            case 'f':
                builder.append('\f');
                break;
            case 'n':
                builder.append('\n');
                break;
            case 'r':
                builder.append('\r');
                break;
            case 't':
                builder.append('\t');
                break;
            case 'u':
                if (index + 4 < json.length()) {
                    String hex = json.substring(index + 1, index + 5);
                    try {
                        builder.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            default:
                builder.append(ch);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "...";
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
