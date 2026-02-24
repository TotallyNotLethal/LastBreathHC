package com.lastbreath.hc.lastBreathHC.nemesis;

public record TerritoryPressureThresholdCrossedEvent(
        String region,
        String reason,
        double previousPressure,
        double currentPressure,
        double threshold,
        long occurredAtEpochMillis
) {
}
