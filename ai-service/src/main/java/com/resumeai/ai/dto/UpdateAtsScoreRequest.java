package com.resumeai.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateAtsScoreRequest(
        @NotNull
        @Min(0)
        @Max(100)
        Integer atsScore
) {
}
