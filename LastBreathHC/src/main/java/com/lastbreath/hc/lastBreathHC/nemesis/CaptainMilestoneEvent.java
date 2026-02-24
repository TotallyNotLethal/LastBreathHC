package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.UUID;

public record CaptainMilestoneEvent(
        UUID captainId,
        MilestoneType milestoneType,
        String key,
        double previousValue,
        double currentValue,
        long occurredAtEpochMillis
) {
    public enum MilestoneType {
        ENCOUNTERS_SURVIVED,
        PROMOTION_SCORE_THRESHOLD,
        PRESSURE_THRESHOLD
    }
}
