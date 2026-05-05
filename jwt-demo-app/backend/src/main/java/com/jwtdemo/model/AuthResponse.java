package com.jwtdemo.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private String username;
    private List<String> roles;
    // Educational fields — show what's inside the JWT
    private String jwtHeader;
    private String jwtPayload;
    private String jwtSignature;
    private Map<String, Object> tokenInfo;
    private String message;
}
