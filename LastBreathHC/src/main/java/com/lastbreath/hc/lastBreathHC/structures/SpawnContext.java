package com.lastbreath.hc.lastBreathHC.structures;

public record SpawnContext(
        String ownerCaptainId,
        String region,
        long lastUpgradeTimestamp
) {
    public static SpawnContext empty() {
        return new SpawnContext("unknown", "unknown", System.currentTimeMillis());
    }
}
