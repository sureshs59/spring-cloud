package com.example.oauth2.security;

import com.example.oauth2.model.AuthProvider;
import com.example.oauth2.model.Role;
import com.example.oauth2.model.User;
import com.example.oauth2.service.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Called by Spring Security after a successful OAuth2 authentication.
 *
 * Responsibilities:
 *  1. Fetch user info from the provider (Google / GitHub)
 *  2. Check if this user already exists in our database
 *  3. If new → register them automatically
 *  4. If existing → update their name and picture (info may have changed)
 *  5. Return a Spring Security OAuth2User principal
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser>  {

    private final UserRepository userRepository;

    // Use the default service to fetch user info from the provider
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    /**
     * Process OAuth2 login — called by Spring Security automatically.
     */
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest request, OAuth2User oAuth2User) {
        String registrationId = request.getClientRegistration().getRegistrationId();

        // Normalise provider-specific attributes
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory
                .getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new RuntimeException("Email not found from OAuth2 provider: " + registrationId);
        }

        Optional<User> userOptional = userRepository.findByEmail(userInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            // Existing user — update profile info
            user = userOptional.get();
            validateProvider(user, registrationId);
            user = updateExistingUser(user, userInfo);
            log.info("OAuth2 login — existing user updated: {}", user.getEmail());
        } else {
            // New user — register them automatically
            user = registerNewUser(request, userInfo);
            log.info("OAuth2 login — new user registered: {}", user.getEmail());
        }

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(user.getRole().name())),
                oAuth2User.getAttributes(),
                request.getClientRegistration()
                        .getProviderDetails().getUserInfoEndpoint()
                        .getUserNameAttributeName()
        );
    }

    private void validateProvider(User existingUser, String registrationId) {
        if (!existingUser.getProvider().name().equals(registrationId)) {
            throw new RuntimeException(
                    "Account registered with " + existingUser.getProvider() +
                    ". Please use " + existingUser.getProvider() + " login.");
        }
    }

    private User registerNewUser(OAuth2UserRequest request, OAuth2UserInfo userInfo) {
        User user = User.builder()
                .name(userInfo.getName())
                .email(userInfo.getEmail())
                .imageUrl(userInfo.getImageUrl())
                .provider(AuthProvider.valueOf(
                        request.getClientRegistration().getRegistrationId()))
                .providerId(userInfo.getId())
                .emailVerified(true)   // OAuth2 providers verify email
                .role(Role.ROLE_USER)
                .build();
        return userRepository.save(user);
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        user.setName(userInfo.getName());
        user.setImageUrl(userInfo.getImageUrl());
        return userRepository.save(user);
    }

    // Required by interface — not used for standard OAuth2
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        return null;
    }
}
