package com.example.oauth2.security;

import java.util.Map;

/**
 * Abstract base class for OAuth2 user info.
 * Each provider (Google, GitHub) returns different attribute names —
 * this normalises them into a common interface.
 */
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() { return attributes; }

    public abstract String getId();
    public abstract String getName();
    public abstract String getEmail();
    public abstract String getImageUrl();
}
