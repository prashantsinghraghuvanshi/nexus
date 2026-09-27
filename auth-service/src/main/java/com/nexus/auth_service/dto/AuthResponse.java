package com.nexus.auth_service.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthResponse {

    private boolean success;
    private String message;
    private Long userId;
    private String username;
}