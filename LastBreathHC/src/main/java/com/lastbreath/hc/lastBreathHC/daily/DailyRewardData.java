package com.lastbreath.hc.lastBreathHC.daily;

import java.util.UUID;

public class DailyRewardData {

    private final UUID uuid;
    private long lastClaimEpochDay;
    private int currentStreak;
    private int maxStreak;
    private Long lastJoinEpochDay;

    public DailyRewardData(UUID uuid) {
        this.uuid = uuid;
        this.lastClaimEpochDay = -1L;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.lastJoinEpochDay = null;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getLastClaimEpochDay() {
        return lastClaimEpochDay;
    }

    public void setLastClaimEpochDay(long lastClaimEpochDay) {
        this.lastClaimEpochDay = lastClaimEpochDay;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = Math.max(0, currentStreak);
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public void setMaxStreak(int maxStreak) {
        this.maxStreak = Math.max(0, maxStreak);
    }

    public Long getLastJoinEpochDay() {
        return lastJoinEpochDay;
    }

    public void setLastJoinEpochDay(Long lastJoinEpochDay) {
        this.lastJoinEpochDay = lastJoinEpochDay;
    }
}
