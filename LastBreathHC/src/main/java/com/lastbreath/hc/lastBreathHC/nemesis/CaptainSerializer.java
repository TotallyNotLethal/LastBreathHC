package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CaptainSerializer {
    private static final int CURRENT_SCHEMA_VERSION = 2;

    private final LastBreathHC plugin;
    private final File file;
    private final Map<UUID, Map<String, Object>> extrasByCaptainId = new HashMap<>();
    private boolean upgradedOnNextSave;

    public CaptainSerializer(LastBreathHC plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public List<CaptainRecord> load() {
        List<CaptainRecord> records = new ArrayList<>();
        extrasByCaptainId.clear();
        upgradedOnNextSave = false;
        if (!file.exists()) {
            return records;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int schemaVersion = config.getInt("schemaVersion", 1);
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            backupUnsupportedSchemaFile(schemaVersion);
            return records;
        }

        if (schemaVersion < CURRENT_SCHEMA_VERSION) {
            migrate(config, schemaVersion);
            upgradedOnNextSave = true;
            plugin.getLogger().warning("Loaded legacy nemesis captain schema version " + schemaVersion
                    + "; ordered migrations were applied and data will be upgraded on the next save.");
        }

        ConfigurationSection captains = config.getConfigurationSection("captains");
        if (captains == null) {
            return records;
        }

        for (String key : captains.getKeys(false)) {
            ConfigurationSection row = captains.getConfigurationSection(key);
            if (row == null) {
                continue;
            }

            UUID captainUuid;
            try {
                captainUuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            CaptainRecord record = parseCaptain(captainUuid, row);
            if (record != null) {
                records.add(record);
            }
        }

        return records;
    }

    public void saveDirty(Collection<CaptainRecord> records, Set<UUID> dirtyCaptainIds) {
        if ((dirtyCaptainIds == null || dirtyCaptainIds.isEmpty()) && !upgradedOnNextSave) {
            return;
        }
        save(records);
        upgradedOnNextSave = false;
    }

    public void save(Collection<CaptainRecord> records) {
        ensureParentDirectory();

        YamlConfiguration config = new YamlConfiguration();
        config.set("schemaVersion", CURRENT_SCHEMA_VERSION);

        if (records != null) {
            for (CaptainRecord record : records) {
                if (record == null || record.identity() == null || record.identity().captainId() == null) {
                    continue;
                }

                String base = "captains." + record.identity().captainId();
                writeCaptain(config, base, record);
            }
        }

        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(tempFile);
            moveAtomic(tempFile, file);
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save nemesis captains: " + e.getMessage());
        } finally {
            if (tempFile.exists() && !tempFile.equals(file)) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    private CaptainRecord parseCaptain(UUID captainUuid, ConfigurationSection row) {
        UUID nemesisOf = parseUuid(row.getString("identity.nemesisOf"));
        long createdAt = row.getLong("identity.createdAtEpochMillis", System.currentTimeMillis());

        CaptainRecord.Identity identity = new CaptainRecord.Identity(captainUuid, nemesisOf, createdAt);
        CaptainRecord.Origin origin = new CaptainRecord.Origin(
                row.getString("origin.world", "world"),
                row.getInt("origin.chunkX", 0),
                row.getInt("origin.chunkZ", 0),
                row.getDouble("origin.spawnX", 0.0),
                row.getDouble("origin.spawnY", 0.0),
                row.getDouble("origin.spawnZ", 0.0),
                row.getString("origin.biome", "UNKNOWN")
        );

        List<UUID> playerVictims = parseUuidList(row.getStringList("victims.playerVictims"));
        CaptainRecord.Victims victims = new CaptainRecord.Victims(
                playerVictims,
                row.getInt("victims.totalVictimCount", playerVictims.size()),
                row.getLong("victims.lastVictimAtEpochMillis", 0L)
        );

        CaptainRecord.NemesisScores nemesisScores = new CaptainRecord.NemesisScores(
                row.getDouble("nemesisScores.threat", 0.0),
                row.getDouble("nemesisScores.rivalry", 0.0),
                row.getDouble("nemesisScores.brutality", 0.0),
                row.getDouble("nemesisScores.cunning", 0.0)
        );

        CaptainRecord.Progression progression = new CaptainRecord.Progression(
                row.getInt("progression.level", 1),
                row.getLong("progression.experience", 0L),
                row.getString("progression.tier", "COMMON")
        );

        CaptainRecord.Naming naming = new CaptainRecord.Naming(
                row.getString("naming.displayName", "Unknown Captain"),
                row.getString("naming.epithet", ""),
                row.getString("naming.title", ""),
                row.getString("naming.aliasSeed", "")
        );

        CaptainRecord.Traits traits = new CaptainRecord.Traits(
                row.getStringList("traits.traits"),
                row.getStringList("traits.weaknesses"),
                row.getStringList("traits.immunities")
        );

        CaptainRecord.MinionPack minionPack = new CaptainRecord.MinionPack(
                row.getString("minionPack.packType", "NONE"),
                row.getInt("minionPack.minionCount", 0),
                row.getStringList("minionPack.minionArchetypes"),
                row.getDouble("minionPack.reinforcementChance", 0.0)
        );

        CaptainRecord.State state = parseState(row, parseUuid(row.getString("identity.spawnEntityUuid")));

        CaptainRecord.Telemetry telemetry = new CaptainRecord.Telemetry(
                row.getLong("telemetry.lastSeenAtEpochMillis", 0L),
                row.getLong("telemetry.lastUpdatedAtEpochMillis", 0L),
                row.getInt("telemetry.encounters", 0),
                readCounters(row.getConfigurationSection("telemetry.counters"))
        );

        CaptainRecord.Political political = parsePolitical(row);
        CaptainRecord.Social social = parseSocial(row);
        CaptainRecord.Relationships relationships = parseRelationships(row);
        CaptainRecord.Memory memory = parseMemory(row);
        CaptainRecord.Persona persona = parsePersona(row);

        extrasByCaptainId.put(captainUuid, readExtras(row));

        return new CaptainRecord(
                identity,
                origin,
                victims,
                nemesisScores,
                progression,
                naming,
                traits,
                minionPack,
                state,
                telemetry,
                political,
                social,
                relationships,
                memory,
                persona
        );
    }

    private void writeCaptain(YamlConfiguration config, String base, CaptainRecord record) {
        config.set(base + ".identity.nemesisOf", stringify(record.identity().nemesisOf()));
        config.set(base + ".identity.createdAtEpochMillis", record.identity().createdAtEpochMillis());

        config.set(base + ".origin.world", record.origin().world());
        config.set(base + ".origin.chunkX", record.origin().chunkX());
        config.set(base + ".origin.chunkZ", record.origin().chunkZ());
        config.set(base + ".origin.spawnX", record.origin().spawnX());
        config.set(base + ".origin.spawnY", record.origin().spawnY());
        config.set(base + ".origin.spawnZ", record.origin().spawnZ());
        config.set(base + ".origin.biome", record.origin().biome());

        config.set(base + ".victims.playerVictims", stringifyUuidList(record.victims().playerVictims()));
        config.set(base + ".victims.totalVictimCount", record.victims().totalVictimCount());
        config.set(base + ".victims.lastVictimAtEpochMillis", record.victims().lastVictimAtEpochMillis());

        config.set(base + ".nemesisScores.threat", record.nemesisScores().threat());
        config.set(base + ".nemesisScores.rivalry", record.nemesisScores().rivalry());
        config.set(base + ".nemesisScores.brutality", record.nemesisScores().brutality());
        config.set(base + ".nemesisScores.cunning", record.nemesisScores().cunning());

        config.set(base + ".progression.level", record.progression().level());
        config.set(base + ".progression.experience", record.progression().experience());
        config.set(base + ".progression.tier", record.progression().tier());

        config.set(base + ".naming.displayName", record.naming().displayName());
        config.set(base + ".naming.epithet", record.naming().epithet());
        config.set(base + ".naming.title", record.naming().title());
        config.set(base + ".naming.aliasSeed", record.naming().aliasSeed());

        config.set(base + ".traits.traits", record.traits().traits());
        config.set(base + ".traits.weaknesses", record.traits().weaknesses());
        config.set(base + ".traits.immunities", record.traits().immunities());

        config.set(base + ".minionPack.packType", record.minionPack().packType());
        config.set(base + ".minionPack.minionCount", record.minionPack().minionCount());
        config.set(base + ".minionPack.minionArchetypes", record.minionPack().minionArchetypes());
        config.set(base + ".minionPack.reinforcementChance", record.minionPack().reinforcementChance());

        config.set(base + ".state.state", record.state().state().name());
        config.set(base + ".state.cooldownUntilEpochMs", record.state().cooldownUntilEpochMs());
        config.set(base + ".state.lastSeenEpochMs", record.state().lastSeenEpochMs());
        config.set(base + ".state.runtimeEntityUuid", stringify(record.state().runtimeEntityUuid()));

        config.set(base + ".telemetry.lastSeenAtEpochMillis", record.telemetry().lastSeenAtEpochMillis());
        config.set(base + ".telemetry.lastUpdatedAtEpochMillis", record.telemetry().lastUpdatedAtEpochMillis());
        config.set(base + ".telemetry.encounters", record.telemetry().encounters());

        String countersPath = base + ".telemetry.counters";
        config.set(countersPath, null);
        for (Map.Entry<String, Long> entry : record.telemetry().counters().entrySet()) {
            config.set(countersPath + "." + entry.getKey(), entry.getValue());
        }

        CaptainRecord.Political political = record.political().orElseGet(this::defaultPolitical);
        config.set(base + ".political.rank", political.rank());
        config.set(base + ".political.region", political.region());
        config.set(base + ".political.seatId", political.seatId());
        config.set(base + ".political.promotionScore", political.promotionScore());
        config.set(base + ".political.influence", political.influence());

        CaptainRecord.Social social = record.social().orElseGet(this::defaultSocial);
        config.set(base + ".social.loyalty", social.loyalty());
        config.set(base + ".social.fear", social.fear());
        config.set(base + ".social.ambition", social.ambition());
        config.set(base + ".social.confidence", social.confidence());

        CaptainRecord.Relationships relationships = record.relationships().orElseGet(this::defaultRelationships);
        config.set(base + ".relationships.allies", stringifyUuidList(relationships.allies()));
        config.set(base + ".relationships.rivals", stringifyUuidList(relationships.rivals()));
        config.set(base + ".relationships.bodyguardOf", stringify(relationships.bodyguardOf()));
        config.set(base + ".relationships.bloodBrotherOf", stringify(relationships.bloodBrotherOf()));

        CaptainRecord.Memory memory = record.memory().orElseGet(this::defaultMemory);
        config.set(base + ".memory.lastDefeatCause", memory.lastDefeatCause());
        config.set(base + ".memory.scars", memory.scars());
        config.set(base + ".memory.humiliations", memory.humiliations());
        config.set(base + ".memory.notablePlayers", stringifyUuidList(memory.notablePlayers()));
        config.set(base + ".memory.callbackLinesSeed", memory.callbackLinesSeed());

        CaptainRecord.Persona persona = record.persona().orElseGet(this::defaultPersona);
        config.set(base + ".persona.archetype", persona.archetype());
        config.set(base + ".persona.temperament", persona.temperament());
        config.set(base + ".persona.quirkTags", persona.quirkTags());
        config.set(base + ".persona.voicePackId", persona.voicePackId());

        Map<String, Object> extras = extrasByCaptainId.get(record.identity().captainId());
        if (extras == null || extras.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : extras.entrySet()) {
            config.set(base + "." + entry.getKey(), entry.getValue());
        }
    }


    private CaptainRecord.State parseState(ConfigurationSection row, UUID legacyRuntimeEntityUuid) {
        String rawState = row.getString("state.state");
        if (rawState != null) {
            CaptainState parsed = parseCaptainState(rawState, CaptainState.DORMANT);
            return new CaptainRecord.State(
                    parsed,
                    row.getLong("state.cooldownUntilEpochMs", 0L),
                    row.getLong("state.lastSeenEpochMs", row.getLong("telemetry.lastSeenAtEpochMillis", 0L)),
                    parseUuid(row.getString("state.runtimeEntityUuid"))
            );
        }

        String legacyStatus = row.getString("state.status", "DORMANT");
        boolean legacyActive = row.getBoolean("state.active", false);
        long spawnedAt = row.getLong("state.spawnedAtEpochMillis", 0L);
        long despawnedAt = row.contains("state.despawnedAtEpochMillis") ? row.getLong("state.despawnedAtEpochMillis") : 0L;

        CaptainState state;
        long cooldownUntil = 0L;
        if (legacyActive) {
            state = CaptainState.ACTIVE;
        } else if ("RETIRED".equalsIgnoreCase(legacyStatus)) {
            state = CaptainState.RETIRED;
        } else if ("DEAD".equalsIgnoreCase(legacyStatus)) {
            state = CaptainState.DEAD;
        } else if ("ESCAPED".equalsIgnoreCase(legacyStatus)) {
            state = CaptainState.COOLDOWN;
            cooldownUntil = despawnedAt;
        } else {
            state = CaptainState.DORMANT;
        }

        long lastSeen = despawnedAt > 0 ? despawnedAt : spawnedAt;
        return new CaptainRecord.State(state, cooldownUntil, lastSeen, legacyRuntimeEntityUuid);
    }

    private CaptainState parseCaptainState(String rawState, CaptainState fallback) {
        if (rawState == null || rawState.isBlank()) {
            return fallback;
        }
        try {
            return CaptainState.valueOf(rawState.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private Map<String, Long> readCounters(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Long> counters = new HashMap<>();
        for (String key : section.getKeys(false)) {
            counters.put(key, section.getLong(key, 0L));
        }
        return counters;
    }

    private CaptainRecord.Political parsePolitical(ConfigurationSection row) {
        CaptainRecord.Political defaults = defaultPolitical();
        return new CaptainRecord.Political(
                row.getString("political.rank", defaults.rank()),
                row.getString("political.region", defaults.region()),
                row.getString("political.seatId", defaults.seatId()),
                row.getDouble("political.promotionScore", defaults.promotionScore()),
                row.getDouble("political.influence", defaults.influence())
        );
    }

    private CaptainRecord.Social parseSocial(ConfigurationSection row) {
        CaptainRecord.Social defaults = defaultSocial();
        return new CaptainRecord.Social(
                row.getDouble("social.loyalty", defaults.loyalty()),
                row.getDouble("social.fear", defaults.fear()),
                row.getDouble("social.ambition", defaults.ambition()),
                row.getDouble("social.confidence", defaults.confidence())
        );
    }

    private CaptainRecord.Relationships parseRelationships(ConfigurationSection row) {
        CaptainRecord.Relationships defaults = defaultRelationships();
        return new CaptainRecord.Relationships(
                parseUuidList(row.getStringList("relationships.allies")),
                parseUuidList(row.getStringList("relationships.rivals")),
                parseUuid(row.getString("relationships.bodyguardOf", stringify(defaults.bodyguardOf()))),
                parseUuid(row.getString("relationships.bloodBrotherOf", stringify(defaults.bloodBrotherOf())))
        );
    }

    private CaptainRecord.Memory parseMemory(ConfigurationSection row) {
        CaptainRecord.Memory defaults = defaultMemory();
        return new CaptainRecord.Memory(
                row.getString("memory.lastDefeatCause", defaults.lastDefeatCause()),
                row.getStringList("memory.scars"),
                row.getStringList("memory.humiliations"),
                parseUuidList(row.getStringList("memory.notablePlayers")),
                row.getLong("memory.callbackLinesSeed", defaults.callbackLinesSeed())
        );
    }

    private CaptainRecord.Persona parsePersona(ConfigurationSection row) {
        CaptainRecord.Persona defaults = defaultPersona();
        return new CaptainRecord.Persona(
                row.getString("persona.archetype", defaults.archetype()),
                row.getString("persona.temperament", defaults.temperament()),
                row.getStringList("persona.quirkTags"),
                row.getString("persona.voicePackId", defaults.voicePackId())
        );
    }

    private CaptainRecord.Political defaultPolitical() {
        return new CaptainRecord.Political("UNALIGNED", "UNKNOWN", "", 0.0, 0.0);
    }

    private CaptainRecord.Social defaultSocial() {
        return new CaptainRecord.Social(0.0, 0.0, 0.0, 0.0);
    }

    private CaptainRecord.Relationships defaultRelationships() {
        return new CaptainRecord.Relationships(List.of(), List.of(), null, null);
    }

    private CaptainRecord.Memory defaultMemory() {
        return new CaptainRecord.Memory("", List.of(), List.of(), List.of(), 0L);
    }

    private CaptainRecord.Persona defaultPersona() {
        return new CaptainRecord.Persona("UNSPECIFIED", "NEUTRAL", List.of(), "");
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<UUID> parseUuidList(List<String> raw) {
        List<UUID> parsed = new ArrayList<>();
        for (String value : raw) {
            UUID uuid = parseUuid(value);
            if (uuid != null) {
                parsed.add(uuid);
            }
        }
        return parsed;
    }

    private List<String> stringifyUuidList(List<UUID> uuids) {
        List<String> values = new ArrayList<>();
        for (UUID uuid : uuids) {
            if (uuid != null) {
                values.add(uuid.toString());
            }
        }
        return values;
    }

    private String stringify(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    private void ensureParentDirectory() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
    }

    private void moveAtomic(File source, File target) throws IOException {
        try {
            Files.move(
                    source.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void backupUnsupportedSchemaFile(int schemaVersion) {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-');
        File backup = new File(file.getParentFile(), file.getName() + ".bak-" + timestamp);
        plugin.getLogger().severe("=====================================================");
        plugin.getLogger().severe("Unsupported nemesis captain schema version " + schemaVersion + " detected.");
        plugin.getLogger().severe("Attempting to move " + file.getName() + " to " + backup.getName() + " and continuing with empty in-memory captain state.");
        try {
            moveAtomic(file, backup);
            plugin.getLogger().severe("Backup completed: " + backup.getName());
        } catch (IOException moveError) {
            plugin.getLogger().severe("Failed to backup unsupported nemesis captain data: " + moveError.getMessage());
        }
        plugin.getLogger().severe("=====================================================");
    }

    private void migrate(YamlConfiguration config, int schemaVersion) {
        int workingVersion = schemaVersion;
        while (workingVersion < CURRENT_SCHEMA_VERSION) {
            switch (workingVersion) {
                case 0 -> migrateV0ToV1(config);
                case 1 -> migrateV1ToV2(config);
                default -> plugin.getLogger().warning("No explicit nemesis captain migration for schema version " + workingVersion + ".");
            }
            workingVersion++;
        }
    }

    private void migrateV0ToV1(YamlConfiguration config) {
        ConfigurationSection captains = config.getConfigurationSection("captains");
        if (captains == null) {
            return;
        }

        for (String captainId : captains.getKeys(false)) {
            ConfigurationSection row = captains.getConfigurationSection(captainId);
            if (row == null || row.contains("state.state")) {
                continue;
            }
            CaptainRecord.State migrated = parseState(row, parseUuid(row.getString("identity.spawnEntityUuid")));
            row.set("state.state", migrated.state().name());
            row.set("state.cooldownUntilEpochMs", migrated.cooldownUntilEpochMs());
            row.set("state.lastSeenEpochMs", migrated.lastSeenEpochMs());
            row.set("state.runtimeEntityUuid", stringify(migrated.runtimeEntityUuid()));
        }
    }

    private void migrateV1ToV2(YamlConfiguration config) {
        ConfigurationSection captains = config.getConfigurationSection("captains");
        if (captains == null) {
            return;
        }

        CaptainRecord.Political defaultPolitical = defaultPolitical();
        CaptainRecord.Social defaultSocial = defaultSocial();
        CaptainRecord.Memory defaultMemory = defaultMemory();
        CaptainRecord.Persona defaultPersona = defaultPersona();

        for (String captainId : captains.getKeys(false)) {
            ConfigurationSection row = captains.getConfigurationSection(captainId);
            if (row == null) {
                continue;
            }

            if (!row.contains("political.rank")) {
                row.set("political.rank", defaultPolitical.rank());
            }
            if (!row.contains("political.region")) {
                row.set("political.region", defaultPolitical.region());
            }
            if (!row.contains("political.seatId")) {
                row.set("political.seatId", defaultPolitical.seatId());
            }
            if (!row.contains("political.promotionScore")) {
                row.set("political.promotionScore", defaultPolitical.promotionScore());
            }
            if (!row.contains("political.influence")) {
                row.set("political.influence", defaultPolitical.influence());
            }

            if (!row.contains("social.loyalty")) {
                row.set("social.loyalty", defaultSocial.loyalty());
            }
            if (!row.contains("social.fear")) {
                row.set("social.fear", defaultSocial.fear());
            }
            if (!row.contains("social.ambition")) {
                row.set("social.ambition", defaultSocial.ambition());
            }
            if (!row.contains("social.confidence")) {
                row.set("social.confidence", defaultSocial.confidence());
            }

            if (!row.contains("relationships.allies")) {
                row.set("relationships.allies", List.of());
            }
            if (!row.contains("relationships.rivals")) {
                row.set("relationships.rivals", List.of());
            }
            if (!row.contains("relationships.bodyguardOf")) {
                row.set("relationships.bodyguardOf", null);
            }
            if (!row.contains("relationships.bloodBrotherOf")) {
                row.set("relationships.bloodBrotherOf", null);
            }

            if (!row.contains("memory.lastDefeatCause")) {
                row.set("memory.lastDefeatCause", defaultMemory.lastDefeatCause());
            }
            if (!row.contains("memory.scars")) {
                row.set("memory.scars", List.of());
            }
            if (!row.contains("memory.humiliations")) {
                row.set("memory.humiliations", List.of());
            }
            if (!row.contains("memory.notablePlayers")) {
                row.set("memory.notablePlayers", List.of());
            }
            if (!row.contains("memory.callbackLinesSeed")) {
                row.set("memory.callbackLinesSeed", defaultMemory.callbackLinesSeed());
            }

            if (!row.contains("persona.archetype")) {
                row.set("persona.archetype", defaultPersona.archetype());
            }
            if (!row.contains("persona.temperament")) {
                row.set("persona.temperament", defaultPersona.temperament());
            }
            if (!row.contains("persona.quirkTags")) {
                row.set("persona.quirkTags", List.of());
            }
            if (!row.contains("persona.voicePackId")) {
                row.set("persona.voicePackId", defaultPersona.voicePackId());
            }
        }
    }

    private Map<String, Object> readExtras(ConfigurationSection row) {
        Map<String, Object> extras = new HashMap<>();
        Set<String> knownPaths = knownPaths();
        for (Map.Entry<String, Object> entry : row.getValues(true).entrySet()) {
            if (isKnownPath(entry.getKey(), knownPaths) || entry.getValue() instanceof ConfigurationSection) {
                continue;
            }
            extras.put(entry.getKey(), entry.getValue());
        }
        return extras;
    }

    private boolean isKnownPath(String path, Set<String> knownPaths) {
        if (knownPaths.contains(path)) {
            return true;
        }
        for (String knownPath : knownPaths) {
            if (path.startsWith(knownPath + ".")) {
                return true;
            }
        }
        return false;
    }

    private Set<String> knownPaths() {
        Set<String> known = new HashSet<>();
        known.add("identity.nemesisOf");
        known.add("identity.createdAtEpochMillis");
        known.add("origin.world");
        known.add("origin.chunkX");
        known.add("origin.chunkZ");
        known.add("origin.spawnX");
        known.add("origin.spawnY");
        known.add("origin.spawnZ");
        known.add("origin.biome");
        known.add("victims.playerVictims");
        known.add("victims.totalVictimCount");
        known.add("victims.lastVictimAtEpochMillis");
        known.add("nemesisScores.threat");
        known.add("nemesisScores.rivalry");
        known.add("nemesisScores.brutality");
        known.add("nemesisScores.cunning");
        known.add("progression.level");
        known.add("progression.experience");
        known.add("progression.tier");
        known.add("naming.displayName");
        known.add("naming.epithet");
        known.add("naming.title");
        known.add("naming.aliasSeed");
        known.add("traits.traits");
        known.add("traits.weaknesses");
        known.add("traits.immunities");
        known.add("minionPack.packType");
        known.add("minionPack.minionCount");
        known.add("minionPack.minionArchetypes");
        known.add("minionPack.reinforcementChance");
        known.add("state.state");
        known.add("state.cooldownUntilEpochMs");
        known.add("state.lastSeenEpochMs");
        known.add("state.runtimeEntityUuid");
        known.add("telemetry.lastSeenAtEpochMillis");
        known.add("telemetry.lastUpdatedAtEpochMillis");
        known.add("telemetry.encounters");
        known.add("telemetry.counters");
        known.add("political.rank");
        known.add("political.region");
        known.add("political.seatId");
        known.add("political.promotionScore");
        known.add("political.influence");
        known.add("social.loyalty");
        known.add("social.fear");
        known.add("social.ambition");
        known.add("social.confidence");
        known.add("relationships.allies");
        known.add("relationships.rivals");
        known.add("relationships.bodyguardOf");
        known.add("relationships.bloodBrotherOf");
        known.add("memory.lastDefeatCause");
        known.add("memory.scars");
        known.add("memory.humiliations");
        known.add("memory.notablePlayers");
        known.add("memory.callbackLinesSeed");
        known.add("persona.archetype");
        known.add("persona.temperament");
        known.add("persona.quirkTags");
        known.add("persona.voicePackId");
        return known;
    }
}
