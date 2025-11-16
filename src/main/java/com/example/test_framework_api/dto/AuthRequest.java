package com.example.test_framework_api.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}