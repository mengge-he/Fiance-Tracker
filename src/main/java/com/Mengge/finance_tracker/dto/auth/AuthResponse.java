package com.Mengge.finance_tracker.dto.auth;

public record AuthResponse(String token, long expiresInMs) {}
