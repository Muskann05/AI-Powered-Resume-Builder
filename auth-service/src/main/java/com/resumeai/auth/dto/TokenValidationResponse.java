package com.resumeai.auth.dto;

public record TokenValidationResponse(
        boolean valid,
        String message
) {
}
