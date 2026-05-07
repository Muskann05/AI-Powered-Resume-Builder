package com.resumeai.section.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleVisibilityRequest(@NotNull Boolean isVisible) {
}
