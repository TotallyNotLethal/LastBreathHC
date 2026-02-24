package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        Telemetry telemetry,
        Optional<Political> political,
        Optional<Social> social,
        Optional<Relationships> relationships,
        Optional<Memory> memory,
        Optional<Persona> persona
) {
    public CaptainRecord {
        political = optionalOrEmpty(political);
        social = optionalOrEmpty(social);
        relationships = optionalOrEmpty(relationships);
        memory = optionalOrEmpty(memory);
        persona = optionalOrEmpty(persona);
    }

    public CaptainRecord(
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
        this(identity, origin, victims, nemesisScores, progression, naming, traits, minionPack, state, telemetry,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public CaptainRecord(
            Identity identity,
            Origin origin,
            Victims victims,
            NemesisScores nemesisScores,
            Progression progression,
            Naming naming,
            Traits traits,
            MinionPack minionPack,
            State state,
            Telemetry telemetry,
            Political political,
            Social social,
            Relationships relationships,
            Memory memory,
            Persona persona
    ) {
        this(identity, origin, victims, nemesisScores, progression, naming, traits, minionPack, state, telemetry,
                Optional.ofNullable(political), Optional.ofNullable(social), Optional.ofNullable(relationships),
                Optional.ofNullable(memory), Optional.ofNullable(persona));
    }

    public static CaptainRecord withOptionalSections(
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
        return new CaptainRecord(identity, origin, victims, nemesisScores, progression, naming, traits, minionPack, state,
                telemetry);
    }

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

    public record Political(String rank, String region, String seatId, double promotionScore, double influence) {
    }

    public record Social(double loyalty, double fear, double ambition, double confidence) {
    }

    public record Relationships(List<UUID> allies, List<UUID> rivals, UUID bodyguardOf, UUID bloodBrotherOf) {
    }

    public record Memory(String lastDefeatCause, List<String> scars, List<String> humiliations, List<UUID> notablePlayers,
                         long callbackLinesSeed) {
    }

    public record Persona(String archetype, String temperament, List<String> quirkTags, String voicePackId) {
    }

    private static <T> Optional<T> optionalOrEmpty(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }
}
