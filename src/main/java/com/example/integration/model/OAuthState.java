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

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.sql.Timestamp createdAt = java.sql.Timestamp.from(Instant.now());

    public OAuthState() {
    }

    public OAuthState(UUID id, String state) {
        this.id = id;
        this.state = state;
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
}


