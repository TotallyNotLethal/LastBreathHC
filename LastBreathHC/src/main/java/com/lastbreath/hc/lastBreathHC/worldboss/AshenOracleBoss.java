package com.lastbreath.hc.lastBreathHC.worldboss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
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

public class AshenOracleBoss extends BaseWorldBossController {

    private static final String PHASE_RITUAL = "ritual";
    private static final String PHASE_PROPHECY = "prophecy";
    private static final String PHASE_CATACLYSM = "cataclysm";
    private static final Material RELIC_MATERIAL = Material.CRYING_OBSIDIAN;
    private static final int DEFAULT_OMEN_COOLDOWN = 160;
    private static final int DEFAULT_ASH_BURST_COOLDOWN = 140;

    private final Set<Location> relics = new HashSet<>();
    private int omenCooldownTicks;
    private int ashBurstCooldownTicks;

    public AshenOracleBoss(Plugin plugin, LivingEntity boss) {
        super(plugin, boss, "world_boss_ashenoracle_phase", "world_boss_ashenoracle_relics", "world_boss_ashenoracle_data");
    }

    @Override
    public WorldBossType getType() {
        return WorldBossType.ASHEN_ORACLE;
    }

    @Override
    public void rebuildFromPersistent() {
        String phase = getPhase(PHASE_RITUAL);
        int[] data = parseInts(getData(serializeInts(DEFAULT_OMEN_COOLDOWN, DEFAULT_ASH_BURST_COOLDOWN)), 2);
        omenCooldownTicks = data[0] > 0 ? data[0] : DEFAULT_OMEN_COOLDOWN;
        ashBurstCooldownTicks = data[1] > 0 ? data[1] : DEFAULT_ASH_BURST_COOLDOWN;

        relics.clear();
        relics.addAll(loadBlockLocations());
        if (PHASE_RITUAL.equals(phase)) {
            if (relics.isEmpty()) {
                relics.addAll(spawnRelics());
                storeBlockLocations(relics);
            }
            if (relics.isEmpty()) {
                transitionToProphecy();
                phase = PHASE_PROPHECY;
            } else {
                boss.setInvulnerable(true);
            }
        } else {
            boss.setInvulnerable(false);
        }
        setPhase(phase);
        updatePersistentData();
    }

    @Override
    public void tick() {
        relics.removeIf(location -> location.getBlock().getType() != RELIC_MATERIAL);
        if (!relics.isEmpty()) {
            spawnRelicRings();
        }
        String phase = getPhase(PHASE_RITUAL);
        if (PHASE_RITUAL.equals(phase)) {
            if (relics.isEmpty()) {
                transitionToProphecy();
            } else {
                pulseShield();
            }
            return;
        }
        if (PHASE_PROPHECY.equals(phase) && shouldEnterCataclysm()) {
            transitionToCataclysm();
            phase = PHASE_CATACLYSM;
        }

        spawnSafeRing();
        omenCooldownTicks = Math.max(0, omenCooldownTicks - 20);
        if (omenCooldownTicks <= 0) {
            triggerOmen(PHASE_CATACLYSM.equals(phase));
            omenCooldownTicks = PHASE_CATACLYSM.equals(phase) ? DEFAULT_ASH_BURST_COOLDOWN : DEFAULT_OMEN_COOLDOWN;
        }

        if (PHASE_CATACLYSM.equals(phase)) {
            ashBurstCooldownTicks = Math.max(0, ashBurstCooldownTicks - 20);
            if (ashBurstCooldownTicks <= 0) {
                triggerAshBurst();
                ashBurstCooldownTicks = DEFAULT_ASH_BURST_COOLDOWN;
            }
        }
        updatePersistentData();
    }

    @Override
    public void handleBossDamaged(EntityDamageByEntityEvent event) {
        if (PHASE_RITUAL.equals(getPhase(PHASE_RITUAL))) {
            event.setCancelled(true);
            boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);
        }
    }

    @Override
    public void handleBossAttack(EntityDamageByEntityEvent event) {
        if (PHASE_CATACLYSM.equals(getPhase(PHASE_RITUAL)) && event.getEntity() instanceof Player player) {
            player.setFireTicks(60);
        }
    }

    @Override
    public void handleBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != RELIC_MATERIAL) {
            return;
        }
        Location location = event.getBlock().getLocation();
        if (relics.remove(location)) {
            storeBlockLocations(relics);
            boss.getWorld().playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.0f);
            if (relics.isEmpty()) {
                transitionToProphecy();
            }
        }
    }

    @Override
    public boolean isBreakableMechanicBlock(Block block) {
        if (block.getType() != RELIC_MATERIAL) {
            return false;
        }
        return relics.contains(block.getLocation());
    }

    @Override
    public void cleanup() {
        for (Location location : relics) {
            if (location.getBlock().getType() == RELIC_MATERIAL) {
                location.getBlock().setType(Material.AIR);
            }
        }
        relics.clear();
    }

    @Override
    public void onArenaEmpty() {
        boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        relics.clear();
        relics.addAll(spawnRelics());
        storeBlockLocations(relics);
        setPhase(PHASE_RITUAL);
        boss.setInvulnerable(!relics.isEmpty());
        omenCooldownTicks = DEFAULT_OMEN_COOLDOWN;
        ashBurstCooldownTicks = DEFAULT_ASH_BURST_COOLDOWN;
        updatePersistentData();
    }

    private Set<Location> spawnRelics() {
        Set<Location> locations = new HashSet<>();
        Location center = boss.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return locations;
        }
        WorldBorder border = world.getWorldBorder();
        Location borderCenter = border.getCenter();
        double maxRadius = Math.max(2.0, (border.getSize() / 2.0) - 2.0);
        int count = getRelicCount();
        double radius = getRelicRadius();
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 / count) * i;
            Location base = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            double dx = base.getX() - borderCenter.getX();
            double dz = base.getZ() - borderCenter.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance > maxRadius) {
                double scale = maxRadius / distance;
                base.setX(borderCenter.getX() + dx * scale);
                base.setZ(borderCenter.getZ() + dz * scale);
            }
            int floorY = findFloorY(world, base.getBlockX(), base.getBlockZ());
            base.setY(floorY);
            Location place = base.clone().add(0, 1, 0);
            if (!place.getBlock().getType().isAir()) {
                continue;
            }
            place.getBlock().setType(RELIC_MATERIAL);
            locations.add(place.getBlock().getLocation());
        }
        return locations;
    }

    private void transitionToProphecy() {
        setPhase(PHASE_PROPHECY);
        boss.setInvulnerable(false);
        omenCooldownTicks = DEFAULT_OMEN_COOLDOWN;
        updatePersistentData();
        World world = boss.getWorld();
        world.playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.2f, 0.9f);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, boss.getLocation(), 60, 1.4, 1.0, 1.4, 0.05);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
    }

    private void transitionToCataclysm() {
        setPhase(PHASE_CATACLYSM);
        omenCooldownTicks = DEFAULT_ASH_BURST_COOLDOWN;
        ashBurstCooldownTicks = DEFAULT_ASH_BURST_COOLDOWN;
        updatePersistentData();
        World world = boss.getWorld();
        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.8f);
        world.spawnParticle(Particle.FLAME, boss.getLocation(), 80, 1.8, 1.2, 1.8, 0.05);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 0));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
    }

    private void pulseShield() {
        boss.getWorld().spawnParticle(Particle.ENCHANT, boss.getLocation(), 15, 1.0, 0.8, 1.0, 0.1);
    }

    private boolean shouldEnterCataclysm() {
        double maxHealth = boss.getAttribute(Attribute.MAX_HEALTH) != null
                ? boss.getAttribute(Attribute.MAX_HEALTH).getValue()
                : boss.getHealth();
        double healthFraction = maxHealth > 0 ? boss.getHealth() / maxHealth : 1.0;
        double threshold = Math.max(0.05, Math.min(1.0, getCataclysmThreshold()));
        return healthFraction <= threshold;
    }

    private void triggerOmen(boolean enraged) {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        telegraphOmen(center);
        double safeRadius = getOmenSafeRadius();
        double safeRadiusSquared = safeRadius * safeRadius;
        double rangeSquared = 20.0 * 20.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSquared) {
                continue;
            }
            if (safeRadiusSquared > 0.0 && player.getLocation().distanceSquared(center) <= safeRadiusSquared) {
                world.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 0.4, 0), 8, 0.4, 0.4, 0.4, 0.02);
                continue;
            }
            player.damage(enraged ? 5.0 : 3.0, boss);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
        }
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.8f);
    }

    private void triggerAshBurst() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        telegraphAshBurst(center);
        double radius = getAshBurstRadius();
        double radiusSquared = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > radiusSquared) {
                continue;
            }
            Vector knockback = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2);
            knockback.setY(0.6);
            player.setVelocity(knockback);
            player.setFireTicks(80);
            player.damage(6.0, boss);
        }
        world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.1f, 0.9f);
        world.spawnParticle(Particle.ASH, center, 80, 2.2, 1.0, 2.2, 0.02);
    }

    private void telegraphOmen(Location center) {
        for (Player player : boss.getWorld().getPlayers()) {
            if (isTelegraphBlocked(player)) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > 400) {
                continue;
            }
            player.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.8f);
            player.spawnParticle(Particle.SMOKE_LARGE, center, 30, 1.2, 0.8, 1.2, 0.02);
        }
    }

    private void telegraphAshBurst(Location center) {
        for (Player player : boss.getWorld().getPlayers()) {
            if (isTelegraphBlocked(player)) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > 256) {
                continue;
            }
            player.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.2f, 1.0f);
            player.spawnParticle(Particle.FLAME, center, 20, 1.4, 0.6, 1.4, 0.02);
        }
    }

    private double getOmenSafeRadius() {
        return Math.max(0.0, plugin.getConfig().getDouble("worldBoss.bosses.AshenOracle.omenSafeRadius", 4.0));
    }

    private double getAshBurstRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble("worldBoss.bosses.AshenOracle.ashBurstRadius", 10.0));
    }

    private double getRelicRadius() {
        return Math.max(2.0, plugin.getConfig().getDouble("worldBoss.bosses.AshenOracle.ritualRelicRadius", 7.0));
    }

    private int getRelicCount() {
        return Math.max(1, plugin.getConfig().getInt("worldBoss.bosses.AshenOracle.ritualRelicCount", 4));
    }

    private double getCataclysmThreshold() {
        return plugin.getConfig().getDouble("worldBoss.bosses.AshenOracle.cataclysmThreshold", 0.35);
    }

    private void spawnRelicRings() {
        World world = boss.getWorld();
        int points = 16;
        double radius = 2.4;
        for (Location relic : relics) {
            if (!relic.getWorld().equals(world)) {
                continue;
            }
            Location center = relic.clone().add(0.5, 0.1, 0.5);
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0 / points) * i;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location point = center.clone().add(x, 0.0, z);
                world.spawnParticle(Particle.SOUL, point, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    private void spawnSafeRing() {
        double radius = getOmenSafeRadius();
        if (radius <= 0.0) {
            return;
        }
        World world = boss.getWorld();
        int points = 18;
        Location center = boss.getLocation().clone().add(0.5, 0.1, 0.5);
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location point = center.clone().add(x, 0.0, z);
            world.spawnParticle(Particle.END_ROD, point, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private void updatePersistentData() {
        setData(serializeInts(omenCooldownTicks, ashBurstCooldownTicks));
    }
}
