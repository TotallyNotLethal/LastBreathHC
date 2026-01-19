package com.lastbreath.hc.lastBreathHC.environment;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class EnvironmentalEffectsManager implements Listener {

    private static final Set<PotionEffectType> DECAY_POTIONS = EnumSet.of(
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOW,
            PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.DARKNESS,
            PotionEffectType.NAUSEA
    );

    private final LastBreathHC plugin;
    private final BukkitTask task;
    private final long intervalTicks;
    private final int spawnRadiusBlocks;
    private final double minDistanceScale;
    private final double maxDistanceScale;
    private final double swampPoisonChance;
    private final int swampPoisonDurationTicks;
    private final int swampPoisonAmplifier;
    private final int desertIdleThresholdTicks;
    private final int desertHeatTicks;
    private final double desertHeatChance;
    private final double mountainKnockbackMultiplier;
    private final double deepOceanSwimSlowMultiplier;
    private final double deepOceanDrownedDamageMultiplier;
    private final UUID deepOceanSwimSlowId = UUID.fromString("a14d4b19-7491-43e7-8b85-85279b6648ea");
    private final Set<String> swampBiomes;
    private final Set<String> desertBiomes;
    private final Set<String> mountainBiomes;
    private final Set<String> deepOceanBiomes;
    private final int moveCooldownTicks;

    public EnvironmentalEffectsManager(LastBreathHC plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("environment");
        if (config == null) {
            config = plugin.getConfig().createSection("environment");
        }

        ConfigurationSection distanceSection = config.getConfigurationSection("distanceScaling");
        if (distanceSection == null) {
            distanceSection = config.createSection("distanceScaling");
        }

        intervalTicks = distanceSection.getLong("checkIntervalTicks", 80L);
        spawnRadiusBlocks = distanceSection.getInt("spawnRadiusBlocks", 6000);
        minDistanceScale = distanceSection.getDouble("minScale", 0.8);
        maxDistanceScale = distanceSection.getDouble("maxScale", 1.4);

        ConfigurationSection biomeSection = config.getConfigurationSection("biomes");
        if (biomeSection == null) {
            biomeSection = config.createSection("biomes");
        }

        ConfigurationSection swampSection = biomeSection.getConfigurationSection("swamp");
        if (swampSection == null) {
            swampSection = biomeSection.createSection("swamp");
        }
        swampPoisonChance = swampSection.getDouble("poisonChance", 0.08);
        swampPoisonDurationTicks = swampSection.getInt("poisonDurationTicks", 100);
        swampPoisonAmplifier = swampSection.getInt("poisonAmplifier", 0);
        swampBiomes = parseBiomeList(swampSection.getStringList("biomes"),
                Set.of("SWAMP", "MANGROVE_SWAMP"));

        ConfigurationSection desertSection = biomeSection.getConfigurationSection("desert");
        if (desertSection == null) {
            desertSection = biomeSection.createSection("desert");
        }
        desertIdleThresholdTicks = desertSection.getInt("idleThresholdTicks", 160);
        desertHeatTicks = desertSection.getInt("heatTicks", 60);
        desertHeatChance = desertSection.getDouble("heatChance", 0.25);
        desertBiomes = parseBiomeList(desertSection.getStringList("biomes"),
                Set.of("DESERT", "BADLANDS", "ERODED_BADLANDS", "WOODED_BADLANDS"));

        ConfigurationSection mountainSection = biomeSection.getConfigurationSection("mountain");
        if (mountainSection == null) {
            mountainSection = biomeSection.createSection("mountain");
        }
        mountainKnockbackMultiplier = mountainSection.getDouble("knockbackMultiplier", 1.25);
        mountainBiomes = parseBiomeList(mountainSection.getStringList("biomes"),
                Set.of("WINDSWEPT_HILLS", "WINDSWEPT_GRAVELLY_HILLS", "WINDSWEPT_FOREST",
                        "JAGGED_PEAKS", "FROZEN_PEAKS", "STONY_PEAKS", "MEADOW", "SNOWY_SLOPES"));

        ConfigurationSection oceanSection = biomeSection.getConfigurationSection("deepOcean");
        if (oceanSection == null) {
            oceanSection = biomeSection.createSection("deepOcean");
        }
        deepOceanSwimSlowMultiplier = oceanSection.getDouble("swimSlowMultiplier", 0.6);
        deepOceanDrownedDamageMultiplier = oceanSection.getDouble("drownedDamageMultiplier", 1.35);
        deepOceanBiomes = parseBiomeList(oceanSection.getStringList("biomes"),
                Set.of("DEEP_OCEAN", "DEEP_COLD_OCEAN", "DEEP_FROZEN_OCEAN", "DEEP_LUKEWARM_OCEAN"));

        moveCooldownTicks = config.getInt("moveCooldownTicks", 20);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickPlayers();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        int now = player.getTicksLived();
        if (player.hasMetadata("lb_last_move")) {
            int last = player.getMetadata("lb_last_move").getFirst().asInt();
            if (now - last < moveCooldownTicks) {
                return;
            }
        }
        player.setMetadata("lb_last_move", new org.bukkit.metadata.FixedMetadataValue(plugin, now));
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        double scale = distanceScale(event.getEntity().getLocation());
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player)) {
                continue;
            }
            for (PotionEffect effect : event.getPotion().getEffects()) {
                if (!DECAY_POTIONS.contains(effect.getType())) {
                    continue;
                }
                PotionEffect scaled = new PotionEffect(effect.getType(),
                        scaleInitialDuration(effect.getDuration(), scale),
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon());
                entity.removePotionEffect(effect.getType());
                entity.addPotionEffect(scaled);
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Drowned drowned) {
            double scale = distanceScale(drowned.getLocation());
            if (isBiome(drowned.getLocation(), deepOceanBiomes)) {
                scale *= deepOceanDrownedDamageMultiplier;
            }
            AttributeInstance attack = drowned.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(attack.getBaseValue() * scale);
            }
            AttributeInstance health = drowned.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(health.getBaseValue() * scale);
                drowned.setHealth(health.getBaseValue());
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        LivingEntity damager = null;
        if (event.getDamager() instanceof LivingEntity living) {
            damager = living;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof LivingEntity shooter) {
            damager = shooter;
        }
        if (damager == null) {
            return;
        }
        if (isBiome(damager.getLocation(), mountainBiomes)) {
            event.setKnockback(event.getKnockback() * mountainKnockbackMultiplier);
        }
    }

    private void tickPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.isDead()) {
                continue;
            }
            Location location = player.getLocation();
            double scale = distanceScale(location);
            applyFireScaling(player, scale);
            applyPotionDecay(player, scale);
            applyBiomeEffects(player, location);
        }
    }

    private void applyFireScaling(Player player, double scale) {
        int fireTicks = player.getFireTicks();
        if (fireTicks <= 0) {
            return;
        }
        int adjusted = adjustDurationForInterval(fireTicks, scale);
        if (adjusted != fireTicks) {
            player.setFireTicks(adjusted);
        }
    }

    private void applyPotionDecay(Player player, double scale) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!DECAY_POTIONS.contains(effect.getType())) {
                continue;
            }
            int adjustedDuration = adjustDurationForInterval(effect.getDuration(), scale);
            if (adjustedDuration != effect.getDuration()) {
                PotionEffect adjusted = new PotionEffect(effect.getType(),
                        adjustedDuration,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon());
                player.addPotionEffect(adjusted, true);
            }
        }
    }

    private void applyBiomeEffects(Player player, Location location) {
        String biomeKey = location.getBlock().getBiome().name();
        if (swampBiomes.contains(biomeKey)) {
            if (plugin.getServer().getRandom().nextDouble() <= swampPoisonChance) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON,
                        swampPoisonDurationTicks,
                        swampPoisonAmplifier,
                        true,
                        true));
                player.spawnParticle(Particle.SPORE_BLOSSOM_AIR, location, 8, 0.6, 0.6, 0.6, 0.01);
            }
        }
        boolean isDeepOcean = deepOceanBiomes.contains(biomeKey);
        if (desertBiomes.contains(biomeKey)) {
            if (isIdle(player) && plugin.getServer().getRandom().nextDouble() <= desertHeatChance) {
                player.setFireTicks(Math.max(player.getFireTicks(), desertHeatTicks));
                player.spawnParticle(Particle.ASH, location, 6, 0.4, 0.4, 0.4, 0.01);
            }
        }
        if (isDeepOcean) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
                    (int) intervalTicks * 2,
                    0,
                    true,
                    false,
                    false));
        }
        updateSwimSlow(player, isDeepOcean);
    }

    private void updateSwimSlow(Player player, boolean inDeepOcean) {
        AttributeInstance movement = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        AttributeModifier existing = movement.getModifier(deepOceanSwimSlowId);
        if (existing != null) {
            movement.removeModifier(existing);
        }
        if (inDeepOcean && player.isSwimming() && deepOceanSwimSlowMultiplier < 1.0) {
            double amount = deepOceanSwimSlowMultiplier - 1.0;
            movement.addModifier(new AttributeModifier(deepOceanSwimSlowId,
                    "lb-deep-ocean-swim-slow",
                    amount,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    private boolean isIdle(Player player) {
        if (!player.hasMetadata("lb_last_move")) {
            return true;
        }
        int lastMove = player.getMetadata("lb_last_move").getFirst().asInt();
        return (player.getTicksLived() - lastMove) >= desertIdleThresholdTicks;
    }

    private double distanceScale(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return 1.0;
        }
        Location spawn = world.getSpawnLocation();
        double distance = location.distance(spawn);
        if (spawnRadiusBlocks <= 0) {
            return maxDistanceScale;
        }
        double clamped = Math.min(distance, spawnRadiusBlocks);
        double t = clamped / spawnRadiusBlocks;
        return minDistanceScale + (maxDistanceScale - minDistanceScale) * t;
    }

    private int scaleInitialDuration(int duration, double scale) {
        return Math.max(1, (int) Math.round(duration * scale));
    }

    private int adjustDurationForInterval(int duration, double scale) {
        if (scale == 1.0) {
            return duration;
        }
        double delta = (scale - 1.0) * intervalTicks;
        return Math.max(1, (int) Math.round(duration + delta));
    }

    private boolean isBiome(Location location, Set<String> biomes) {
        return biomes.contains(location.getBlock().getBiome().name());
    }

    private Set<String> parseBiomeList(java.util.List<String> values, Set<String> defaults) {
        if (values == null || values.isEmpty()) {
            return defaults;
        }
        Set<String> result = new java.util.HashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            result.add(value.trim().toUpperCase(Locale.ROOT));
        }
        return result.isEmpty() ? defaults : result;
    }
}
