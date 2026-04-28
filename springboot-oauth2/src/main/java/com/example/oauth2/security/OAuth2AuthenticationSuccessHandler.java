package com.example.oauth2.security;

import com.example.oauth2.service.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Called by Spring Security after a successful OAuth2 login.
 *
 * Generates a JWT token and redirects to the configured success URL
 * with the token as a query parameter.
 *
 * Redirect: http://localhost:8080/oauth2/success?token=eyJhb...
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository   userRepository;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        Authentication      authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            log.error("Email not available from OAuth2 provider");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not available");
            return;
        }

        // Generate JWT for the OAuth2 user
        String token = jwtTokenProvider.generateTokenFromEmail(email);
        log.info("OAuth2 login successful for: {} — JWT generated", email);

        // Redirect with the token as a query parameter
        String redirectUrl = UriComponentsBuilder
                .fromUriString(authorizedRedirectUri)
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
