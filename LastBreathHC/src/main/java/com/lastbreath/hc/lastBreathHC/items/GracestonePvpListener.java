package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GracestonePvpListener implements Listener {

    private static final long PVP_DISABLE_DURATION_TICKS = Gracestone.PVP_GRACESTONE_DISABLE_DURATION_MS / 50L;

    private final LastBreathHC plugin;
    private final Map<UUID, BukkitTask> restoreTasks = new HashMap<>();

    public GracestonePvpListener(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerVsPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        tagPlayerInPvp(attacker);
        tagPlayerInPvp(victim);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BukkitTask task = restoreTasks.remove(event.getPlayer().getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void tagPlayerInPvp(Player player) {
        Gracestone.markPvpCombat(player);
        Gracestone.refreshDisplay(player);

        BukkitTask currentTask = restoreTasks.remove(player.getUniqueId());
        if (currentTask != null) {
            currentTask.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            restoreTasks.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            Gracestone.refreshDisplay(player);
        }, PVP_DISABLE_DURATION_TICKS);

        restoreTasks.put(player.getUniqueId(), task);
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
