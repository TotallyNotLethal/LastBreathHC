package com.lastbreath.hc.lastBreathHC.fakeplayer;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public record FakePlayersSettings(
        boolean autoRespawnActiveOnStartup,
        DeathReactionsSettings deathReactions,
        BatchingSettings batching
) {

    public static FakePlayersSettings load(LastBreathHC plugin) {
        FileConfiguration config = plugin.getConfig();

        boolean autoRespawn = readBoolean(
                plugin,
                config,
                "fakePlayers.autoRespawnActiveOnStartup",
                true
        );

        boolean reactionsEnabled = readBoolean(
                plugin,
                config,
                "fakePlayers.deathReactions.enabled",
                true
        );

        int maxReactionsPerMinute = clamp(
                plugin,
                readInt(plugin, config, "fakePlayers.deathReactions.maxReactionsPerMinute", 3),
                0,
                60,
                "fakePlayers.deathReactions.maxReactionsPerMinute"
        );

        long perFakeCooldownSeconds = clamp(
                plugin,
                readLong(plugin, config, "fakePlayers.deathReactions.perFakeCooldownSeconds", 30L),
                0L,
                3600L,
                "fakePlayers.deathReactions.perFakeCooldownSeconds"
        );

        int delayMinTicks = clamp(
                plugin,
                readDelayMinTicks(plugin, config),
                0,
                20 * 30,
                "fakePlayers.deathReactions.delayMinTicks"
        );

        int delayMaxTicks = clamp(
                plugin,
                readDelayMaxTicks(plugin, config),
                0,
                20 * 60,
                "fakePlayers.deathReactions.delayMaxTicks"
        );

        if (delayMaxTicks < delayMinTicks) {
            plugin.getLogger().warning("Invalid fakePlayers.deathReactions delay range: delayMaxTicks < delayMinTicks. Using delayMaxTicks=" + delayMinTicks + ".");
            delayMaxTicks = delayMinTicks;
        }

        List<String> messages = config.getStringList("fakePlayers.deathReactions.messages");
        if (messages == null || messages.isEmpty()) {
            plugin.getLogger().warning("fakePlayers.deathReactions.messages is empty or malformed. Death reactions will be disabled until valid messages are configured.");
            messages = List.of();
        }

        int startupRespawnBatchSize = clamp(
                plugin,
                readInt(plugin, config, "fakePlayers.batching.startupRespawnBatchSize", 20),
                1,
                500,
                "fakePlayers.batching.startupRespawnBatchSize"
        );

        int startupRespawnIntervalTicks = clamp(
                plugin,
                readInt(plugin, config, "fakePlayers.batching.startupRespawnIntervalTicks", 1),
                1,
                20,
                "fakePlayers.batching.startupRespawnIntervalTicks"
        );

        int visualUpdateBatchSize = clamp(
                plugin,
                readInt(plugin, config, "fakePlayers.batching.visualUpdateBatchSize", 30),
                1,
                500,
                "fakePlayers.batching.visualUpdateBatchSize"
        );

        int visualUpdateIntervalTicks = clamp(
                plugin,
                readInt(plugin, config, "fakePlayers.batching.visualUpdateIntervalTicks", 1),
                1,
                20,
                "fakePlayers.batching.visualUpdateIntervalTicks"
        );

        return new FakePlayersSettings(
                autoRespawn,
                new DeathReactionsSettings(
                        reactionsEnabled,
                        messages,
                        delayMinTicks,
                        delayMaxTicks,
                        perFakeCooldownSeconds,
                        maxReactionsPerMinute
                ),
                new BatchingSettings(
                        startupRespawnBatchSize,
                        startupRespawnIntervalTicks,
                        visualUpdateBatchSize,
                        visualUpdateIntervalTicks
                )
        );
    }

    private static int readDelayMinTicks(LastBreathHC plugin, FileConfiguration config) {
        if (config.isInt("fakePlayers.deathReactions.delayMinTicks")) {
            return config.getInt("fakePlayers.deathReactions.delayMinTicks");
        }
        if (config.isInt("fakePlayers.deathReactions.delayTicks.min")) {
            plugin.getLogger().warning("fakePlayers.deathReactions.delayTicks.min is deprecated. Use fakePlayers.deathReactions.delayMinTicks instead.");
            return config.getInt("fakePlayers.deathReactions.delayTicks.min");
        }
        plugin.getLogger().warning("Missing fakePlayers.deathReactions.delayMinTicks. Using default 20.");
        return 20;
    }

    private static int readDelayMaxTicks(LastBreathHC plugin, FileConfiguration config) {
        if (config.isInt("fakePlayers.deathReactions.delayMaxTicks")) {
            return config.getInt("fakePlayers.deathReactions.delayMaxTicks");
        }
        if (config.isInt("fakePlayers.deathReactions.delayTicks.max")) {
            plugin.getLogger().warning("fakePlayers.deathReactions.delayTicks.max is deprecated. Use fakePlayers.deathReactions.delayMaxTicks instead.");
            return config.getInt("fakePlayers.deathReactions.delayTicks.max");
        }
        plugin.getLogger().warning("Missing fakePlayers.deathReactions.delayMaxTicks. Using default 80.");
        return 80;
    }

    private static boolean readBoolean(LastBreathHC plugin, FileConfiguration config, String path, boolean defaultValue) {
        if (!config.isSet(path)) {
            return defaultValue;
        }
        if (!config.isBoolean(path)) {
            plugin.getLogger().warning("Malformed config at " + path + ": expected a boolean. Using default " + defaultValue + ".");
            return defaultValue;
        }
        return config.getBoolean(path);
    }

    private static int readInt(LastBreathHC plugin, FileConfiguration config, String path, int defaultValue) {
        if (!config.isSet(path)) {
            return defaultValue;
        }
        if (!config.isInt(path)) {
            plugin.getLogger().warning("Malformed config at " + path + ": expected an integer. Using default " + defaultValue + ".");
            return defaultValue;
        }
        return config.getInt(path);
    }

    private static long readLong(LastBreathHC plugin, FileConfiguration config, String path, long defaultValue) {
        if (!config.isSet(path)) {
            return defaultValue;
        }
        if (!config.isLong(path) && !config.isInt(path)) {
            plugin.getLogger().warning("Malformed config at " + path + ": expected a whole number. Using default " + defaultValue + ".");
            return defaultValue;
        }
        return config.getLong(path);
    }

    private static int clamp(LastBreathHC plugin, int value, int min, int max, String path) {
        if (value < min || value > max) {
            int clamped = Math.max(min, Math.min(max, value));
            plugin.getLogger().warning("Invalid config at " + path + "=" + value + ". Clamped to " + clamped + " (allowed " + min + ".." + max + ").");
            return clamped;
        }
        return value;
    }

    private static long clamp(LastBreathHC plugin, long value, long min, long max, String path) {
        if (value < min || value > max) {
            long clamped = Math.max(min, Math.min(max, value));
            plugin.getLogger().warning("Invalid config at " + path + "=" + value + ". Clamped to " + clamped + " (allowed " + min + ".." + max + ").");
            return clamped;
        }
        return value;
    }

    public record DeathReactionsSettings(
            boolean enabled,
            List<String> messages,
            int delayMinTicks,
            int delayMaxTicks,
            long perFakeCooldownSeconds,
            int maxReactionsPerMinute
    ) {
    }

    public record BatchingSettings(
            int startupRespawnBatchSize,
            int startupRespawnIntervalTicks,
            int visualUpdateBatchSize,
            int visualUpdateIntervalTicks
    ) {
    }
}
