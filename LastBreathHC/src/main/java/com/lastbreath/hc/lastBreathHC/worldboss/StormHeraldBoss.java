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
    private static final String PHASE_TEMPEST = "tempest";
    private static final Material ANCHOR_MATERIAL = Material.LIGHTNING_ROD;

    private final Set<Location> anchors = new HashSet<>();
    private int lightningCooldownTicks;
    private int gustCooldownTicks;
    private int stormCooldownTicks;

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
        if (shielded) {
            setPhase(PHASE_SHIELDED);
        } else {
            String phase = getPhase(PHASE_STORMCALLER);
            setPhase(PHASE_TEMPEST.equals(phase) ? PHASE_TEMPEST : PHASE_STORMCALLER);
        }
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
        if (PHASE_STORMCALLER.equals(getPhase(PHASE_SHIELDED)) && healthFraction <= getTempestThreshold()) {
            enterTempest();
        }
        lightningCooldownTicks = Math.max(0, lightningCooldownTicks - 20);
        gustCooldownTicks = Math.max(0, gustCooldownTicks - 20);
        stormCooldownTicks = Math.max(0, stormCooldownTicks - 20);
        if (lightningCooldownTicks <= 0) {
            triggerChainLightning();
            lightningCooldownTicks = getLightningCooldownTicks();
        }
        if (gustCooldownTicks <= 0) {
            triggerWindGust();
            gustCooldownTicks = getWindGustCooldownTicks();
        }
        if (PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED)) && stormCooldownTicks <= 0) {
            triggerTempestStorm();
            stormCooldownTicks = getStormCooldownTicks();
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
            int amplifier = PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED)) ? 1 : 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, amplifier));
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
            int floorY = findFloorY(world, anchorBase.getBlockX(), anchorBase.getBlockZ());
            anchorBase.setY(floorY);
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

    private void enterTempest() {
        setPhase(PHASE_TEMPEST);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 240, 1));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 240, 0));
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
        boss.getWorld().spawnParticle(Particle.FLASH, boss.getLocation(), 3, 0.6, 0.8, 0.6, 0.0);
    }

    private void pulseShield() {
        boss.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, boss.getLocation(), 10, 1.0, 0.8, 1.0, 0.1);
    }

    private void triggerChainLightning() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        telegraphChainLightning(center);
        double safeRadius = getLightningSafeRadius();
        double safeRadiusSquared = safeRadius * safeRadius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > 256) {
                continue;
            }
            world.strikeLightningEffect(player.getLocation());
            if (safeRadiusSquared > 0.0 && isPlayerInAnchorSafeZone(player.getLocation(), safeRadiusSquared)) {
                world.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 0.2, 0), 8, 0.4, 0.4, 0.4, 0.02);
                continue;
            }
            player.damage(getChainLightningDamage(), boss);
        }
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }

    private void triggerWindGust() {
        Location center = boss.getLocation();
        World world = boss.getWorld();
        telegraphWindGust(center);
        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distance(center);
            if (distance > getWindGustRadius()) {
                continue;
            }
            Vector knockback = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(getWindGustKnockback());
            knockback.setY(PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED)) ? 0.75 : 0.6);
            player.setVelocity(knockback);
            player.damage(getWindGustDamage(), boss);
        }
        world.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.1f, 0.8f);
        world.spawnParticle(Particle.CLOUD, center, 50, 2.5, 0.8, 2.5, 0.2);
    }

    private void triggerTempestStorm() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        double radius = getTempestStormRadius();
        double radiusSquared = radius * radius;
        for (Player player : world.getPlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSquared) {
                continue;
            }
            Location strike = player.getLocation().clone().add((Math.random() - 0.5) * 4.0, 0.0, (Math.random() - 0.5) * 4.0);
            world.strikeLightningEffect(strike);
            player.damage(getTempestStormDamage(), boss);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        }
        world.spawnParticle(Particle.ELECTRIC_SPARK, center, 80, 2.5, 1.0, 2.5, 0.2);
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.8f);
    }

    private void telegraphChainLightning(Location center) {
        for (Player player : boss.getWorld().getPlayers()) {
            if (isTelegraphBlocked(player)) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > 256) {
                continue;
            }
            player.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.1f, 1.5f);
            player.spawnParticle(Particle.ELECTRIC_SPARK, center, 30, 1.5, 1.0, 1.5, 0.2);
        }
    }

    private void telegraphWindGust(Location center) {
        for (Player player : boss.getWorld().getPlayers()) {
            if (isTelegraphBlocked(player)) {
                continue;
            }
            if (player.getLocation().distance(center) > getWindGustRadius()) {
                continue;
            }
            player.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 1.4f);
            player.spawnParticle(Particle.CLOUD, center, 20, 1.5, 0.6, 1.5, 0.1);
        }
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

    private double getTempestThreshold() {
        return plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.tempestThreshold", 0.3);
    }

    private int getLightningCooldownTicks() {
        int base = plugin.getConfig().getInt("worldBoss.bosses.StormHerald.lightningCooldownTicks", 120);
        if (PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED))) {
            return Math.max(60, (int) Math.round(base * 0.7));
        }
        return base;
    }

    private int getWindGustCooldownTicks() {
        int base = plugin.getConfig().getInt("worldBoss.bosses.StormHerald.windGustCooldownTicks", 160);
        if (PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED))) {
            return Math.max(80, (int) Math.round(base * 0.75));
        }
        return base;
    }

    private double getChainLightningDamage() {
        double base = plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.chainLightningDamage", 5.0);
        if (PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED))) {
            return base + plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.tempestBonusDamage", 3.0);
        }
        return base;
    }

    private double getWindGustDamage() {
        double base = plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.windGustDamage", 2.5);
        if (PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED))) {
            return base + plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.tempestBonusDamage", 3.0);
        }
        return base;
    }

    private double getWindGustRadius() {
        double base = plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.windGustRadius", 12.0);
        if (PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED))) {
            return base + plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.tempestRadiusBonus", 3.0);
        }
        return base;
    }

    private double getWindGustKnockback() {
        double base = plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.windGustKnockback", 1.3);
        if (PHASE_TEMPEST.equals(getPhase(PHASE_SHIELDED))) {
            return base + 0.3;
        }
        return base;
    }

    private int getStormCooldownTicks() {
        return plugin.getConfig().getInt("worldBoss.bosses.StormHerald.tempestStormCooldownTicks", 140);
    }

    private double getTempestStormRadius() {
        return plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.tempestStormRadius", 14.0);
    }

    private double getTempestStormDamage() {
        return plugin.getConfig().getDouble("worldBoss.bosses.StormHerald.tempestStormDamage", 4.5);
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
