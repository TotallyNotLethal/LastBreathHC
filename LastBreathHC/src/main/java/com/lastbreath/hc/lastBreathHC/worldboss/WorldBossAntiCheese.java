package com.lastbreath.hc.lastBreathHC.worldboss;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldBossAntiCheese {

    private static final String CONFIG_ROOT = "worldBoss.antiCheese";

    private final Plugin plugin;
    private final Map<UUID, BossState> bossStates = new HashMap<>();

    public WorldBossAntiCheese(Plugin plugin) {
        this.plugin = plugin;
    }

    public void recordDamage(WorldBossController controller, EntityDamageByEntityEvent event) {
        LivingEntity boss = controller.getBoss();
        if (boss == null) {
            return;
        }
        BossState state = bossStates.computeIfAbsent(boss.getUniqueId(), id -> new BossState());
        state.lastDamageTimeMillis = System.currentTimeMillis();

        Player attacker = resolvePlayer(event);
        if (attacker == null) {
            return;
        }

        double maxDistance = getDouble("maxPlayerDistance", 48.0);
        double maxHeight = getDouble("maxPillarHeight", 6.0);
        double distance = attacker.getLocation().distance(boss.getLocation());
        double heightDelta = attacker.getLocation().getY() - boss.getLocation().getY();

        if (distance > maxDistance || heightDelta > maxHeight) {
            event.setCancelled(true);
            teleportBossToPlayer(controller, attacker);
            return;
        }

        if (event.getDamager() instanceof Projectile) {
            state.rangedHits++;
            int maxRangedHits = getInt("maxRangedHits", 6);
            if (state.rangedHits >= maxRangedHits) {
                state.rangedHits = 0;
                teleportBossToPlayer(controller, attacker);
            }
        }
    }

    public boolean tick(WorldBossController controller) {
        LivingEntity boss = controller.getBoss();
        if (boss == null) {
            return false;
        }
        BossState state = bossStates.computeIfAbsent(boss.getUniqueId(), id -> new BossState());
        long now = System.currentTimeMillis();

        double arenaRadius = getDouble("arenaRadius", 40.0);
        Player nearest = findNearestPlayer(boss, arenaRadius);
        if (nearest == null) {
            controller.onArenaEmpty();
        } else {
            state.lastPlayerSeenMillis = now;
            double maxTeleportDistance = getDouble("teleportDistance", 30.0);
            if (boss.getLocation().distance(nearest.getLocation()) > maxTeleportDistance) {
                teleportBossToPlayer(controller, nearest);
            }
        }

        long despawnMinutes = getLong("despawnAfterMinutes", 10);
        if (despawnMinutes > 0) {
            long timeoutMillis = Duration.ofMinutes(despawnMinutes).toMillis();
            if (now - state.lastDamageTimeMillis > timeoutMillis) {
                boss.remove();
                controller.cleanup();
                return true;
            }
        }
        return false;
    }

    public void clear(LivingEntity boss) {
        if (boss != null) {
            bossStates.remove(boss.getUniqueId());
        }
    }

    private void teleportBossToPlayer(WorldBossController controller, Player player) {
        LivingEntity boss = controller.getBoss();
        if (boss == null || player == null) {
            return;
        }
        Location target = player.getLocation().clone().add(0, 1.0, 0);
        boss.teleport(target);
    }

    private Player findNearestPlayer(LivingEntity boss, double radius) {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        double radiusSquared = radius * radius;
        Player nearest = null;
        double closest = Double.MAX_VALUE;
        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distanceSquared(center);
            if (distance > radiusSquared) {
                continue;
            }
            if (distance < closest) {
                closest = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private Player resolvePlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private double getDouble(String key, double fallback) {
        return plugin.getConfig().getDouble(CONFIG_ROOT + "." + key, fallback);
    }

    private int getInt(String key, int fallback) {
        return plugin.getConfig().getInt(CONFIG_ROOT + "." + key, fallback);
    }

    private long getLong(String key, long fallback) {
        return plugin.getConfig().getLong(CONFIG_ROOT + "." + key, fallback);
    }

    private static class BossState {
        private long lastDamageTimeMillis = System.currentTimeMillis();
        private long lastPlayerSeenMillis = System.currentTimeMillis();
        private int rangedHits;
    }
}
