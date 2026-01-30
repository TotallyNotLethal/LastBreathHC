package com.lastbreath.hc.lastBreathHC.worldboss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class GravewardenBoss extends BaseWorldBossController {

    private static final String PHASE_SHIELDED = "shielded";
    private static final String PHASE_UNSEALED = "unsealed";
    private static final Material GRAVESTONE_MATERIAL = Material.CHISELED_DEEPSLATE;

    private final Set<Location> gravestones = new HashSet<>();
    private int archerCooldownTicks;

    public GravewardenBoss(Plugin plugin, LivingEntity boss) {
        super(plugin, boss, "world_boss_gravewarden_phase", "world_boss_gravewarden_stones", "world_boss_gravewarden_data");
    }

    @Override
    public WorldBossType getType() {
        return WorldBossType.GRAVEWARDEN;
    }

    @Override
    public void rebuildFromPersistent() {
        gravestones.clear();
        gravestones.addAll(loadBlockLocations());
        if (gravestones.isEmpty()) {
            gravestones.addAll(spawnGravestones());
        }
        storeBlockLocations(gravestones);
        boolean shielded = !gravestones.isEmpty();
        setPhase(shielded ? PHASE_SHIELDED : PHASE_UNSEALED);
        boss.setInvulnerable(shielded);
    }

    @Override
    public void tick() {
        if (!boss.isValid()) {
            return;
        }
        gravestones.removeIf(location -> location.getBlock().getType() != GRAVESTONE_MATERIAL);
        if (PHASE_SHIELDED.equals(getPhase(PHASE_SHIELDED))) {
            if (gravestones.isEmpty()) {
                transitionToUnsealed();
            } else {
                pulseShield();
            }
            return;
        }
        archerCooldownTicks = Math.max(0, archerCooldownTicks - 20);
        if (archerCooldownTicks <= 0) {
            telegraphArchers();
            spawnArchers();
            archerCooldownTicks = 200;
        }
    }

    @Override
    public void handleBossDamaged(EntityDamageByEntityEvent event) {
        if (PHASE_SHIELDED.equals(getPhase(PHASE_SHIELDED))) {
            event.setCancelled(true);
            boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        }
    }

    @Override
    public void handleBossAttack(EntityDamageByEntityEvent event) {
        if (!PHASE_UNSEALED.equals(getPhase(PHASE_SHIELDED))) {
            return;
        }
        double healAmount = event.getFinalDamage() * 0.35;
        double maxHealth = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                : boss.getHealth();
        boss.setHealth(Math.min(maxHealth, boss.getHealth() + healAmount));
        if (event.getEntity() instanceof Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
        }
    }

    @Override
    public void handleBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != GRAVESTONE_MATERIAL) {
            return;
        }
        Location location = event.getBlock().getLocation();
        if (gravestones.remove(location)) {
            storeBlockLocations(gravestones);
            boss.getWorld().playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.2f);
            if (gravestones.isEmpty()) {
                transitionToUnsealed();
            }
        }
    }

    @Override
    public void cleanup() {
        for (Location location : gravestones) {
            if (location.getBlock().getType() == GRAVESTONE_MATERIAL) {
                location.getBlock().setType(Material.AIR);
            }
        }
        gravestones.clear();
    }

    @Override
    public void onArenaEmpty() {
        boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        if (!PHASE_SHIELDED.equals(getPhase(PHASE_SHIELDED))) {
            gravestones.clear();
            gravestones.addAll(spawnGravestones());
            storeBlockLocations(gravestones);
            setPhase(PHASE_SHIELDED);
            boss.setInvulnerable(true);
        }
    }

    private Set<Location> spawnGravestones() {
        Set<Location> locations = new HashSet<>();
        Location center = boss.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return locations;
        }
        int count = 5;
        double radius = 6.0;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 / count) * i;
            Location location = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Location base = location.clone();
            base.setY(world.getHighestBlockYAt(location));
            Location place = base.clone().add(0, 1, 0);
            if (!place.getBlock().getType().isAir()) {
                continue;
            }
            place.getBlock().setType(GRAVESTONE_MATERIAL);
            locations.add(place.getBlock().getLocation());
        }
        return locations;
    }

    private void transitionToUnsealed() {
        setPhase(PHASE_UNSEALED);
        boss.setInvulnerable(false);
        World world = boss.getWorld();
        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.6f);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, boss.getLocation(), 80, 2.0, 1.0, 2.0, 0.02);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0));
    }

    private void pulseShield() {
        World world = boss.getWorld();
        world.spawnParticle(Particle.CRIT, boss.getLocation(), 12, 1.2, 0.8, 1.2, 0.1);
        world.playSound(boss.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.2f);
    }

    private void telegraphArchers() {
        World world = boss.getWorld();
        world.spawnParticle(Particle.SQUID_INK, boss.getLocation(), 30, 1.2, 0.6, 1.2, 0.05);
        world.playSound(boss.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, 1.0f, 0.9f);
    }

    private void spawnArchers() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        for (int i = 0; i < 2; i++) {
            Location spawn = center.clone().add(randomOffset(), 0, randomOffset());
            spawn.setY(world.getHighestBlockYAt(spawn));
            Skeleton skeleton = world.spawn(spawn, Skeleton.class);
            skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
            skeleton.setTarget(findNearestPlayer());
        }
        world.playSound(center, Sound.ENTITY_SKELETON_AMBIENT, 0.9f, 0.8f);
    }

    private double randomOffset() {
        return (Math.random() - 0.5) * 8.0;
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
}
