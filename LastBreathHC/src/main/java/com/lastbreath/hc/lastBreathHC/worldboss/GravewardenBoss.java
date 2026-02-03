package com.lastbreath.hc.lastBreathHC.worldboss;

import com.lastbreath.hc.lastBreathHC.items.CustomEnchant;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantments;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
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
    private static final String PHASE_REVENANT = "revenant";
    private static final Material GRAVESTONE_MATERIAL = Material.CHISELED_DEEPSLATE;

    private final Set<Location> gravestones = new HashSet<>();
    private int archerCooldownTicks;
    private int wraithCooldownTicks;

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
        if (shielded) {
            setPhase(PHASE_SHIELDED);
        } else {
            String phase = getPhase(PHASE_UNSEALED);
            if (PHASE_REVENANT.equals(phase)) {
                setPhase(PHASE_REVENANT);
            } else {
                setPhase(PHASE_UNSEALED);
            }
        }
        boss.setInvulnerable(shielded);
    }

    @Override
    public void tick() {
        if (!boss.isValid()) {
            return;
        }
        gravestones.removeIf(location -> location.getBlock().getType() != GRAVESTONE_MATERIAL);
        if (!gravestones.isEmpty()) {
            spawnGravestoneSafeRings();
        }
        if (PHASE_SHIELDED.equals(getPhase(PHASE_SHIELDED))) {
            if (gravestones.isEmpty()) {
                transitionToUnsealed();
            } else {
                pulseShield();
            }
            return;
        }
        if (PHASE_UNSEALED.equals(getPhase(PHASE_SHIELDED)) && shouldEnterRevenant()) {
            transitionToRevenant();
        }
        archerCooldownTicks = Math.max(0, archerCooldownTicks - 20);
        wraithCooldownTicks = Math.max(0, wraithCooldownTicks - 20);
        if (archerCooldownTicks <= 0) {
            telegraphArchers();
            spawnArchers();
            archerCooldownTicks = getArcherCooldownTicks();
        }
        if (PHASE_REVENANT.equals(getPhase(PHASE_SHIELDED)) && wraithCooldownTicks <= 0) {
            triggerWraithStrike();
            wraithCooldownTicks = getWraithCooldownTicks();
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
        if (PHASE_SHIELDED.equals(getPhase(PHASE_SHIELDED))) {
            return;
        }
        double healAmount = event.getFinalDamage() * 0.10;
        if (event.getEntity() instanceof Player player) {
            int pieces = Math.min(
                    CustomEnchantments.countArmorPiecesWithEnchant(player, CustomEnchant.LIFESTEAL_WARD.getId()),
                    4
            );
            double reduction = Math.min(1.0, pieces * 0.25);
            healAmount *= (1.0 - reduction);
        }
        double maxHealth = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                : boss.getHealth();
        boss.setHealth(Math.min(maxHealth, boss.getHealth() + healAmount));
        if (event.getEntity() instanceof Player player) {
            int witherAmplifier = PHASE_REVENANT.equals(getPhase(PHASE_SHIELDED)) ? 1 : 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, witherAmplifier));
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
    public boolean isBreakableMechanicBlock(Block block) {
        if (block.getType() != GRAVESTONE_MATERIAL) {
            return false;
        }
        return gravestones.contains(block.getLocation());
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
        WorldBorder border = world.getWorldBorder();
        Location borderCenter = border.getCenter();
        double maxRadius = Math.max(2.0, (border.getSize() / 2.0) - 2.0);
        int count = Math.max(1, getGravestoneCount());
        double radius = 6.0;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 / count) * i;
            Location location = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            double dx = location.getX() - borderCenter.getX();
            double dz = location.getZ() - borderCenter.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance > maxRadius) {
                double scale = maxRadius / distance;
                location.setX(borderCenter.getX() + dx * scale);
                location.setZ(borderCenter.getZ() + dz * scale);
            }
            Location base = location.clone();
            int floorY = findFloorY(world, location.getBlockX(), location.getBlockZ());
            base.setY(floorY);
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

    private void transitionToRevenant() {
        setPhase(PHASE_REVENANT);
        World world = boss.getWorld();
        world.playSound(boss.getLocation(), Sound.ENTITY_WARDEN_AGITATED, 1.4f, 0.6f);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, boss.getLocation(), 120, 2.5, 1.2, 2.5, 0.02);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
    }

    private void pulseShield() {
        World world = boss.getWorld();
        world.spawnParticle(Particle.CRIT, boss.getLocation(), 12, 1.2, 0.8, 1.2, 0.1);
        world.playSound(boss.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.2f);
    }

    private void telegraphArchers() {
        World world = boss.getWorld();
        for (Player player : world.getPlayers()) {
            if (isTelegraphBlocked(player)) {
                continue;
            }
            player.spawnParticle(Particle.SQUID_INK, boss.getLocation(), 30, 1.2, 0.6, 1.2, 0.05);
            player.playSound(boss.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, 1.0f, 0.9f);
        }
    }

    private void spawnArchers() {
        World world = boss.getWorld();
        Location center = boss.getLocation();
        double safeRadiusSquared = getGravewardenSafeRadius();
        safeRadiusSquared *= safeRadiusSquared;
        int count = Math.max(1, getArcherCount());
        if (PHASE_REVENANT.equals(getPhase(PHASE_SHIELDED))) {
            count += 1;
        }
        for (int i = 0; i < count; i++) {
            Location spawn = center.clone().add(randomOffset(), 0, randomOffset());
            spawn.setY(world.getHighestBlockYAt(spawn));
            Skeleton skeleton = world.spawn(spawn, Skeleton.class);
            skeleton.addScoreboardTag(WorldBossConstants.WORLD_BOSS_MINION_TAG);
            skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
            Player target = findNearestPlayerOutsideSafeZone(safeRadiusSquared);
            if (target != null) {
                skeleton.setTarget(target);
            }
        }
        world.playSound(center, Sound.ENTITY_SKELETON_AMBIENT, 0.9f, 0.8f);
    }

    private void triggerWraithStrike() {
        World world = boss.getWorld();
        Player target = findNearestPlayer();
        if (target == null) {
            return;
        }
        Location strikeLocation = target.getLocation();
        double radius = Math.max(1.0, getWraithStrikeRadius());
        double radiusSquared = radius * radius;
        world.spawnParticle(Particle.SOUL, strikeLocation, 40, 1.0, 0.5, 1.0, 0.05);
        world.playSound(strikeLocation, Sound.ENTITY_GHAST_SCREAM, 1.1f, 0.6f);
        for (Player player : world.getPlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(strikeLocation) <= radiusSquared) {
                player.damage(getWraithStrikeDamage(), boss);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0));
            }
        }
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

    private Player findNearestPlayerOutsideSafeZone(double safeRadiusSquared) {
        if (safeRadiusSquared <= 0.0) {
            return findNearestPlayer();
        }
        Player nearest = null;
        double closest = Double.MAX_VALUE;
        for (Player player : boss.getWorld().getPlayers()) {
            if (isPlayerInGravestoneSafeZone(player.getLocation(), safeRadiusSquared)) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(boss.getLocation());
            if (distance < closest) {
                closest = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private boolean isPlayerInGravestoneSafeZone(Location playerLocation, double safeRadiusSquared) {
        if (gravestones.isEmpty()) {
            return false;
        }
        for (Location gravestone : gravestones) {
            if (!gravestone.getWorld().equals(playerLocation.getWorld())) {
                continue;
            }
            if (playerLocation.distanceSquared(gravestone) <= safeRadiusSquared) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldEnterRevenant() {
        double maxHealth = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                : boss.getHealth();
        double threshold = Math.max(0.0, Math.min(1.0, getRevenantThreshold()));
        return maxHealth > 0 && (boss.getHealth() / maxHealth) <= threshold;
    }

    private double getGravewardenSafeRadius() {
        return Math.max(0.0, plugin.getConfig().getDouble("worldBoss.bosses.Gravewarden.safeRadius", 3.0));
    }

    private int getGravestoneCount() {
        return plugin.getConfig().getInt("worldBoss.bosses.Gravewarden.gravestoneCount", 6);
    }

    private int getArcherCooldownTicks() {
        int base = plugin.getConfig().getInt("worldBoss.bosses.Gravewarden.archerCooldownTicks", 160);
        if (PHASE_REVENANT.equals(getPhase(PHASE_SHIELDED))) {
            return Math.max(60, (int) Math.round(base * 0.75));
        }
        return base;
    }

    private int getArcherCount() {
        return plugin.getConfig().getInt("worldBoss.bosses.Gravewarden.archerCount", 3);
    }

    private double getRevenantThreshold() {
        return plugin.getConfig().getDouble("worldBoss.bosses.Gravewarden.revenantThreshold", 0.35);
    }

    private int getWraithCooldownTicks() {
        return plugin.getConfig().getInt("worldBoss.bosses.Gravewarden.wraithCooldownTicks", 140);
    }

    private double getWraithStrikeDamage() {
        return Math.max(0.0, plugin.getConfig().getDouble("worldBoss.bosses.Gravewarden.wraithStrikeDamage", 5.0));
    }

    private double getWraithStrikeRadius() {
        return Math.max(0.0, plugin.getConfig().getDouble("worldBoss.bosses.Gravewarden.wraithStrikeRadius", 3.5));
    }

    private void spawnGravestoneSafeRings() {
        double radius = getGravewardenSafeRadius();
        if (radius <= 0.0) {
            return;
        }
        World world = boss.getWorld();
        int points = 16;
        for (Location gravestone : gravestones) {
            if (!gravestone.getWorld().equals(world)) {
                continue;
            }
            Location center = gravestone.clone().add(0.5, 0.1, 0.5);
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0 / points) * i;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location point = center.clone().add(x, 0.0, z);
                world.spawnParticle(Particle.SOUL, point, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }
}
