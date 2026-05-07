package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record SkillSuggestionRequest(
        @NotBlank String userId,
        @NotBlank String targetJobTitle
) {}
