package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DeathMarkerManager {

    private final LastBreathHC plugin;
    private final TeamManager teamManager;
    private final int durationTicks;
    private final Map<UUID, ActiveMarker> activeMarkers = new HashMap<>();

    public DeathMarkerManager(LastBreathHC plugin, TeamManager teamManager, int durationSeconds) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.durationTicks = Math.max(1, durationSeconds) * 20;
    }

    public void spawnMarker(Player victim, Location location) {
        Set<Player> teammates = teamManager.getOnlineTeamMembers(victim);
        if (teammates.isEmpty()) {
            return;
        }

        Set<UUID> recipientIds = new HashSet<>();
        for (Player teammate : teammates) {
            if (!teammate.getUniqueId().equals(victim.getUniqueId())) {
                recipientIds.add(teammate.getUniqueId());
            }
        }

        if (recipientIds.isEmpty()) {
            return;
        }

        Location markerLocation = location.clone().add(0.0, 1.25, 0.0);
        BossBar bossBar = Bukkit.createBossBar(
                "§cDeath marker: " + victim.getName(),
                BarColor.RED,
                BarStyle.SOLID
        );
        bossBar.setVisible(true);
        for (UUID recipientId : recipientIds) {
            Player teammate = Bukkit.getPlayer(recipientId);
            if (teammate != null) {
                bossBar.addPlayer(teammate);
            }
        }

        BukkitTask tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (recipientIds.isEmpty()) {
                    return;
                }
                Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.4f);
                for (UUID recipientId : recipientIds) {
                    Player teammate = Bukkit.getPlayer(recipientId);
                    if (teammate == null || !teammate.isOnline()) {
                        if (teammate != null) {
                            bossBar.removePlayer(teammate);
                        }
                        continue;
                    }
                    bossBar.addPlayer(teammate);
                    teammate.spawnParticle(
                            Particle.REDSTONE,
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
        }.runTaskLater(plugin, durationTicks);

        activeMarkers.put(markerId, new ActiveMarker(bossBar, tickTask, expiryTask));
    }

    public void shutdown() {
        Set<UUID> ids = new HashSet<>(activeMarkers.keySet());
        for (UUID markerId : ids) {
            removeMarker(markerId);
        }
    }

    private void removeMarker(UUID markerId) {
        ActiveMarker marker = activeMarkers.remove(markerId);
        if (marker == null) {
            return;
        }
        marker.tickTask.cancel();
        marker.expiryTask.cancel();
        marker.bossBar.removeAll();
    }

    private record ActiveMarker(BossBar bossBar, BukkitTask tickTask, BukkitTask expiryTask) {
    }
}
