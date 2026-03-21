package com.lastbreath.hc.lastBreathHC.cosmetics;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class StasisAuraTeleportListener implements Listener {

    private static final BossAura STASIS_AURA = BossAura.STASIS_AURA;
    private static final int BURST_STEPS = 10;
    private static final long BURST_INTERVAL_TICKS = 2L;
    private static final Particle.DustOptions PRIMARY_DUST = new Particle.DustOptions(Color.fromRGB(64, 224, 255), 1.6f);
    private static final Particle.DustOptions ACCENT_DUST = new Particle.DustOptions(Color.fromRGB(180, 255, 255), 1.0f);

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL || event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!CosmeticManager.isBackgroundAuraEnabled(player, STASIS_AURA)) {
            return;
        }
        Location destination = event.getTo();
        if (destination == null) {
            return;
        }

        Location center = destination.clone().add(0.0, 1.0, 0.0);
        player.getServer().getScheduler().runTask(LastBreathHC.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            playBurst(player, center);
        });
    }

    private void playBurst(Player player, Location center) {
        center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 1.55f);
        center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.35f, 1.8f);

        new BukkitRunnable() {
            private int step = 0;

            @Override
            public void run() {
                if (!player.isOnline() || center.getWorld() == null) {
                    cancel();
                    return;
                }
                spawnStasisFrame(center, step);
                step++;
                if (step >= BURST_STEPS) {
                    cancel();
                }
            }
        }.runTaskTimer(LastBreathHC.getInstance(), 0L, BURST_INTERVAL_TICKS);
    }

    private void spawnStasisFrame(Location center, int step) {
        double progress = step / (double) BURST_STEPS;
        double radius = 0.75 + (1.8 - progress);
        double height = 0.15 + progress * 1.3;
        double twist = progress * Math.PI * 2.4;

        for (int i = 0; i < 2; i++) {
            double angle = twist + (Math.PI * i);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location point = center.clone().add(x, height, z);
            center.getWorld().spawnParticle(Particle.DUST, point, 3, 0.06, 0.06, 0.06, 0.0, PRIMARY_DUST);
        }

        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i / 8.0) + twist * 0.55;
            double x = Math.cos(angle) * (radius * 0.85);
            double z = Math.sin(angle) * (radius * 0.85);
            Location point = center.clone().add(x, 0.1 + (0.18 * step), z);
            center.getWorld().spawnParticle(Particle.DUST, point, 1, 0.03, 0.03, 0.03, 0.0, ACCENT_DUST);
        }

        center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0.0, 0.3 + progress, 0.0), 4,
                0.18, 0.22, 0.18, 0.01);
        center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 8, 0.55, 0.75, 0.55, 0.01);
        center.getWorld().spawnParticle(Particle.ENCHANT, center, 12, 0.4, 0.6, 0.4, 0.0);
    }
}
