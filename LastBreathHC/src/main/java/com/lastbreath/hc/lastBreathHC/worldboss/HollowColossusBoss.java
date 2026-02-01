package com.lastbreath.hc.lastBreathHC.worldboss;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HollowColossusBoss extends BaseWorldBossController {

    private static final String PHASE_CLOSED = "closed";
    private static final String PHASE_EXPOSED = "exposed";

    private static final int CLOSED_TICKS = 240;
    private static final int EXPOSED_TICKS = 120;

    private int phaseTicksRemaining;
    private int loopCount;
    private int difficultyBonus;
    private final Random random = new Random();

    public HollowColossusBoss(Plugin plugin, LivingEntity boss) {
        super(plugin, boss, "world_boss_hollowcolossus_phase", "world_boss_hollowcolossus_blocks", "world_boss_hollowcolossus_data");
    }

    @Override
    public WorldBossType getType() {
        return WorldBossType.HOLLOW_COLOSSUS;
    }

    @Override
    public void rebuildFromPersistent() {
        String phase = getPhase(PHASE_CLOSED);
        int[] data = parseInts(getData("0,0"), 2);
        loopCount = data[0];
        difficultyBonus = data[1];
        phaseTicksRemaining = PHASE_EXPOSED.equals(phase) ? EXPOSED_TICKS : CLOSED_TICKS;
        if (PHASE_CLOSED.equals(phase)) {
            spawnMinionWave();
        }
    }

    @Override
    public void tick() {
        spawnSafeZoneMarkers();
        if (phaseTicksRemaining == 60) {
            telegraphPhaseShift();
        }
        phaseTicksRemaining -= 20;
        if (phaseTicksRemaining > 0) {
            return;
        }
        if (PHASE_CLOSED.equals(getPhase(PHASE_CLOSED))) {
            openCore();
        } else {
            closeCore();
        }
    }

    @Override
    public void handleBossDamaged(EntityDamageByEntityEvent event) {
        if (!PHASE_EXPOSED.equals(getPhase(PHASE_CLOSED))) {
            event.setCancelled(true);
            return;
        }
        if (!isWeakSpotHit(event)) {
            event.setCancelled(true);
            boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.9f, 1.4f);
        }
    }

    @Override
    public void handleBossAttack(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.setVelocity(player.getVelocity().add(new Vector(0, 0.2, 0)));
        }
    }

    @Override
    public void cleanup() {
        // no persistent blocks to clear for this boss
    }

    @Override
    public void onArenaEmpty() {
        boss.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 100, 1));
        if (!PHASE_CLOSED.equals(getPhase(PHASE_CLOSED))) {
            setPhase(PHASE_CLOSED);
            phaseTicksRemaining = CLOSED_TICKS;
        }
    }

    private void openCore() {
        setPhase(PHASE_EXPOSED);
        phaseTicksRemaining = EXPOSED_TICKS;
        World world = boss.getWorld();
        world.playSound(boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.2f);
        world.spawnParticle(Particle.SOUL, boss.getLocation(), 60, 1.5, 1.0, 1.5, 0.1);
        world.spawnParticle(Particle.FLASH, boss.getLocation(), 2, 0.3, 0.6, 0.3, 0.0);
        boss.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 40, 0));
    }

    private void closeCore() {
        setPhase(PHASE_CLOSED);
        loopCount++;
        difficultyBonus++;
        setData(serializeInts(loopCount, difficultyBonus));
        phaseTicksRemaining = CLOSED_TICKS;
        spawnMinionWave();
        triggerDebris();
        if (loopCount >= 2) {
            triggerArenaCollapse();
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.2f, 0.6f);
    }

    private void telegraphPhaseShift() {
        World world = boss.getWorld();
        if (PHASE_CLOSED.equals(getPhase(PHASE_CLOSED))) {
            for (Player player : world.getPlayers()) {
                if (isTelegraphBlocked(player)) {
                    continue;
                }
                player.playSound(boss.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.9f, 1.3f);
                player.spawnParticle(Particle.PORTAL, boss.getLocation(), 40, 1.2, 0.8, 1.2, 0.2);
            }
        } else {
            for (Player player : world.getPlayers()) {
                if (isTelegraphBlocked(player)) {
                    continue;
                }
                player.playSound(boss.getLocation(), Sound.BLOCK_SCULK_CHARGE, 0.8f, 1.1f);
                Particle.DustTransition dustTransition = new Particle.DustTransition(
                        Color.fromRGB(120, 20, 160),
                        Color.fromRGB(30, 200, 220),
                        1.3f
                );
                player.spawnParticle(Particle.DUST_COLOR_TRANSITION, boss.getLocation(), 30, 1.0, 0.5, 1.0, 0.02, dustTransition);
            }
        }
    }

    private boolean isWeakSpotHit(EntityDamageByEntityEvent event) {
        Location hitLocation = event.getDamager().getLocation();
        double height = boss.getHeight();
        double threshold = boss.getLocation().getY() + height * 0.7;
        return hitLocation.getY() >= threshold;
    }

    private void spawnMinionWave() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        int count = 3 + difficultyBonus;
        for (int i = 0; i < count; i++) {
            Location spawn = center.clone().add(randomOffset(), 0, randomOffset());
            spawn.setY(world.getHighestBlockYAt(spawn));
            Zombie zombie = world.spawn(spawn, Zombie.class);
            zombie.setTarget(findNearestPlayer());
            if (zombie.getAttribute(Attribute.MAX_HEALTH) != null) {
                zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(10 + difficultyBonus * 2.0);
                zombie.setHealth(zombie.getAttribute(Attribute.MAX_HEALTH).getValue());
            }
        }
        world.playSound(center, Sound.ENTITY_ZOMBIE_AMBIENT, 0.9f, 0.8f);
    }

    private void triggerDebris() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        int debrisCount = 6 + difficultyBonus;
        double safeRadiusSquared = getSafeRadius();
        safeRadiusSquared *= safeRadiusSquared;
        for (int i = 0; i < debrisCount; i++) {
            Location spawn = center.clone().add(randomOffset(), 6 + random.nextDouble() * 3, randomOffset());
            if (isInSafeZone(spawn, safeRadiusSquared)) {
                continue;
            }
            FallingBlock fallingBlock = world.spawnFallingBlock(spawn, Material.COBBLED_DEEPSLATE.createBlockData());
            fallingBlock.setDropItem(false);
            fallingBlock.setVelocity(new Vector(random.nextDouble() - 0.5, -0.4, random.nextDouble() - 0.5));
        }
        world.spawnParticle(Particle.DUST_PLUME, center, 40, 2.0, 1.5, 2.0, Material.DEEPSLATE.createBlockData());
    }

    private void triggerArenaCollapse() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        int radius = 5 + loopCount;
        double safeRadiusSquared = getSafeRadius();
        safeRadiusSquared *= safeRadiusSquared;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (random.nextDouble() > 0.2) {
                    continue;
                }
                Location target = center.clone().add(x, -1, z);
                if (isInSafeZone(target, safeRadiusSquared)) {
                    continue;
                }
                if (!target.getBlock().getType().isSolid()) {
                    continue;
                }
                Material material = target.getBlock().getType();
                target.getBlock().setType(Material.AIR);
                FallingBlock fallingBlock = world.spawnFallingBlock(target.clone().add(0, 1, 0), material.createBlockData());
                fallingBlock.setDropItem(false);
                fallingBlock.setVelocity(new Vector(0, -0.5, 0));
            }
        }
        world.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.7f);
    }

    private double randomOffset() {
        return (random.nextDouble() - 0.5) * 10.0;
    }

    private Player findNearestPlayer() {
        Player nearest = null;
        double closest = Double.MAX_VALUE;
        for (Player player : boss.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(boss.getLocation());
            if (distance < closest) {
                closest = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private double getSafeRadius() {
        return Math.max(0.0, plugin.getConfig().getDouble("worldBoss.bosses.HollowColossus.safeRadius", 3.0));
    }

    private List<Location> getSafeZoneCenters() {
        List<Location> centers = new ArrayList<>();
        Location base = boss.getLocation();
        World world = base.getWorld();
        if (world == null) {
            return centers;
        }
        double offset = 6.0;
        centers.add(base.clone().add(offset, 0.0, 0.0));
        centers.add(base.clone().add(-offset, 0.0, 0.0));
        centers.add(base.clone().add(0.0, 0.0, offset));
        centers.add(base.clone().add(0.0, 0.0, -offset));
        return centers;
    }

    private boolean isInSafeZone(Location location, double safeRadiusSquared) {
        if (safeRadiusSquared <= 0.0) {
            return false;
        }
        for (Location center : getSafeZoneCenters()) {
            if (!center.getWorld().equals(location.getWorld())) {
                continue;
            }
            double dx = location.getX() - center.getX();
            double dz = location.getZ() - center.getZ();
            if ((dx * dx + dz * dz) <= safeRadiusSquared) {
                return true;
            }
        }
        return false;
    }

    private void spawnSafeZoneMarkers() {
        double radius = getSafeRadius();
        if (radius <= 0.0) {
            return;
        }
        World world = boss.getWorld();
        int points = 16;
        for (Location safeCenter : getSafeZoneCenters()) {
            if (!safeCenter.getWorld().equals(world)) {
                continue;
            }
            Location ringCenter = safeCenter.clone().add(0.5, 0.1, 0.5);
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0 / points) * i;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location point = ringCenter.clone().add(x, 0.0, z);
                world.spawnParticle(Particle.END_ROD, point, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }
}
