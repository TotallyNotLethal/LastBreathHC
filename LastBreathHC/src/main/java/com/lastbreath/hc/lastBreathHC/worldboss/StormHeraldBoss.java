package com.lastbreath.hc.lastBreathHC.worldboss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.WorldBorder;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class StormHeraldBoss extends BaseWorldBossController {

    private static final String PHASE_SHIELDED = "shielded";
    private static final String PHASE_STORMCALLER = "stormcaller";
    private static final Material ANCHOR_MATERIAL = Material.LIGHTNING_ROD;

    private final Set<Location> anchors = new HashSet<>();
    private int lightningCooldownTicks;
    private int gustCooldownTicks;
    private boolean enraged;

    public StormHeraldBoss(Plugin plugin, LivingEntity boss) {
        super(plugin, boss, "world_boss_stormherald_phase", "world_boss_stormherald_anchors", "world_boss_stormherald_data");
    }

    @Override
    public WorldBossType getType() {
        return WorldBossType.STORM_HERALD;
    }

    @Override
    public void rebuildFromPersistent() {
        anchors.clear();
        anchors.addAll(loadBlockLocations());
        if (anchors.isEmpty()) {
            anchors.addAll(spawnAnchors());
        }
        storeBlockLocations(anchors);
        boolean shielded = !anchors.isEmpty();
        setPhase(shielded ? PHASE_SHIELDED : PHASE_STORMCALLER);
        boss.setInvulnerable(shielded);
    }

    @Override
    public void tick() {
        anchors.removeIf(location -> location.getBlock().getType() != ANCHOR_MATERIAL);
        if (!anchors.isEmpty()) {
            spawnAnchorSafeRings();
        }
        if (PHASE_SHIELDED.equals(getPhase(PHASE_SHIELDED))) {
            if (anchors.isEmpty()) {
                dropShield();
            } else {
                pulseShield();
            }
            return;
        }
        double maxHealth = boss.getAttribute(Attribute.MAX_HEALTH) != null
                ? boss.getAttribute(Attribute.MAX_HEALTH).getValue()
                : boss.getHealth();
        double healthFraction = maxHealth > 0 ? boss.getHealth() / maxHealth : 1.0;
        if (!enraged && healthFraction <= 0.2) {
            enraged = true;
            boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.7f);
            boss.getWorld().spawnParticle(Particle.FLASH, boss.getLocation(), 2, 0.4, 0.6, 0.4, 0.0);
        }
        lightningCooldownTicks = Math.max(0, lightningCooldownTicks - 20);
        gustCooldownTicks = Math.max(0, gustCooldownTicks - 20);
        if (lightningCooldownTicks <= 0) {
            triggerChainLightning();
            lightningCooldownTicks = enraged ? 80 : 140;
        }
        if (gustCooldownTicks <= 0) {
            triggerWindGust();
            gustCooldownTicks = enraged ? 120 : 180;
        }
    }

    @Override
    public void handleBossDamaged(EntityDamageByEntityEvent event) {
        if (PHASE_SHIELDED.equals(getPhase(PHASE_SHIELDED))) {
            event.setCancelled(true);
            boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.3f);
        }
    }

    @Override
    public void handleBossAttack(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, enraged ? 1 : 0));
        }
    }

    @Override
    public void handleBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != ANCHOR_MATERIAL) {
            return;
        }
        Location location = event.getBlock().getLocation();
        if (anchors.remove(location)) {
            storeBlockLocations(anchors);
            boss.getWorld().spawnParticle(Particle.CRIT, location, 12, 0.6, 0.4, 0.6, 0.2);
            if (anchors.isEmpty()) {
                dropShield();
            }
        }
    }

    @Override
    public boolean isBreakableMechanicBlock(Block block) {
        if (block.getType() != ANCHOR_MATERIAL) {
            return false;
        }
        return anchors.contains(block.getLocation());
    }

    @Override
    public void cleanup() {
        for (Location location : anchors) {
            if (location.getBlock().getType() == ANCHOR_MATERIAL) {
                location.getBlock().setType(Material.AIR);
            }
        }
        anchors.clear();
    }

    @Override
    public void onArenaEmpty() {
        boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        if (!PHASE_SHIELDED.equals(getPhase(PHASE_SHIELDED))) {
            anchors.clear();
            anchors.addAll(spawnAnchors());
            storeBlockLocations(anchors);
            setPhase(PHASE_SHIELDED);
            boss.setInvulnerable(true);
        }
    }

    private Set<Location> spawnAnchors() {
        Set<Location> locations = new HashSet<>();
        Location center = boss.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return locations;
        }
        WorldBorder border = world.getWorldBorder();
        Location borderCenter = border.getCenter();
        double maxRadius = Math.max(2.0, (border.getSize() / 2.0) - 2.0);
        int count = 4;
        double radius = 8.0;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 / count) * i;
            Location anchorBase = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            double dx = anchorBase.getX() - borderCenter.getX();
            double dz = anchorBase.getZ() - borderCenter.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance > maxRadius) {
                double scale = maxRadius / distance;
                anchorBase.setX(borderCenter.getX() + dx * scale);
                anchorBase.setZ(borderCenter.getZ() + dz * scale);
            }
            anchorBase.setY(world.getHighestBlockYAt(anchorBase));
            Location place = anchorBase.clone().add(0, 1, 0);
            if (!place.getBlock().getType().isAir()) {
                continue;
            }
            place.getBlock().setType(ANCHOR_MATERIAL);
            locations.add(place.getBlock().getLocation());
        }
        return locations;
    }

    private void dropShield() {
        setPhase(PHASE_STORMCALLER);
        boss.setInvulnerable(false);
        World world = boss.getWorld();
        world.playSound(boss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.2f, 0.7f);
        world.spawnParticle(Particle.ELECTRIC_SPARK, boss.getLocation(), 60, 1.8, 1.0, 1.8, 0.1);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0));
    }

    private void pulseShield() {
        boss.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, boss.getLocation(), 10, 1.0, 0.8, 1.0, 0.1);
    }

    private void triggerChainLightning() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        world.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.1f, 1.5f);
        world.spawnParticle(Particle.ELECTRIC_SPARK, center, 30, 1.5, 1.0, 1.5, 0.2);
        double safeRadius = getLightningSafeRadius();
        double safeRadiusSquared = safeRadius * safeRadius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > 225) {
                continue;
            }
            world.strikeLightningEffect(player.getLocation());
            if (safeRadiusSquared > 0.0 && isPlayerInAnchorSafeZone(player.getLocation(), safeRadiusSquared)) {
                world.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 0.2, 0), 8, 0.4, 0.4, 0.4, 0.02);
                continue;
            }
            player.damage(enraged ? 7.0 : 4.0, boss);
        }
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }

    private void triggerWindGust() {
        Location center = boss.getLocation();
        World world = boss.getWorld();
        world.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 1.4f);
        world.spawnParticle(Particle.CLOUD, center, 20, 1.5, 0.6, 1.5, 0.1);
        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distance(center);
            if (distance > 12) {
                continue;
            }
            Vector knockback = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(enraged ? 1.6 : 1.2);
            knockback.setY(0.6);
            player.setVelocity(knockback);
            player.damage(enraged ? 3.0 : 2.0, boss);
        }
        world.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.1f, 0.8f);
        world.spawnParticle(Particle.CLOUD, center, 50, 2.5, 0.8, 2.5, 0.2);
    }

    private boolean isPlayerInAnchorSafeZone(Location playerLocation, double safeRadiusSquared) {
        if (anchors.isEmpty()) {
            return false;
        }
        for (Location anchor : anchors) {
            if (!anchor.getWorld().equals(playerLocation.getWorld())) {
                continue;
            }
            if (playerLocation.distanceSquared(anchor) <= safeRadiusSquared) {
                return true;
            }
        }
        return false;
    }

    private double getLightningSafeRadius() {
        return Math.max(0.0, plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.lightningSafeRadius", 3.0));
    }

    private void spawnAnchorSafeRings() {
        double radius = getLightningSafeRadius();
        if (radius <= 0.0) {
            return;
        }
        World world = boss.getWorld();
        int points = 16;
        for (Location anchor : anchors) {
            if (!anchor.getWorld().equals(world)) {
                continue;
            }
            Location center = anchor.clone().add(0.5, 0.1, 0.5);
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0 / points) * i;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location point = center.clone().add(x, 0.0, z);
                world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }
}
