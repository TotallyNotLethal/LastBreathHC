package com.lastbreath.hc.lastBreathHC.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
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
                    spawnAuraParticle(player, center, particle);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void spawnAuraParticle(Player player, Location center, Particle particle) {
        Class<?> dataType = particle.getDataType();
        if (Void.class.equals(dataType)) {
            player.getWorld().spawnParticle(particle, center, 6, 0.3, 0.4, 0.3, 0.02);
            return;
        }

        if (Float.class.equals(dataType)) {
            player.getWorld().spawnParticle(particle, center, 6, 0.3, 0.4, 0.3, 0.02, 1.0f);
            return;
        }

        if (Integer.class.equals(dataType)) {
            player.getWorld().spawnParticle(particle, center, 6, 0.3, 0.4, 0.3, 0.02, 1);
            return;
        }

        if (ItemStack.class.equals(dataType)) {
            player.getWorld().spawnParticle(particle, center, 6, 0.3, 0.4, 0.3, 0.02,
                    new ItemStack(org.bukkit.Material.STONE));
            return;
        }

        if (BlockData.class.equals(dataType)) {
            player.getWorld().spawnParticle(particle, center, 6, 0.3, 0.4, 0.3, 0.02,
                    org.bukkit.Material.STONE.createBlockData());
            return;
        }

        player.getWorld().spawnParticle(Particle.ENCHANT, center, 6, 0.3, 0.4, 0.3, 0.02);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
