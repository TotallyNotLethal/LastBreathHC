package com.lastbreath.hc.lastBreathHC.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class CosmeticAuraService {

    private BukkitTask task;

    public void start(Plugin plugin) {
        stop();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    BossAura aura = CosmeticManager.getEquippedAura(player);
                    if (aura == null) {
                        continue;
                    }
                    Location center = player.getLocation().clone().add(0, 1.0, 0);
                    Particle particle = aura.particle();
                    player.getWorld().spawnParticle(particle, center, 6, 0.3, 0.4, 0.3, 0.02);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
