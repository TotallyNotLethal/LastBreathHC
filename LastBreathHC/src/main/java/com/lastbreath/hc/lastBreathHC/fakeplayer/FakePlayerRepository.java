package com.lastbreath.hc.lastBreathHC.fakeplayer;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakePlayerRepository {
    private static final int CURRENT_SCHEMA_VERSION = 4;

    private final LastBreathHC plugin;
    private final File file;

    public FakePlayerRepository(LastBreathHC plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public Map<UUID, FakePlayerRecord> load() {
        Map<UUID, FakePlayerRecord> records = new HashMap<>();
        loadInto(records, new HashMap<>());
        return records;
    }

    public void loadInto(Map<UUID, FakePlayerRecord> records, Map<String, SkinCacheEntry> skinCache) {
        records.clear();
        skinCache.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int schemaVersion = config.getInt("schemaVersion", 1);
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            plugin.getLogger().warning("Unsupported fake player schema version " + schemaVersion + ", skipping load.");
            return;
        }

        boolean normalizeUuidKeys = schemaVersion < CURRENT_SCHEMA_VERSION;
        Map<String, FakePlayerRecord> byCanonicalName = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                UUID storedUuid;
                try {
                    storedUuid = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                ConfigurationSection row = section.getConfigurationSection(key);
                if (row == null) {
                    continue;
                }

                FakePlayerRecord record = new FakePlayerRecord();
                record.setName(canonicalName(row.getString("name", "unknown")));
                UUID expectedUuid = FakePlayerService.deterministicUuid(record.getName());
                record.setUuid(normalizeUuidKeys ? expectedUuid : storedUuid);
                record.setSkinOwner(row.getString("skinOwner"));
                record.setTextures(row.getString("textures"));
                record.setSignature(row.getString("signature"));
                record.setActive(row.getBoolean("active", true));
                record.setMuted(row.getBoolean("muted", false));
                record.setCreatedAt(fromEpochMillis(row, "createdAt"));
                record.setLastSeenAt(fromEpochMillis(row, "lastSeenAt"));
                record.setLastChatAt(fromEpochMillis(row, "lastChatAt"));
                record.setLastReactionAt(fromEpochMillis(row, "lastReactionAt"));
                record.setChatCount(row.getLong("chatCount", 0L));
                record.setReactionCount(row.getLong("reactionCount", 0L));
                record.setTabTitleKey(row.getString("tabTitleKey"));
                record.setTabPingMillis(row.getInt("tabPingMillis", 0));
                if (record.getCreatedAt() == null) {
                    record.setCreatedAt(Instant.now());
                }

                if (normalizeUuidKeys && !storedUuid.equals(expectedUuid)) {
                    plugin.getLogger().info("Normalizing fake-player UUID for " + record.getName() + ": " + storedUuid + " -> " + expectedUuid);
                }

                mergeNormalizedRecord(records, byCanonicalName, record);
            }
        }

        ConfigurationSection cacheSection = config.getConfigurationSection("skinCache");
        if (cacheSection == null) {
            return;
        }

        for (String ownerKey : cacheSection.getKeys(false)) {
            ConfigurationSection row = cacheSection.getConfigurationSection(ownerKey);
            if (row == null) {
                continue;
            }
            SkinCacheEntry entry = new SkinCacheEntry();
            entry.setOwner(ownerKey);
            entry.setTextures(row.getString("textures"));
            entry.setSignature(row.getString("signature"));
            entry.setFetchedAt(fromEpochMillis(row, "fetchedAt"));
            entry.setExpiresAt(fromEpochMillis(row, "expiresAt"));
            skinCache.put(ownerKey, entry);
        }
    }

    public void save(Collection<FakePlayerRecord> records) {
        save(records, Map.of());
    }

    public void save(Collection<FakePlayerRecord> records, Map<String, SkinCacheEntry> skinCache) {
        ensureParentDirectory();

        YamlConfiguration config = new YamlConfiguration();
        config.set("schemaVersion", CURRENT_SCHEMA_VERSION);
        for (FakePlayerRecord record : records) {
            String canonicalName = canonicalName(record.getName());
            UUID normalizedUuid = FakePlayerService.deterministicUuid(canonicalName);
            record.setName(canonicalName);
            record.setUuid(normalizedUuid);

            String base = "players." + normalizedUuid;
            config.set(base + ".name", canonicalName);
            config.set(base + ".skinOwner", record.getSkinOwner());
            config.set(base + ".textures", record.getTextures());
            config.set(base + ".signature", record.getSignature());
            config.set(base + ".active", record.isActive());
            config.set(base + ".muted", record.isMuted());
            config.set(base + ".createdAt", toEpochMillis(record.getCreatedAt()));
            config.set(base + ".lastSeenAt", toEpochMillis(record.getLastSeenAt()));
            config.set(base + ".lastChatAt", toEpochMillis(record.getLastChatAt()));
            config.set(base + ".lastReactionAt", toEpochMillis(record.getLastReactionAt()));
            config.set(base + ".chatCount", record.getChatCount());
            config.set(base + ".reactionCount", record.getReactionCount());
            config.set(base + ".tabTitleKey", record.getTabTitleKey());
            config.set(base + ".tabPingMillis", record.getTabPingMillis());
        }

        for (Map.Entry<String, SkinCacheEntry> cacheEntry : skinCache.entrySet()) {
            String owner = cacheEntry.getKey();
            SkinCacheEntry entry = cacheEntry.getValue();
            String base = "skinCache." + owner;
            config.set(base + ".textures", entry.getTextures());
            config.set(base + ".signature", entry.getSignature());
            config.set(base + ".fetchedAt", toEpochMillis(entry.getFetchedAt()));
            config.set(base + ".expiresAt", toEpochMillis(entry.getExpiresAt()));
        }

        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(tempFile);
            moveAtomic(tempFile, file);
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save fake players: " + e.getMessage());
        } finally {
            if (tempFile.exists() && !tempFile.equals(file)) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    private void mergeNormalizedRecord(Map<UUID, FakePlayerRecord> records,
                                       Map<String, FakePlayerRecord> byCanonicalName,
                                       FakePlayerRecord candidate) {
        if (candidate == null || candidate.getUuid() == null || candidate.getName() == null) {
            return;
        }

        String canonicalName = candidate.getName().toLowerCase();
        FakePlayerRecord existingByName = byCanonicalName.get(canonicalName);
        if (existingByName != null && existingByName != candidate) {
            FakePlayerRecord preferred = preferNewest(existingByName, candidate);
            FakePlayerRecord discarded = preferred == existingByName ? candidate : existingByName;
            records.remove(discarded.getUuid());
            records.put(preferred.getUuid(), preferred);
            byCanonicalName.put(canonicalName, preferred);
            return;
        }

        FakePlayerRecord existingByUuid = records.get(candidate.getUuid());
        if (existingByUuid != null && existingByUuid != candidate) {
            FakePlayerRecord preferred = preferNewest(existingByUuid, candidate);
            records.put(preferred.getUuid(), preferred);
            byCanonicalName.put(preferred.getName().toLowerCase(), preferred);
            return;
        }

        records.put(candidate.getUuid(), candidate);
        byCanonicalName.put(canonicalName, candidate);
    }

    private FakePlayerRecord preferNewest(FakePlayerRecord first, FakePlayerRecord second) {
        Instant firstSeenAt = first.getLastSeenAt() == null ? Instant.EPOCH : first.getLastSeenAt();
        Instant secondSeenAt = second.getLastSeenAt() == null ? Instant.EPOCH : second.getLastSeenAt();
        if (secondSeenAt.isAfter(firstSeenAt)) {
            return second;
        }
        if (firstSeenAt.isAfter(secondSeenAt)) {
            return first;
        }

        Instant firstCreatedAt = first.getCreatedAt() == null ? Instant.EPOCH : first.getCreatedAt();
        Instant secondCreatedAt = second.getCreatedAt() == null ? Instant.EPOCH : second.getCreatedAt();
        return secondCreatedAt.isAfter(firstCreatedAt) ? second : first;
    }

    private String canonicalName(String name) {
        String normalized = name == null ? "unknown" : name.trim();
        return normalized.isEmpty() ? "unknown" : normalized;
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

    private static Instant fromEpochMillis(ConfigurationSection row, String key) {
        long value = row.getLong(key, -1L);
        return value <= 0L ? null : Instant.ofEpochMilli(value);
    }

    private static Long toEpochMillis(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }
}
