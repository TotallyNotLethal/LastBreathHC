package com.lastbreath.hc.lastBreathHC.daily;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

public class DailyCosmeticListener implements Listener {

    private final LastBreathHC plugin;
    private final DailyRewardManager dailyRewardManager;
    private BukkitTask auraTask;

    public DailyCosmeticListener(LastBreathHC plugin, DailyRewardManager dailyRewardManager) {
        this.plugin = plugin;
        this.dailyRewardManager = dailyRewardManager;
    }

    public void start() {
        if (auraTask != null) {
            auraTask.cancel();
        }
        auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                DailyCosmeticType cosmetic = dailyRewardManager.getEquippedCosmetic(player.getUniqueId());
                if (cosmetic == null || cosmetic.style() != DailyCosmeticType.CosmeticRenderStyle.AURA) {
                    continue;
                }
                Location center = player.getLocation().clone().add(0, 1.1, 0);
                player.getWorld().spawnParticle(cosmetic.particle(), center, 6, 0.35, 0.45, 0.35, 0.01);
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (auraTask != null) {
            auraTask.cancel();
            auraTask = null;
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().distanceSquared(event.getTo()) < 0.04) {
            return;
        }

        Player player = event.getPlayer();
        DailyCosmeticType cosmetic = dailyRewardManager.getEquippedCosmetic(player.getUniqueId());
        if (cosmetic == null || cosmetic.style() != DailyCosmeticType.CosmeticRenderStyle.TRAIL) {
            return;
        }

        Location trail = player.getLocation().clone().add(0, 0.15, 0);
        player.getWorld().spawnParticle(cosmetic.particle(), trail, 4, 0.18, 0.08, 0.18, 0.002);
    }
}
