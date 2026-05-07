package com.resumeai.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
	
}
