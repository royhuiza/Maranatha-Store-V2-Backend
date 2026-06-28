package com.libreria.auth.dto;

public record AuthResponse(String token, String username, String rol) {
}
