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
    private static final int CURRENT_SCHEMA_VERSION = 1;

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

        extrasByCaptainId.put(captainUuid, readExtras(row));

        return new CaptainRecord(identity, origin, victims, nemesisScores, progression, naming, traits, minionPack, state, telemetry);
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

    private Map<String, Object> readExtras(ConfigurationSection row) {
        Map<String, Object> extras = new HashMap<>();
        Set<String> knownPaths = knownPaths();
        for (Map.Entry<String, Object> entry : row.getValues(true).entrySet()) {
            if (knownPaths.contains(entry.getKey()) || entry.getValue() instanceof ConfigurationSection) {
                continue;
            }
            extras.put(entry.getKey(), entry.getValue());
        }
        return extras;
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
        return known;
    }
}
