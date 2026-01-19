package com.lastbreath.hc.lastBreathHC.bounty;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyManager {

    private static final Map<UUID, BountyRecord> BOUNTIES = new HashMap<>();
    public static final long IN_GAME_HOUR_TICKS = 1000L;
    public static final long BOUNTY_EXPIRATION_TICKS = IN_GAME_HOUR_TICKS * 8L;
    private static final String FILE_NAME = "bounties.yml";

    private BountyManager() {
    }

    public static void load() {
        File file = getFile();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("bounties");
        if (section == null) {
            return;
        }

        BOUNTIES.clear();
        for (String key : section.getKeys(false)) {
            UUID uuid = parseUuid(key);
            if (uuid == null) {
                continue;
            }

            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }

            BountyRecord record = new BountyRecord(uuid);
            record.targetName = entry.getString("targetName");
            record.createdAt = readInstant(entry, "createdAt");
            record.accumulatedOnlineSeconds = entry.getLong("accumulatedOnlineSeconds");
            record.accumulatedOnlineTicks = entry.getLong("accumulatedOnlineTicks");
            record.lastLogoutInstant = readInstant(entry, "lastLogoutInstant");
            record.rewardTier = entry.getInt("rewardTier");
            record.rewardValue = entry.getDouble("rewardValue");
            if (record.createdAt == null) {
                record.createdAt = Instant.now();
            }
            BOUNTIES.put(uuid, record);
        }
    }

    public static void save() {
        File file = getFile();
        ensureDirectory(file.getParentFile());

        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, BountyRecord> entry : BOUNTIES.entrySet()) {
            String base = "bounties." + entry.getKey();
            BountyRecord record = entry.getValue();
            config.set(base + ".targetName", record.targetName);
            config.set(base + ".createdAt", toEpochMillis(record.createdAt));
            config.set(base + ".accumulatedOnlineSeconds", record.accumulatedOnlineSeconds);
            config.set(base + ".accumulatedOnlineTicks", record.accumulatedOnlineTicks);
            config.set(base + ".lastLogoutInstant", toEpochMillis(record.lastLogoutInstant));
            config.set(base + ".rewardTier", record.rewardTier);
            config.set(base + ".rewardValue", record.rewardValue);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            LastBreathHC.getInstance().getLogger().warning("Unable to save bounties: " + e.getMessage());
        }
    }

    public static BountyRecord createBounty(UUID targetUuid) {
        return BOUNTIES.computeIfAbsent(targetUuid, uuid -> {
            BountyRecord record = new BountyRecord(uuid);
            record.targetName = resolveName(uuid);
            record.accumulatedOnlineSeconds = 0L;
            record.accumulatedOnlineTicks = 0L;
            record.rewardTier = 1;
            record.rewardValue = calculateRewardValue(record.rewardTier);
            save();
            return record;
        });
    }

    public static boolean removeBounty(UUID targetUuid, String reason) {
        BountyRecord removed = BOUNTIES.remove(targetUuid);
        if (removed == null) {
            return false;
        }

        if (reason != null && !reason.isBlank()) {
            LastBreathHC.getInstance().getLogger().info(
                    "Removed bounty for " + targetUuid + ": " + reason
            );
        }
        save();
        return true;
    }

    public static Map<UUID, BountyRecord> getBounties() {
        return Collections.unmodifiableMap(BOUNTIES);
    }

    public static BountyRecord getBounty(UUID targetUuid) {
        return BOUNTIES.get(targetUuid);
    }

    public static void updateReward(UUID targetUuid) {
        BountyRecord record = BOUNTIES.get(targetUuid);
        if (record == null) {
            return;
        }

        record.rewardTier = calculateTier(record.accumulatedOnlineSeconds, record.accumulatedOnlineTicks);
        record.rewardValue = calculateRewardValue(record.rewardTier);
    }

    public static void incrementOnlineTimeForOnlinePlayers(long ticks, long seconds) {
        if (ticks <= 0L && seconds <= 0L) {
            return;
        }

        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, BountyRecord> entry : BOUNTIES.entrySet()) {
            UUID uuid = entry.getKey();
            org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }

            BountyRecord record = entry.getValue();
            record.accumulatedOnlineTicks += ticks;
            record.accumulatedOnlineSeconds += seconds;
            updateReward(uuid);

            if (record.accumulatedOnlineTicks >= BOUNTY_EXPIRATION_TICKS) {
                expired.add(uuid);
            }
        }

        for (UUID uuid : expired) {
            removeBounty(uuid, "Expired after 8 in-game hours online.");
        }
    }

    public static int purgeExpiredLogouts(Instant cutoff) {
        if (cutoff == null) {
            return 0;
        }

        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, BountyRecord> entry : BOUNTIES.entrySet()) {
            Instant lastLogout = entry.getValue().lastLogoutInstant;
            if (lastLogout != null && lastLogout.isBefore(cutoff)) {
                expired.add(entry.getKey());
            }
        }

        for (UUID uuid : expired) {
            removeBounty(uuid, "Removed after 30 days of inactivity.");
        }
        return expired.size();
    }

    private static String resolveName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString() : name;
    }

    private static int calculateTier(long seconds, long ticks) {
        long totalSeconds = seconds + (ticks / 20L);
        int tier = (int) (totalSeconds / 3600L) + 1;
        return Math.max(tier, 1);
    }

    private static double calculateRewardValue(int tier) {
        double base = 100.0;
        double increment = 50.0;
        return base + (Math.max(1, tier) - 1) * increment;
    }

    private static Instant readInstant(ConfigurationSection section, String path) {
        if (!section.contains(path)) {
            return null;
        }
        long millis = section.getLong(path);
        return millis > 0 ? Instant.ofEpochMilli(millis) : null;
    }

    private static long toEpochMillis(Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }

    private static UUID parseUuid(String key) {
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static File getFile() {
        return new File(LastBreathHC.getInstance().getDataFolder(), FILE_NAME);
    }

    private static void ensureDirectory(File dir) {
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            LastBreathHC.getInstance().getLogger().warning(
                    "Unable to create bounty data directory: " + dir.getAbsolutePath()
            );
        }
    }
}
