package com.example.integration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oauth_state")
public class OAuthState {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String state;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "redirect_uri", nullable = false, columnDefinition = "TEXT")
    private String redirectUri;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.sql.Timestamp createdAt = java.sql.Timestamp.from(Instant.now());

    @Column(nullable = false)
    private Boolean consumed = false;

    public OAuthState() {
    }

    public OAuthState(UUID id, String state, String provider, String redirectUri) {
        this.id = id;
        this.state = state;
        this.provider = provider;
        this.redirectUri = redirectUri;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public Boolean getConsumed() {
        return consumed;
    }

    public void setConsumed(Boolean consumed) {
        this.consumed = consumed;
    }
}


