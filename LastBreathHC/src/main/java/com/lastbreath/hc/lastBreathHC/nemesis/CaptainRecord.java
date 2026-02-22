package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CaptainRecord(
        Identity identity,
        Origin origin,
        Victims victims,
        NemesisScores nemesisScores,
        Progression progression,
        Naming naming,
        Traits traits,
        MinionPack minionPack,
        State state,
        Telemetry telemetry
) {
    public record Identity(UUID captainId, UUID nemesisOf, long createdAtEpochMillis) {
    }

    public record Origin(String world, int chunkX, int chunkZ, double spawnX, double spawnY, double spawnZ, String biome) {
    }

    public record Victims(List<UUID> playerVictims, int totalVictimCount, long lastVictimAtEpochMillis) {
    }

    public record NemesisScores(double threat, double rivalry, double brutality, double cunning) {
    }

    public record Progression(int level, long experience, String tier) {
    }

    public record Naming(String displayName, String epithet, String title, String aliasSeed) {
    }

    public record Traits(List<String> traits, List<String> weaknesses, List<String> immunities) {
    }

    public record MinionPack(String packType, int minionCount, List<String> minionArchetypes, double reinforcementChance) {
    }

    public record State(CaptainState state, long cooldownUntilEpochMs, long lastSeenEpochMs, UUID runtimeEntityUuid) {
    }

    public record Telemetry(long lastSeenAtEpochMillis, long lastUpdatedAtEpochMillis, int encounters, Map<String, Long> counters) {
    }
}
