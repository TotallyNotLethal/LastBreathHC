package com.lastbreath.hc.lastBreathHC.bounty;

import java.time.Instant;
import java.util.UUID;

public class BountyRecord {
    public UUID targetUuid;
    public String targetName;
    public Instant createdAt;
    public long accumulatedOnlineSeconds;
    public long accumulatedOnlineTicks;
    public Instant lastLogoutInstant;
    public int rewardTier;
    public double rewardValue;

    public BountyRecord() {
    }

    public BountyRecord(UUID targetUuid) {
        this.targetUuid = targetUuid;
        this.createdAt = Instant.now();
    }
}
