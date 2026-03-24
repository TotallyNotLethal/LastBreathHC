package com.lastbreath.hc.lastBreathHC.revive;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ReviveStateManager {

    private static final String FILE_NAME = "revive_state.yml";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_TELEPORT_ON_DEATH_DISABLED = "teleportOnDeathDisabled";
    private static final Set<UUID> pendingRevives = new HashSet<>();
    private static final Map<UUID, Boolean> teleportOnDeathOverrides = new ConcurrentHashMap<>();
    private static File dataFile;

    private ReviveStateManager() {
    }

    public static void initialize(LastBreathHC plugin) {
        dataFile = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public static boolean isRevivePending(UUID uuid) {
        return pendingRevives.contains(uuid);
    }

    public static void markRevivePending(UUID uuid) {
        if (pendingRevives.add(uuid)) {
            save();
        }
    }

    public static void clearRevivePending(UUID uuid) {
        if (pendingRevives.remove(uuid)) {
            save();
        }
    }

    public static boolean isTeleportOnDeathEnabled(UUID uuid) {
        return teleportOnDeathOverrides.getOrDefault(uuid, Boolean.TRUE);
    }

    public static void setTeleportOnDeathEnabled(UUID uuid, boolean enabled) {
        if (enabled) {
            if (teleportOnDeathOverrides.remove(uuid) != null) {
                save();
            }
            return;
        }

        Boolean previous = teleportOnDeathOverrides.put(uuid, Boolean.FALSE);
        if (previous == null || previous) {
            save();
        }
    }

    public static boolean toggleTeleportOnDeath(UUID uuid) {
        boolean enabled = !isTeleportOnDeathEnabled(uuid);
        setTeleportOnDeathEnabled(uuid, enabled);
        return enabled;
    }

    public static void save() {
        if (dataFile == null) {
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        List<String> pending = pendingRevives.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        config.set(KEY_PENDING, pending);
        List<String> teleportDisabled = teleportOnDeathOverrides.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(entry -> entry.getKey().toString())
                .collect(Collectors.toList());
        config.set(KEY_TELEPORT_ON_DEATH_DISABLED, teleportDisabled);
        try {
            config.save(dataFile);
        } catch (IOException e) {
            LastBreathHC.getInstance().getLogger().warning(
                    "Unable to save revive state: " + e.getMessage()
            );
        }
    }

    private static void load() {
        pendingRevives.clear();
        teleportOnDeathOverrides.clear();
        if (dataFile == null || !dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String entry : config.getStringList(KEY_PENDING)) {
            try {
                pendingRevives.add(UUID.fromString(entry));
            } catch (IllegalArgumentException ignored) {
                LastBreathHC.getInstance().getLogger().warning(
                        "Invalid revive state entry: " + entry
                );
            }
        }

        for (String entry : config.getStringList(KEY_TELEPORT_ON_DEATH_DISABLED)) {
            try {
                teleportOnDeathOverrides.put(UUID.fromString(entry), Boolean.FALSE);
            } catch (IllegalArgumentException ignored) {
                LastBreathHC.getInstance().getLogger().warning(
                        "Invalid revive teleport setting entry: " + entry
                );
            }
        }
    }
}
