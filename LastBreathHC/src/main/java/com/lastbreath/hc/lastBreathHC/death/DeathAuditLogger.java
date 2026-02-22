package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public class DeathAuditLogger {

    private static final String FILE_NAME = "death-audit-log.yml";
    private final LastBreathHC plugin;
    private final File file;
    private final YamlConfiguration config;

    public DeathAuditLogger(LastBreathHC plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void recordDeath(Player player, String deathReason, String lastMessage) {
        String path = "events." + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        config.set(path + ".playerUuid", player.getUniqueId().toString());
        config.set(path + ".playerName", player.getName());
        config.set(path + ".deathReason", deathReason);
        config.set(path + ".lastMessage", (lastMessage == null || lastMessage.isBlank()) ? "No last message recorded." : lastMessage);
        config.set(path + ".recordedAt", Instant.now().toString());
        save();
    }

    private void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for death audit log.");
            return;
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save death audit log file.");
        }
    }
}
