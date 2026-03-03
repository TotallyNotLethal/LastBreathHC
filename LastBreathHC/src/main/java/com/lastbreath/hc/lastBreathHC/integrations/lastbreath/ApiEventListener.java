package com.lastbreath.hc.lastBreathHC.integrations.lastbreath;

import com.lastbreath.hc.lastBreathHC.cosmetics.BossAura;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossKillMessage;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossPrefix;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ApiEventListener implements Listener {
    private final ApiClient apiClient;

    public ApiEventListener(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        apiClient.sendJoin(player.getUniqueId(), player.getName());
        apiClient.sendBulkStats(List.of(toPayload(player.getUniqueId(), StatsManager.get(player.getUniqueId()))));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        apiClient.sendBulkStats(List.of(toPayload(player.getUniqueId(), StatsManager.get(player.getUniqueId()))));
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

    public void sendBulkStatsStartup() {
        Map<UUID, PlayerStats> snapshot = StatsManager.getAllStatsSnapshot();
        Set<UUID> knownPlayerIds = new HashSet<>(snapshot.keySet());
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            knownPlayerIds.add(offlinePlayer.getUniqueId());
        }

        List<ApiClient.PlayerStatsPayload> payload = knownPlayerIds.stream()
                .sorted()
                .map(uuid -> toPayload(uuid, snapshot.getOrDefault(uuid, StatsManager.get(uuid))))
                .toList();
        apiClient.sendBulkStats(payload);
    }

    public void sendStatsFor(Player player) {
        long survivalMinutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L;
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        apiClient.sendStats(player.getUniqueId(), survivalMinutes, stats.playerKills);
    }

    private ApiClient.PlayerStatsPayload toPayload(UUID uuid, PlayerStats stats) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String username = offlinePlayer.getName();
        if (username == null || username.isBlank()) {
            Player online = Bukkit.getPlayer(uuid);
            username = online != null ? online.getName() : uuid.toString();
        }

        boolean isBanned = offlinePlayer.isBanned();

        return new ApiClient.PlayerStatsPayload(
                uuid,
                username,
                stats.nickname,
                stats.timeAlive,
                stats.deaths,
                stats.revives,
                stats.mobsKilled,
                stats.asteroidLoots,
                stats.cropsHarvested,
                stats.blocksMined,
                stats.blocksPlaced,
                stats.fishCaught,
                stats.playerKills,
                stats.rareOresMined,
                stats.worldScalerEnabled,
                mapNames(stats.unlockedTitles),
                stats.equippedTitle == null ? null : stats.equippedTitle.name(),
                mapNames(stats.unlockedPrefixes),
                stats.equippedPrefix == null ? null : stats.equippedPrefix.name(),
                mapNames(stats.unlockedAuras),
                stats.equippedAura == null ? null : stats.equippedAura.name(),
                mapNames(stats.unlockedKillMessages),
                stats.equippedKillMessage == null ? null : stats.equippedKillMessage.name(),
                !isBanned,
                isBanned
        );
    }

    private List<String> mapNames(Iterable<?> values) {
        List<String> names = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Title title) {
                names.add(title.name());
            } else if (value instanceof BossPrefix prefix) {
                names.add(prefix.name());
            } else if (value instanceof BossAura aura) {
                names.add(aura.name());
            } else if (value instanceof BossKillMessage killMessage) {
                names.add(killMessage.name());
            }
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }
}
