package com.lastbreath.hc.lastBreathHC.integrations.lastbreath;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.Statistic;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class ApiEventListener implements Listener {
    private final ApiClient apiClient;

    public ApiEventListener(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        apiClient.sendJoin(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        apiClient.sendLeave(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String deathMessage = event.getDeathMessage() == null ? "" : event.getDeathMessage();
        apiClient.sendDeath(player.getUniqueId(), deathMessage);
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        Player killer = dragon.getKiller();
        apiClient.sendDragon(killer == null ? Optional.empty() : Optional.of(killer.getUniqueId()));
    }

    public void sendStatsFor(Player player) {
        long survivalMinutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L;
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        apiClient.sendStats(player.getUniqueId(), survivalMinutes, stats.playerKills);
    }
}
