package com.example.oauth2.controller;

import com.example.oauth2.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Landing page after successful OAuth2 login.
 * The OAuth2SuccessHandler redirects here with the JWT as a query parameter.
 *
 * URL: GET http://localhost:8080/oauth2/success?token=eyJhb...
 *
 * In a real app this would redirect to your frontend (Angular / React).
 * For this demo we return the token as plain JSON.
 */
@RestController
@RequiredArgsConstructor
public class OAuth2SuccessController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/oauth2/success")
    public String oAuth2Success(@RequestParam(required = false) String token) {
        if (token == null) {
            return "OAuth2 login failed — no token received.";
        }
        return """
               {
                 "message": "OAuth2 login successful!",
                 "token": "%s",
                 "usage": "Add this to your request headers: Authorization: Bearer %s"
               }
               """.formatted(token, token);
    }
}
