package com.example.integration.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_client_links")
public class UserClientLink {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_application_id", nullable = false)
    private ClientApplication clientApplication;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.sql.Timestamp createdAt = java.sql.Timestamp.from(Instant.now());

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public ClientApplication getClientApplication() {
        return clientApplication;
    }

    public void setClientApplication(ClientApplication clientApplication) {
        this.clientApplication = clientApplication;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}


