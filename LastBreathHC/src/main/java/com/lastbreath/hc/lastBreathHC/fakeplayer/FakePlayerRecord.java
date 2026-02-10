package com.lastbreath.hc.lastBreathHC.fakeplayer;

import java.time.Instant;
import java.util.UUID;

public class FakePlayerRecord {
    private String name;
    private UUID uuid;
    private String skinOwner;
    private String textures;
    private String signature;
    private boolean active;
    private boolean muted;
    private Instant createdAt;
    private Instant lastSeenAt;
    private Instant lastChatAt;
    private Instant lastReactionAt;
    private long chatCount;
    private long reactionCount;
    private String tabTitleKey;
    private int tabPingMillis;

    public FakePlayerRecord() {
    }

    public FakePlayerRecord(String name, UUID uuid, String skinOwner, String textures, String signature) {
        this.name = name;
        this.uuid = uuid;
        this.skinOwner = skinOwner;
        this.textures = textures;
        this.signature = signature;
        this.active = true;
        this.muted = false;
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastSeenAt = now;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getSkinOwner() {
        return skinOwner;
    }

    public void setSkinOwner(String skinOwner) {
        this.skinOwner = skinOwner;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getLastChatAt() {
        return lastChatAt;
    }

    public void setLastChatAt(Instant lastChatAt) {
        this.lastChatAt = lastChatAt;
    }

    public Instant getLastReactionAt() {
        return lastReactionAt;
    }

    public void setLastReactionAt(Instant lastReactionAt) {
        this.lastReactionAt = lastReactionAt;
    }

    public long getChatCount() {
        return chatCount;
    }

    public void setChatCount(long chatCount) {
        this.chatCount = chatCount;
    }

    public long getReactionCount() {
        return reactionCount;
    }

    public void setReactionCount(long reactionCount) {
        this.reactionCount = reactionCount;
    }

    public String getTabTitleKey() {
        return tabTitleKey;
    }

    public void setTabTitleKey(String tabTitleKey) {
        this.tabTitleKey = tabTitleKey;
    }

    public int getTabPingMillis() {
        return tabPingMillis;
    }

    public void setTabPingMillis(int tabPingMillis) {
        this.tabPingMillis = tabPingMillis;
    }
}
