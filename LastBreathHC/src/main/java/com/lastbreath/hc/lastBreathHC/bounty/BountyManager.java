package com.lastbreath.hc.lastBreathHC.bounty;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BountyManager {

    private static final Map<UUID, BountyRecord> BOUNTIES = new HashMap<>();
    public static final long IN_GAME_HOUR_TICKS = 1000L;
    public static final long BOUNTY_EXPIRATION_TICKS = IN_GAME_HOUR_TICKS * 8L;
    private static final long MAX_REWARD_HOURS = 7L;
    private static final int MIN_DIAMOND_REWARD = 1;
    private static final int MAX_DIAMOND_REWARD = 7;
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
            updateReward(record);
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

    public static BountyRecord claimBounty(UUID targetUuid, String reason) {
        BountyRecord removed;
        synchronized (BOUNTIES) {
            removed = BOUNTIES.remove(targetUuid);
        }
        if (removed == null) {
            return null;
        }

        if (reason != null && !reason.isBlank()) {
            LastBreathHC.getInstance().getLogger().info(
                    "Claimed bounty for " + targetUuid + ": " + reason
            );
        }
        save();
        return removed;
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

        updateReward(record);
    }

    public static ItemStack getRewardItemStack(UUID targetUuid) {
        BountyRecord record = BOUNTIES.get(targetUuid);
        if (record == null) {
            return null;
        }
        return getRewardItemStack(record);
    }

    public static ItemStack getRewardItemStack(BountyRecord record) {
        RewardDescriptor descriptor = calculateRewardDescriptor(
                record.accumulatedOnlineSeconds,
                record.accumulatedOnlineTicks
        );
        ItemStack itemStack = new ItemStack(descriptor.material, descriptor.amount);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Bounty Reward");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Reward: " + descriptor.displayName);
            lore.add(ChatColor.DARK_GRAY + "Online time: " + descriptor.formattedHours);
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
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

    private static void updateReward(BountyRecord record) {
        RewardDescriptor descriptor = calculateRewardDescriptor(
                record.accumulatedOnlineSeconds,
                record.accumulatedOnlineTicks
        );
        record.rewardTier = descriptor.tier;
        record.rewardValue = descriptor.inGameHours;
    }

    private static RewardDescriptor calculateRewardDescriptor(long seconds, long ticks) {
        long totalInGameTicks = calculateTotalInGameTicks(seconds, ticks);
        double inGameHours = totalInGameTicks / (double) IN_GAME_HOUR_TICKS;
        double cappedHours = Math.min(inGameHours, MAX_REWARD_HOURS);
        if (inGameHours >= MAX_REWARD_HOURS) {
            return new RewardDescriptor(
                    Material.NETHERITE_INGOT,
                    1,
                    MAX_DIAMOND_REWARD + 1,
                    inGameHours,
                    "1 Netherite Ingot",
                    formatHours(cappedHours)
            );
        }

        int diamondAmount = Math.min(
                MAX_DIAMOND_REWARD,
                MIN_DIAMOND_REWARD + (int) Math.floor(cappedHours)
        );
        String label = diamondAmount + (diamondAmount == 1 ? " Diamond" : " Diamonds");
        return new RewardDescriptor(
                Material.DIAMOND,
                diamondAmount,
                diamondAmount,
                inGameHours,
                label,
                formatHours(cappedHours)
        );
    }

    private static long calculateTotalSeconds(long seconds, long ticks) {
        return seconds + (ticks / 20L);
    }

    private static long calculateTotalInGameTicks(long seconds, long ticks) {
        long totalSeconds = calculateTotalSeconds(seconds, ticks);
        return totalSeconds * 20L;
    }

    private static String formatHours(double hours) {
        return String.format(Locale.US, "%.1f", hours) + "h";
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

    private static class RewardDescriptor {
        private final Material material;
        private final int amount;
        private final int tier;
        private final double inGameHours;
        private final String displayName;
        private final String formattedHours;

        private RewardDescriptor(
                Material material,
                int amount,
                int tier,
                double inGameHours,
                String displayName,
                String formattedHours
        ) {
            this.material = material;
            this.amount = amount;
            this.tier = tier;
            this.inGameHours = inGameHours;
            this.displayName = displayName;
            this.formattedHours = formattedHours;
        }
    }
}
