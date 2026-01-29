package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DeathMarkerManager {

    private final LastBreathHC plugin;
    private final TeamManager teamManager;
    private final int durationTicks;
    private final File dataFile;
    private final Map<UUID, ActiveMarker> activeMarkers = new HashMap<>();

    public DeathMarkerManager(LastBreathHC plugin, TeamManager teamManager, int durationSeconds) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.durationTicks = Math.max(1, durationSeconds) * 20;
        this.dataFile = new File(plugin.getDataFolder(), "death-markers.yml");
        loadMarkers();
    }

    public void spawnMarker(Player victim, Location location) {
        Optional<Team> teamOptional = teamManager.getTeam(victim);
        if (teamOptional.isEmpty()) {
            return;
        }
        Set<Player> teammates = teamManager.getOnlineTeamMembers(teamOptional.get());
        if (teammates.isEmpty()) {
            return;
        }

        MarkerData data = new MarkerData(teamOptional.get().getName(), victim.getName(), location, Instant.now().plusSeconds(durationTicks / 20));
        spawnMarkerFromData(data, durationTicks);
    }

    public void shutdown() {
        saveMarkers();
        Set<UUID> ids = new HashSet<>(activeMarkers.keySet());
        for (UUID markerId : ids) {
            removeMarker(markerId, false);
        }
    }

    private void removeMarker(UUID markerId) {
        removeMarker(markerId, true);
    }

    private void removeMarker(UUID markerId, boolean persist) {
        ActiveMarker marker = activeMarkers.remove(markerId);
        if (marker == null) {
            return;
        }
        marker.tickTask.cancel();
        marker.expiryTask.cancel();
        marker.bossBar.removeAll();
        if (persist) {
            saveMarkers();
        }
    }

    private void spawnMarkerFromData(MarkerData data, long remainingTicks) {
        Location markerLocation = data.location().clone().add(0.0, 1.25, 0.0);
        BossBar bossBar = Bukkit.createBossBar(
                "§cDeath marker: " + data.victimName(),
                BarColor.RED,
                BarStyle.SOLID
        );
        bossBar.setVisible(true);

        BukkitTask tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                Optional<Team> teamOptional = teamManager.getTeamByName(data.teamName());
                if (teamOptional.isEmpty()) {
                    return;
                }
                Set<Player> recipients = teamManager.getOnlineTeamMembers(teamOptional.get());
                if (recipients.isEmpty()) {
                    return;
                }
                Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.4f);
                for (Player teammate : recipients) {
                    if (teammate == null || !teammate.isOnline()) {
                        if (teammate != null) {
                            bossBar.removePlayer(teammate);
                        }
                        continue;
                    }
                    if (teammate.getName().equalsIgnoreCase(data.victimName())) {
                        continue;
                    }
                    bossBar.addPlayer(teammate);
                    teammate.spawnParticle(
                            Particle.END_ROD,
                            markerLocation,
                            8,
                            0.45,
                            0.45,
                            0.45,
                            0.0,
                            dust
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        UUID markerId = UUID.randomUUID();
        BukkitTask expiryTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeMarker(markerId);
            }
        }.runTaskLater(plugin, Math.max(1L, remainingTicks));

        activeMarkers.put(markerId, new ActiveMarker(data, bossBar, tickTask, expiryTask));
        saveMarkers();
    }

    private void loadMarkers() {
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = config.getConfigurationSection("markers");
        if (root == null) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String teamName = section.getString("team");
            String victimName = section.getString("victim");
            String worldName = section.getString("world");
            if (teamName == null || victimName == null || worldName == null) {
                continue;
            }
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            long expiresAt = section.getLong("expiresAt");
            if (expiresAt <= now) {
                continue;
            }
            long remainingTicks = Math.max(1L, (expiresAt - now) / 50L);
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            Location location = new Location(world, x, y, z);
            MarkerData data = new MarkerData(teamName, victimName, location, Instant.ofEpochMilli(expiresAt));
            spawnMarkerFromData(data, remainingTicks);
        }
    }

    private void saveMarkers() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, ActiveMarker> entry : activeMarkers.entrySet()) {
            String path = "markers." + entry.getKey();
            MarkerData data = entry.getValue().data();
            config.set(path + ".team", data.teamName());
            config.set(path + ".victim", data.victimName());
            config.set(path + ".world", data.location().getWorld() != null ? data.location().getWorld().getName() : null);
            config.set(path + ".x", data.location().getX());
            config.set(path + ".y", data.location().getY());
            config.set(path + ".z", data.location().getZ());
            config.set(path + ".expiresAt", data.expiresAt().toEpochMilli());
        }
        dataFile.getParentFile().mkdirs();
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save death markers.");
        }
    }

    private record MarkerData(String teamName, String victimName, Location location, Instant expiresAt) {
    }

    private record ActiveMarker(MarkerData data, BossBar bossBar, BukkitTask tickTask, BukkitTask expiryTask) {
    }
}
