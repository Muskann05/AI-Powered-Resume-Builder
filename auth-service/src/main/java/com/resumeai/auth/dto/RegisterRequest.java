package com.resumeai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 60) String password,
        @Pattern(regexp = "^[0-9+\\-() ]{0,25}$", message = "Invalid phone number") String phone) {}
