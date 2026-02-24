package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.Location;

import java.util.UUID;

public record PlayerRaidInteractionEvent(
        UUID playerId,
        String playerName,
        RaidTargetType targetType,
        String region,
        Location location,
        long occurredAtEpochMillis
) {
    public enum RaidTargetType {
        BANNER,
        THRONE,
        HEART
    }
}
