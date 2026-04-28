package com.example.oauth2.security;

import java.util.Map;

// ── Google OAuth2 User Info ───────────────────────────────────────────────────
class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override public String getId()       { return (String) attributes.get("sub"); }
    @Override public String getName()     { return (String) attributes.get("name"); }
    @Override public String getEmail()    { return (String) attributes.get("email"); }
    @Override public String getImageUrl() { return (String) attributes.get("picture"); }
}

// ── GitHub OAuth2 User Info ───────────────────────────────────────────────────
class GitHubOAuth2UserInfo extends OAuth2UserInfo {

    GitHubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override public String getId()       { return String.valueOf(attributes.get("id")); }
    @Override public String getName()     { return (String) attributes.get("name"); }
    @Override public String getEmail()    { return (String) attributes.get("email"); }
    @Override public String getImageUrl() { return (String) attributes.get("avatar_url"); }
}

// ── Factory — picks the right implementation ──────────────────────────────────
class OAuth2UserInfoFactory {

    static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                            Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "github" -> new GitHubOAuth2UserInfo(attributes);
            default       -> throw new IllegalArgumentException(
                    "Unsupported OAuth2 provider: " + registrationId);
        };
    }
}
