package com.resumeai.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.enums.AiModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GeminiProviderService implements AiProviderService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FallbackAiGenerator fallbackAiGenerator;

    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${ai.gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${ai.gemini.model}")
    private String geminiModel;

    @Value("${ai.fallback.enabled:true}")
    private boolean fallbackEnabled;

    public GeminiProviderService(RestTemplate restTemplate, FallbackAiGenerator fallbackAiGenerator) {
        this.restTemplate = restTemplate;
        this.fallbackAiGenerator = fallbackAiGenerator;
    }

    @Override
    public ProviderResult generate(String prompt, int maxTokens) {
        int maxRetries = 3;
        long waitMs = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return callGemini(prompt, maxTokens);
            } catch (HttpStatusCodeException ex) {
                int code = ex.getStatusCode().value();

                if ((code == 429 || code == 503) && attempt < maxRetries) {
                    sleep(waitMs * attempt);
                    continue;
                }

                if (fallbackEnabled) {
                    return new ProviderResult(
                            fallbackAiGenerator.generateFallback(prompt),
                            AiModel.FALLBACK,
                            0
                    );
                }

                throw new RuntimeException("Gemini call failed: " + ex.getResponseBodyAsString());
            } catch (Exception ex) {
                if (attempt < maxRetries) {
                    sleep(waitMs * attempt);
                    continue;
                }

                if (fallbackEnabled) {
                    return new ProviderResult(
                            fallbackAiGenerator.generateFallback(prompt),
                            AiModel.FALLBACK,
                            0
                    );
                }

                throw new RuntimeException("Gemini call failed: " + ex.getMessage());
            }
        }

        if (fallbackEnabled) {
            return new ProviderResult(
                    fallbackAiGenerator.generateFallback(prompt),
                    AiModel.FALLBACK,
                    0
            );
        }

        throw new RuntimeException("Gemini call failed after retries");
    }

    private ProviderResult callGemini(String prompt, int maxTokens) throws Exception {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new RuntimeException("Gemini API key is missing");
        }

        String url = geminiBaseUrl + "/" + geminiModel + ":generateContent?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", new Object[]{textPart});

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("temperature", 0.7);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", new Object[]{content});
        body.put("generationConfig", generationConfig);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String blockReason = root.path("promptFeedback").path("blockReason").asText("");
            throw new RuntimeException("Gemini returned no candidates. " + blockReason);
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode parts = firstCandidate.path("content").path("parts");

        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("Gemini returned empty content");
        }

        String text = parts.get(0).path("text").asText("");
        if (text.isBlank()) {
            throw new RuntimeException("Gemini returned blank text");
        }

        int totalTokens = root.path("usageMetadata").path("totalTokenCount").asInt(0);

        return new ProviderResult(text, AiModel.GEMINI, totalTokens);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
