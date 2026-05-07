package com.resumeai.ai.service;

import com.resumeai.ai.enums.AiModel;

public interface AiProviderService {

    ProviderResult generate(String prompt, int maxTokens);

    record ProviderResult(String output, AiModel model, Integer tokensUsed) {}
}
