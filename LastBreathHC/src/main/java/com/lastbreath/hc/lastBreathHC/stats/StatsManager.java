package com.lastbreath.hc.lastBreathHC.stats;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossAura;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossKillMessage;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossPrefix;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class StatsManager {

    private static final Map<UUID, PlayerStats> stats = new HashMap<>();
    private static final String FILE_NAME = "player-stats.yml";

    public static PlayerStats get(UUID uuid) {
        return stats.computeIfAbsent(uuid, StatsManager::loadFromDisk);
    }

    public static PlayerStats load(UUID uuid) {
        PlayerStats loaded = loadFromDisk(uuid);
        stats.put(uuid, loaded);
        return loaded;
    }

    public static void save(UUID uuid) {
        PlayerStats playerStats = stats.get(uuid);
        if (playerStats == null) {
            return;
        }
        saveStats(playerStats, true);
    }

    public static void saveAll() {
        File file = getFile();
        ensureDirectory(file.getParentFile());

        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        for (PlayerStats entry : stats.values()) {
            writeStats(config, entry);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            LastBreathHC.getInstance().getLogger().warning("Unable to save player stats: " + e.getMessage());
        }
    }

    public static StatsSummary summarize() {
        Map<UUID, Integer> deathsByPlayer = new HashMap<>();
        Map<UUID, Long> timeAliveByPlayer = new HashMap<>();
        File file = getFile();
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String base = "players";
            ConfigurationSection playersSection = config.getConfigurationSection(base);
            if (playersSection != null) {
                for (String key : playersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        deathsByPlayer.put(uuid, config.getInt(base + "." + key + ".deaths", 0));
                        timeAliveByPlayer.put(uuid, config.getLong(base + "." + key + ".timeAlive", 0L));
                    } catch (IllegalArgumentException ignored) {
                        LastBreathHC.getInstance().getLogger().warning("Invalid UUID in stats file: " + key);
                    }
                }
            }
        }

        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            deathsByPlayer.put(entry.getKey(), entry.getValue().deaths);
            timeAliveByPlayer.put(entry.getKey(), entry.getValue().timeAlive);
        }

        int totalDeaths = deathsByPlayer.values().stream().mapToInt(Integer::intValue).sum();
        long totalPlaytimeTicks = timeAliveByPlayer.values().stream().mapToLong(Long::longValue).sum();
        return new StatsSummary(deathsByPlayer.size(), totalDeaths, totalPlaytimeTicks);
    }

    private static PlayerStats loadFromDisk(UUID uuid) {
        PlayerStats playerStats = new PlayerStats(uuid);

        File file = getFile();
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String base = "players." + uuid;
            playerStats.timeAlive = config.getLong(base + ".timeAlive", 0L);
            playerStats.deaths = config.getInt(base + ".deaths", 0);
            playerStats.revives = config.getInt(base + ".revives", 0);
            playerStats.cropsHarvested = config.getInt(base + ".cropsHarvested", 0);
            playerStats.blocksMined = config.getInt(base + ".blocksMined", 0);
            playerStats.rareOresMined = config.getInt(base + ".rareOresMined", 0);
            playerStats.nickname = config.getString(base + ".nickname");

            List<String> unlocked = config.getStringList(base + ".unlockedTitles");
            Set<Title> titles = new HashSet<>();
            for (String titleName : unlocked) {
                Title title = Title.fromInput(titleName);
                if (title != null) {
                    titles.add(title);
                }
            }
            playerStats.unlockedTitles = titles;

            String equippedName = config.getString(base + ".equippedTitle");
            if (equippedName != null && !equippedName.isBlank()) {
                playerStats.equippedTitle = Title.fromInput(equippedName);
            }

            List<String> unlockedPrefixes = config.getStringList(base + ".unlockedPrefixes");
            Set<BossPrefix> prefixSet = new HashSet<>();
            for (String prefixName : unlockedPrefixes) {
                BossPrefix prefix = BossPrefix.fromInput(prefixName);
                if (prefix != null) {
                    prefixSet.add(prefix);
                }
            }
            playerStats.unlockedPrefixes = prefixSet;

            String equippedPrefixName = config.getString(base + ".equippedPrefix");
            if (equippedPrefixName != null && !equippedPrefixName.isBlank()) {
                playerStats.equippedPrefix = BossPrefix.fromInput(equippedPrefixName);
            }

            List<String> unlockedAuras = config.getStringList(base + ".unlockedAuras");
            Set<BossAura> auraSet = new HashSet<>();
            for (String auraName : unlockedAuras) {
                BossAura aura = BossAura.fromInput(auraName);
                if (aura != null) {
                    auraSet.add(aura);
                }
            }
            playerStats.unlockedAuras = auraSet;

            String equippedAuraName = config.getString(base + ".equippedAura");
            if (equippedAuraName != null && !equippedAuraName.isBlank()) {
                playerStats.equippedAura = BossAura.fromInput(equippedAuraName);
            }

            List<String> unlockedKillMessages = config.getStringList(base + ".unlockedKillMessages");
            Set<BossKillMessage> killMessageSet = new HashSet<>();
            for (String messageName : unlockedKillMessages) {
                BossKillMessage message = BossKillMessage.fromInput(messageName);
                if (message != null) {
                    killMessageSet.add(message);
                }
            }
            playerStats.unlockedKillMessages = killMessageSet;

            String equippedKillMessageName = config.getString(base + ".equippedKillMessage");
            if (equippedKillMessageName != null && !equippedKillMessageName.isBlank()) {
                playerStats.equippedKillMessage = BossKillMessage.fromInput(equippedKillMessageName);
            }
        }

        TitleManager.initialize(playerStats);
        return playerStats;
    }

    private static void saveStats(PlayerStats playerStats, boolean preserveExisting) {
        File file = getFile();
        ensureDirectory(file.getParentFile());

        YamlConfiguration config = preserveExisting && file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        writeStats(config, playerStats);

        try {
            config.save(file);
        } catch (IOException e) {
            LastBreathHC.getInstance().getLogger().warning("Unable to save player stats: " + e.getMessage());
        }
    }

    private static void writeStats(YamlConfiguration config, PlayerStats playerStats) {
        String base = "players." + playerStats.uuid;
        config.set(base + ".timeAlive", playerStats.timeAlive);
        config.set(base + ".deaths", playerStats.deaths);
        config.set(base + ".revives", playerStats.revives);
        config.set(base + ".cropsHarvested", playerStats.cropsHarvested);
        config.set(base + ".blocksMined", playerStats.blocksMined);
        config.set(base + ".rareOresMined", playerStats.rareOresMined);
        config.set(base + ".nickname", playerStats.nickname);
        config.set(base + ".unlockedTitles", playerStats.unlockedTitles.stream()
                .map(Title::name)
                .sorted()
                .collect(Collectors.toList()));
        config.set(base + ".equippedTitle", playerStats.equippedTitle != null ? playerStats.equippedTitle.name() : null);
        config.set(base + ".unlockedPrefixes", playerStats.unlockedPrefixes.stream()
                .map(BossPrefix::name)
                .sorted()
                .collect(Collectors.toList()));
        config.set(base + ".equippedPrefix", playerStats.equippedPrefix != null ? playerStats.equippedPrefix.name() : null);
        config.set(base + ".unlockedAuras", playerStats.unlockedAuras.stream()
                .map(BossAura::name)
                .sorted()
                .collect(Collectors.toList()));
        config.set(base + ".equippedAura", playerStats.equippedAura != null ? playerStats.equippedAura.name() : null);
        config.set(base + ".unlockedKillMessages", playerStats.unlockedKillMessages.stream()
                .map(BossKillMessage::name)
                .sorted()
                .collect(Collectors.toList()));
        config.set(base + ".equippedKillMessage", playerStats.equippedKillMessage != null ? playerStats.equippedKillMessage.name() : null);
    }

    private static File getFile() {
        return new File(LastBreathHC.getInstance().getDataFolder(), FILE_NAME);
    }

    private static void ensureDirectory(File directory) {
        if (directory != null && !directory.exists() && !directory.mkdirs()) {
            LastBreathHC.getInstance().getLogger().warning(
                    "Unable to create stats directory at " + directory.getAbsolutePath()
            );
        }
    }

    public record StatsSummary(int uniqueJoins, int totalDeaths, long totalPlaytimeTicks) {
    }
}
