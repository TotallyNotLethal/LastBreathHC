package com.lastbreath.hc.lastBreathHC.fakeplayer;

import java.time.Instant;

public class SkinCacheEntry {
    private String owner;
    private String textures;
    private String signature;
    private Instant fetchedAt;
    private Instant expiresAt;

    public SkinCacheEntry() {
    }

    public SkinCacheEntry(String owner, String textures, String signature, Instant fetchedAt, Instant expiresAt) {
        this.owner = owner;
        this.textures = textures;
        this.signature = signature;
        this.fetchedAt = fetchedAt;
        this.expiresAt = expiresAt;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTextures() {
        return textures;
    }

    public void setTextures(String textures) {
        this.textures = textures;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
