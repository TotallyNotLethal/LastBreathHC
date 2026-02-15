package com.lastbreath.hc.lastBreathHC.asteroid;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.OptionalLong;
import java.util.Set;

public class AsteroidBossMechanics implements Listener {
    private static final long FIREWALL_FIRE_DURATION_TICKS = 60L;
    private static final Set<EntityType> TIER3_BOSS_TYPES = EnumSet.of(
            EntityType.RAVAGER,
            EntityType.WARDEN,
            EntityType.PIGLIN_BRUTE,
            EntityType.WITHER_SKELETON
    );
    private static final String META_PROJECTILE_BLAST = "lb_asteroid_projectile_blast";
    private static final String META_FIREWALL = "lb_asteroid_firewall";
    private static final String META_RAVAGER_SLAM = "lb_asteroid_ravager_slam";
    private static final String META_WARDEN_PULSE = "lb_asteroid_warden_pulse";
    private static final String META_BRUTE_FLAMES = "lb_asteroid_brute_flames";
    private static final String META_WITHER_NOVA = "lb_asteroid_wither_nova";

    private final JavaPlugin plugin;
    private BukkitTask bossTask;

    public AsteroidBossMechanics(JavaPlugin plugin) {
        this.plugin = plugin;
        startBossTask();
    }

    private void startBossTask() {
        if (bossTask != null) {
            return;
        }
        bossTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickBosses();
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    private void tickBosses() {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
                if (!isTier3AsteroidBoss(entity)) {
                    continue;
                }
                if (entity.getType() == EntityType.WARDEN && entity instanceof Mob mob) {
                    LivingEntity target = mob.getTarget();
                    if (target != null && !target.isDead()) {
                        attemptFirewall(entity, target);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!isTier3AsteroidBoss(target)) {
            return;
        }
        long now = currentTick(target);
        if (!cooldownReady(target, META_PROJECTILE_BLAST, now)) {
            return;
        }
        setCooldown(target, META_PROJECTILE_BLAST, now, 60L);
        Location impact = event.getEntity().getLocation();
        World world = impact.getWorld();
        if (world != null) {
            world.createExplosion(impact, 2.5f, false, false, target);
            world.spawnParticle(Particle.EXPLOSION, impact, 1);
        }
    }

    @EventHandler
    public void onAsteroidBossDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity damager && isTier3AsteroidBoss(damager)) {
            switch (damager.getType()) {
                case RAVAGER -> attemptRavagerSlam(damager);
                case PIGLIN_BRUTE -> attemptBruteFlames(damager, event.getEntity());
                case WITHER_SKELETON -> attemptWitherNova(damager);
                default -> {
                }
            }
        }

        if (event.getEntity() instanceof LivingEntity target && isTier3AsteroidBoss(target)
                && target.getType() == EntityType.WARDEN) {
            attemptWardenPulse(target);
        }
    }

    private void attemptRavagerSlam(LivingEntity ravager) {
        long now = currentTick(ravager);
        if (!cooldownReady(ravager, META_RAVAGER_SLAM, now)) {
            return;
        }
        setCooldown(ravager, META_RAVAGER_SLAM, now, 100L);
        Location center = ravager.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.6f);
        world.spawnParticle(Particle.EXPLOSION, center, 3);
        for (LivingEntity nearby : center.getNearbyLivingEntities(4.5, 2.5, 4.5)) {
            if (!(nearby instanceof Player)) {
                continue;
            }
            Vector knockback = nearby.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.3);
            knockback.setY(0.5);
            nearby.setVelocity(knockback);
            nearby.damage(6.0, ravager);
        }
    }

    private void attemptWardenPulse(LivingEntity warden) {
        long now = currentTick(warden);
        if (!cooldownReady(warden, META_WARDEN_PULSE, now)) {
            return;
        }
        setCooldown(warden, META_WARDEN_PULSE, now, 140L);
        Location center = warden.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.8f);
        world.spawnParticle(Particle.SONIC_BOOM, center, 1);
        for (LivingEntity nearby : center.getNearbyLivingEntities(6.0, 3.0, 6.0)) {
            if (!(nearby instanceof Player)) {
                continue;
            }
            nearby.damage(7.0, warden);
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        }
    }

    private void attemptFirewall(LivingEntity warden, LivingEntity target) {
        long now = currentTick(warden);
        if (!cooldownReady(warden, META_FIREWALL, now)) {
            return;
        }
        setCooldown(warden, META_FIREWALL, now, 120L);
        spawnFirewallRing(warden, target.getLocation(), 4);
    }

    private void spawnFirewallRing(LivingEntity caster, Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Set<Location> temporaryFireBlocks = new java.util.HashSet<>();
        for (int degrees = 0; degrees < 360; degrees += 20) {
            double radians = Math.toRadians(degrees);
            int x = (int) Math.round(Math.cos(radians) * radius);
            int z = (int) Math.round(Math.sin(radians) * radius);
            Location fireLoc = center.clone().add(x, 0, z);
            if (fireLoc.getBlock().getType() != Material.AIR) {
                continue;
            }
            Location below = fireLoc.clone().add(0, -1, 0);
            if (below.getBlock().getType().isSolid()) {
                fireLoc.getBlock().setType(Material.FIRE);
                temporaryFireBlocks.add(fireLoc.getBlock().getLocation());
            }
        }
        if (!temporaryFireBlocks.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Location fireBlock : temporaryFireBlocks) {
                    if (fireBlock.getBlock().getType() == Material.FIRE) {
                        fireBlock.getBlock().setType(Material.AIR);
                    }
                }
            }, FIREWALL_FIRE_DURATION_TICKS);
        }
        world.spawnParticle(Particle.FLAME, center, 80, radius, 0.5, radius, 0.02);
        for (LivingEntity nearby : center.getNearbyLivingEntities(radius, 2.0, radius)) {
            if (nearby instanceof Player) {
                nearby.damage(4.0, caster);
                nearby.setFireTicks(Math.max(nearby.getFireTicks(), 80));
            }
        }
    }

    private void attemptBruteFlames(LivingEntity brute, Entity target) {
        long now = currentTick(brute);
        if (!cooldownReady(brute, META_BRUTE_FLAMES, now)) {
            return;
        }
        setCooldown(brute, META_BRUTE_FLAMES, now, 100L);
        Location center = brute.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.9f);
        world.spawnParticle(Particle.FLAME, center, 40, 1.5, 0.8, 1.5, 0.03);
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.setFireTicks(Math.max(livingTarget.getFireTicks(), 100));
            livingTarget.damage(4.0, brute);
        }
        for (LivingEntity nearby : center.getNearbyLivingEntities(3.0, 2.0, 3.0)) {
            if (nearby instanceof Player) {
                nearby.setFireTicks(Math.max(nearby.getFireTicks(), 60));
            }
        }
    }

    private void attemptWitherNova(LivingEntity witherSkeleton) {
        long now = currentTick(witherSkeleton);
        if (!cooldownReady(witherSkeleton, META_WITHER_NOVA, now)) {
            return;
        }
        setCooldown(witherSkeleton, META_WITHER_NOVA, now, 120L);
        Location center = witherSkeleton.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.2f);
        world.spawnParticle(Particle.SMOKE, center, 60, 2.0, 1.0, 2.0, 0.02);
        for (LivingEntity nearby : center.getNearbyLivingEntities(4.0, 2.0, 4.0)) {
            if (nearby instanceof Player) {
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
                nearby.damage(3.0, witherSkeleton);
            }
        }
    }

    private boolean isTier3AsteroidBoss(LivingEntity entity) {
        if (!entity.getScoreboardTags().contains(AsteroidManager.ASTEROID_MOB_TAG)) {
            return false;
        }
        if (!TIER3_BOSS_TYPES.contains(entity.getType())) {
            return false;
        }
        return entity.getScoreboardTags().contains(AsteroidManager.ASTEROID_TIER_TAG_PREFIX + "3");
    }

    private long currentTick(LivingEntity entity) {
        World world = entity.getWorld();
        return world != null ? world.getFullTime() : 0L;
    }

    private boolean cooldownReady(LivingEntity entity, String key, long currentTick) {
        OptionalLong next = entity.getMetadata(key).stream()
                .filter(value -> value.getOwningPlugin() == plugin)
                .mapToLong(MetadataValue::asLong)
                .findFirst();
        return next.isEmpty() || currentTick >= next.getAsLong();
    }

    private void setCooldown(LivingEntity entity, String key, long currentTick, long cooldownTicks) {
        entity.setMetadata(key, new FixedMetadataValue(plugin, currentTick + cooldownTicks));
    }
}
