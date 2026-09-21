package com.brijesh.notify.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String fullName
) {
}
