package com.lastbreath.hc.lastBreathHC.asteroid;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.Chunk;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class AsteroidManager {
    private static final double ASTEROID_MAX_HEALTH = 1024.0;
    private static final int ASTEROID_MAX_DISTANCE = 50000;
    private static final int ASTEROID_EDGE_VARIANCE = 64;

    public static final Map<Location, AsteroidEntry> ASTEROIDS = new HashMap<>();
    public static final String ASTEROID_MOB_TAG = "asteroid_mob";
    public static final String ASTEROID_AGGRESSIVE_TAG = "asteroid_aggressive";
    public static final String ASTEROID_KEY_TAG_PREFIX = "asteroid_key:";
    public static final String ASTEROID_SCALE_TAG_PREFIX = "asteroid_scale:";
    public static final String ASTEROID_TIER_TAG_PREFIX = "asteroid_tier:";
    public static final String ASTEROID_MARKER_TAG = "asteroid_marker";
    private static final Set<String> PERSISTED_ASTEROIDS = new HashSet<>();
    private static File dataFile;
    private static JavaPlugin plugin;
    private static BukkitTask mobLeashTask;
    private static BukkitTask asteroidSpawnTask;
    private static final Deque<AsteroidSpawnSequence> ASTEROID_SPAWN_QUEUE = new ArrayDeque<>();

    public static class AsteroidEntry {
        private final Inventory inventory;
        private final int tier;
        private final Set<UUID> mobs;
        private UUID markerId;

        private AsteroidEntry(Inventory inventory, int tier) {
            this.inventory = inventory;
            this.tier = tier;
            this.mobs = new HashSet<>();
        }

        public Inventory inventory() {
            return inventory;
        }

        public int tier() {
            return tier;
        }

        public Set<UUID> mobs() {
            return mobs;
        }

        public UUID markerId() {
            return markerId;
        }

        public void markerId(UUID markerId) {
            this.markerId = markerId;
        }
    }

    public static boolean isAsteroid(Location loc) {
        return ASTEROIDS.containsKey(blockLocation(loc));
    }

    public static Inventory getInventory(Location loc) {
        AsteroidEntry entry = ASTEROIDS.get(blockLocation(loc));
        return entry == null ? null : entry.inventory();
    }

    public static AsteroidEntry getEntry(Location loc) {
        return ASTEROIDS.get(blockLocation(loc));
    }

    public static AsteroidEntry getEntryByKey(String asteroidKey) {
        if (asteroidKey == null || asteroidKey.isBlank()) {
            return null;
        }
        for (Map.Entry<Location, AsteroidEntry> entry : ASTEROIDS.entrySet()) {
            if (asteroidKey.equals(asteroidKey(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static String asteroidKey(Location loc) {
        return toKey(blockLocation(loc));
    }

    public static void remove(Location loc) {
        Location blockLoc = blockLocation(loc);
        AsteroidEntry entry = ASTEROIDS.remove(blockLoc);
        if (entry != null) {
            PERSISTED_ASTEROIDS.remove(toKey(blockLoc, entry.tier()));
        } else {
            PERSISTED_ASTEROIDS.remove(toKey(blockLoc));
        }
        saveAsteroids();
        removeAsteroidMobs(blockLoc, entry);
        removeAsteroidMarker(blockLoc, entry);
        blockLoc.getBlock().setType(Material.AIR);
    }

    public static void registerAsteroid(Location loc, int tier) {
        Location blockLoc = blockLocation(loc);
        ASTEROIDS.put(blockLoc, new AsteroidEntry(AsteroidLoot.createLoot(tier), tier));
        PERSISTED_ASTEROIDS.add(toKey(blockLoc, tier));
        saveAsteroids();
        ensureAsteroidMarker(blockLoc);
    }

    public static void initialize(JavaPlugin plugin) {
        AsteroidManager.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "asteroids.yml");
        cleanupPersistedAsteroids();
        startMobLeashTask();
    }

    public static void clearAllAsteroids() {
        for (Location location : ASTEROIDS.keySet().toArray(new Location[0])) {
            location.getBlock().setType(Material.AIR);
            removeAsteroidMarker(location, ASTEROIDS.get(location));
        }
        ASTEROIDS.clear();
        PERSISTED_ASTEROIDS.clear();
        saveAsteroids();
    }

    public static int purgeAsteroidMobsFromMemory() {
        int removed = 0;
        for (AsteroidEntry entry : ASTEROIDS.values()) {
            entry.mobs().clear();
        }

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
                if (entity.getScoreboardTags().contains(ASTEROID_MOB_TAG)) {
                    entity.remove();
                    removed++;
                }
            }
        }

        int leashRadius = getMobLeashRadius();
        int chunkRadius = Math.max(1, (int) Math.ceil(leashRadius / 16.0));
        for (Location asteroidLoc : ASTEROIDS.keySet()) {
            World world = asteroidLoc.getWorld();
            if (world == null) {
                continue;
            }
            int baseChunkX = asteroidLoc.getBlockX() >> 4;
            int baseChunkZ = asteroidLoc.getBlockZ() >> 4;
            for (int x = baseChunkX - chunkRadius; x <= baseChunkX + chunkRadius; x++) {
                for (int z = baseChunkZ - chunkRadius; z <= baseChunkZ + chunkRadius; z++) {
                    Chunk chunk = world.getChunkAt(x, z);
                    if (!chunk.isLoaded()) {
                        chunk.load();
                    }
                    for (Entity entity : chunk.getEntities()) {
                        if (entity instanceof LivingEntity living && living.getScoreboardTags().contains(ASTEROID_MOB_TAG)) {
                            living.remove();
                            removed++;
                        }
                    }
                }
            }
        }

        return removed;
    }

    private static void cleanupPersistedAsteroids() {
        if (dataFile == null || !dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        List<String> entries = config.getStringList("asteroids");
        for (String entry : entries) {
            Location location = fromKey(entry);
            if (location == null) {
                continue;
            }
            removeTaggedMobsForKey(location.getWorld(), ASTEROID_KEY_TAG_PREFIX + asteroidKey(location));
            location.getBlock().setType(Material.AIR);
        }

        config.set("asteroids", List.of());
        try {
            config.save(dataFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to clear asteroid data file: " + e.getMessage());
        }
    }

    private static void saveAsteroids() {
        if (dataFile == null) {
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("asteroids", List.copyOf(PERSISTED_ASTEROIDS));
        try {
            config.save(dataFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save asteroid data: " + e.getMessage());
        }
    }

    private static Location blockLocation(Location loc) {
        return loc.getBlock().getLocation();
    }

    private static void removeAsteroidMobs(Location blockLoc, AsteroidEntry entry) {
        World world = blockLoc.getWorld();
        if (world == null) {
            return;
        }
        if (entry != null) {
            entry.mobs().removeIf(mobId -> {
                org.bukkit.entity.Entity entity = world.getEntity(mobId);
                if (!(entity instanceof LivingEntity mob)) {
                    return true;
                }
                mob.remove();
                return true;
            });
        }
        removeTaggedMobsForKey(world, ASTEROID_KEY_TAG_PREFIX + asteroidKey(blockLoc));
    }

    private static void removeTaggedMobsForKey(World world, String keyTag) {
        if (world == null || keyTag == null || keyTag.isBlank()) {
            return;
        }
        for (LivingEntity mob : world.getEntitiesByClass(LivingEntity.class)) {
            Set<String> tags = mob.getScoreboardTags();
            if (tags.contains(ASTEROID_MOB_TAG) && tags.contains(keyTag)) {
                mob.remove();
            }
        }
    }

    private static void ensureAsteroidMarker(Location blockLoc) {
        World world = blockLoc.getWorld();
        if (world == null) {
            return;
        }
        AsteroidEntry entry = ASTEROIDS.get(blockLoc);
        if (entry == null) {
            return;
        }
        ArmorStand existingMarker = findAsteroidMarker(blockLoc, entry);
        if (existingMarker != null) {
            entry.markerId(existingMarker.getUniqueId());
            return;
        }
        Location markerLoc = blockLoc.clone().add(0.5, -1.0, 0.5);
        ArmorStand marker = world.spawn(markerLoc, ArmorStand.class);
        marker.setInvisible(true);
        marker.setInvulnerable(true);
        marker.setGravity(false);
        marker.setMarker(true);
        marker.setPersistent(true);
        marker.setCanPickupItems(false);
        marker.setSilent(true);
        marker.addScoreboardTag(ASTEROID_MARKER_TAG);
        marker.addScoreboardTag(ASTEROID_KEY_TAG_PREFIX + asteroidKey(blockLoc));
        marker.setGlowing(true);
        EntityEquipment equipment = marker.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(new ItemStack(Material.ANCIENT_DEBRIS));
        }
        entry.markerId(marker.getUniqueId());
    }

    private static void removeAsteroidMarker(Location blockLoc, AsteroidEntry entry) {
        World world = blockLoc.getWorld();
        if (world == null) {
            return;
        }
        ArmorStand marker = findAsteroidMarker(blockLoc, entry);
        if (marker == null) {
            if (entry != null) {
                entry.markerId(null);
            }
            return;
        }
        EntityEquipment equipment = marker.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(null);
        }
        marker.setGlowing(false);
        marker.remove();
        if (entry != null) {
            entry.markerId(null);
        }
    }

    private static ArmorStand findAsteroidMarker(Location blockLoc, AsteroidEntry entry) {
        World world = blockLoc.getWorld();
        if (world == null) {
            return null;
        }
        String keyTag = ASTEROID_KEY_TAG_PREFIX + asteroidKey(blockLoc);
        if (entry != null && entry.markerId() != null) {
            Entity entity = world.getEntity(entry.markerId());
            if (entity instanceof ArmorStand stand) {
                Set<String> tags = stand.getScoreboardTags();
                if (tags.contains(ASTEROID_MARKER_TAG) && tags.contains(keyTag)) {
                    return stand;
                }
            }
            entry.markerId(null);
        }
        Location markerLoc = blockLoc.clone().add(0.5, -1.0, 0.5);
        for (Entity entity : world.getNearbyEntities(markerLoc, 0.6, 0.8, 0.6)) {
            if (entity instanceof ArmorStand stand
                    && stand.getScoreboardTags().contains(ASTEROID_MARKER_TAG)
                    && stand.getScoreboardTags().contains(keyTag)) {
                return stand;
            }
        }
        return null;
    }

    private static void syncAsteroidMarker(Location blockLoc) {
        World world = blockLoc.getWorld();
        if (world == null) {
            return;
        }
        Location center = blockLoc.clone().add(0.5, 0.5, 0.5);
        boolean playerNearby = !world.getNearbyPlayers(center, 3.0).isEmpty();
        if (playerNearby) {
            removeAsteroidMarker(blockLoc, ASTEROIDS.get(blockLoc));
            return;
        }
        ensureAsteroidMarker(blockLoc);
    }

    private static String toKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static String toKey(Location loc, int tier) {
        return toKey(loc) + ":" + tier;
    }

    private static Location fromKey(String entry) {
        if (entry == null || entry.isBlank()) {
            return null;
        }
        String[] parts = entry.split(":");
        if (parts.length < 2) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        String[] coords = parts[1].split(",");
        if (coords.length != 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            int z = Integer.parseInt(coords[2]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void spawnAsteroid(World world, Location loc) {
        Location center = clampAsteroidLocation(loc.getBlock().getLocation());
        int tier = determineTierByDistance(world, center);
        spawnAsteroid(world, center, tier);
    }

    public static void spawnAsteroid(World world, Location loc, int tier) {
        Location center = clampAsteroidLocation(loc.getBlock().getLocation());
        int resolvedTier = Math.max(1, Math.min(3, tier));
        enqueueAsteroidSpawn(new AsteroidSpawnSequence(world, center, resolvedTier));
    }

    private static Location clampAsteroidLocation(Location location) {
        if (location == null) {
            return null;
        }
        double x = clampCoordinateWithVariance(location.getX());
        double z = clampCoordinateWithVariance(location.getZ());
        if (x == location.getX() && z == location.getZ()) {
            return location;
        }
        return new Location(location.getWorld(), x, location.getY(), z);
    }

    public static int getMaxAsteroidDistance() {
        return ASTEROID_MAX_DISTANCE;
    }

    private static double clampCoordinateWithVariance(double value) {
        if (value > ASTEROID_MAX_DISTANCE) {
            return ASTEROID_MAX_DISTANCE - randomEdgeVariance();
        }
        if (value < -ASTEROID_MAX_DISTANCE) {
            return -ASTEROID_MAX_DISTANCE + randomEdgeVariance();
        }
        return value;
    }

    private static double randomEdgeVariance() {
        int maxVariance = Math.min(ASTEROID_EDGE_VARIANCE, ASTEROID_MAX_DISTANCE);
        if (maxVariance <= 0) {
            return 0.0;
        }
        return ThreadLocalRandom.current().nextInt(maxVariance + 1);
    }

    private static void enqueueAsteroidSpawn(AsteroidSpawnSequence sequence) {
        ASTEROID_SPAWN_QUEUE.add(sequence);
        startAsteroidSpawnTask();
    }

    private static void startAsteroidSpawnTask() {
        if (plugin == null || asteroidSpawnTask != null) {
            return;
        }
        asteroidSpawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                AsteroidSpawnSequence sequence = ASTEROID_SPAWN_QUEUE.peek();
                if (sequence == null) {
                    cancel();
                    asteroidSpawnTask = null;
                    return;
                }
                boolean complete = sequence.runNextStep();
                if (complete) {
                    ASTEROID_SPAWN_QUEUE.poll();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static final class AsteroidSpawnSequence {
        private final World world;
        private final Location center;
        private final int tier;
        private int step;

        private AsteroidSpawnSequence(World world, Location center, int tier) {
            this.world = world;
            this.center = center;
            this.tier = tier;
            this.step = 0;
        }

        private boolean runNextStep() {
            if (world == null) {
                return true;
            }
            switch (step) {
                case 0 -> playImpactEffects();
                case 1 -> placeCoreAndScorch();
                case 2 -> {
                    spawnTierEffects(world, center.clone().add(0, 1, 0), tier);
                    return true;
                }
                default -> {
                    return true;
                }
            }
            step++;
            return false;
        }

        private void playImpactEffects() {
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.8f);
            world.spawnParticle(Particle.EXPLOSION, center, 1);
        }

        private void placeCoreAndScorch() {
            int impactRadius = 2;
            if (plugin != null) {
                impactRadius = plugin.getConfig().getInt("asteroid.impact.radius", impactRadius);
            }
            impactRadius = Math.max(1, impactRadius);
            for (int x = -impactRadius; x <= impactRadius; x++) {
                for (int z = -impactRadius; z <= impactRadius; z++) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.6) {
                        Location ground = center.clone().add(x, 0, z);
                        if (ground.getBlock().getType().isSolid()) {
                            ground.getBlock().setType(Material.NETHERRACK);
                            clearBlockingAbove(ground, 1);
                            placeFireIfAir(ground.clone().add(0, 1, 0));
                        }
                    }
                }
            }

            Location coreGround = center.clone();
            if (!coreGround.getBlock().getType().isSolid()) {
                return;
            }
            Location core = findPassableAbove(coreGround, 3);
            clearBlockingAbove(coreGround, 2);
            core.getBlock().setType(Material.ANCIENT_DEBRIS);
            AsteroidManager.registerAsteroid(core, tier);

            Bukkit.broadcast(
                    Component.text("☄ An asteroid has crashed at ")
                            .color(NamedTextColor.GOLD)
                            .append(Component.text(
                                    core.getBlockX() + ", " +
                                            core.getBlockY() + ", " +
                                            core.getBlockZ(),
                                    NamedTextColor.YELLOW
                            ))
            );
        }

        private void clearBlockingAbove(Location ground, int height) {
            for (int y = 1; y <= height; y++) {
                Location above = ground.clone().add(0, y, 0);
                if (!above.getBlock().isPassable()) {
                    above.getBlock().setType(Material.AIR);
                }
            }
        }

        private Location findPassableAbove(Location ground, int height) {
            for (int y = 1; y <= height; y++) {
                Location above = ground.clone().add(0, y, 0);
                if (above.getBlock().isPassable()) {
                    return above;
                }
            }
            return ground.clone().add(0, 1, 0);
        }

        private void placeFireIfAir(Location location) {
            if (location.getBlock().getType() == Material.AIR) {
                location.getBlock().setType(Material.FIRE);
            }
        }
    }

    private static int determineTierByDistance(World world, Location center) {
        if (plugin == null) {
            return 1;
        }
        int tier2Min = plugin.getConfig().getInt("asteroid.tiers.tier2MinDistance", 10000);
        int tier3Min = plugin.getConfig().getInt("asteroid.tiers.tier3MinDistance", 100000);
        double distance = center.distance(world.getSpawnLocation());
        if (distance >= tier3Min) {
            return 3;
        }
        if (distance > tier2Min) {
            return 2;
        }
        return 1;
    }

    private static void applyAsteroidScale(LivingEntity livingEntity) {
        boolean hasScaleTag = livingEntity.getScoreboardTags().stream()
                .anyMatch(tag -> tag.startsWith(ASTEROID_SCALE_TAG_PREFIX));
        if (hasScaleTag) {
            return;
        }
        double scale = ThreadLocalRandom.current().nextDouble(1.1, 2.6);
        AttributeInstance scaleAttribute = livingEntity.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }
        AttributeInstance maxHealthAttribute = livingEntity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double newMax = maxHealthAttribute.getBaseValue() * scale * 1.15;
            double cappedMax = Math.min(newMax, ASTEROID_MAX_HEALTH);
            maxHealthAttribute.setBaseValue(cappedMax);
            livingEntity.setHealth(cappedMax);
        }
        AttributeInstance attackAttribute = livingEntity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            attackAttribute.setBaseValue(attackAttribute.getBaseValue() * scale * 1.12);
        }
        livingEntity.addScoreboardTag(ASTEROID_SCALE_TAG_PREFIX + scale);
    }

    private static void spawnTierEffects(World world, Location center, int tier) {
        if (plugin == null) {
            return;
        }
        String basePath = "asteroid.tiers.effects.tier" + tier;
        int mobCount = plugin.getConfig().getInt(basePath + ".mobCount", 0);
        List<String> mobTypes = plugin.getConfig().getStringList(basePath + ".mobTypes");
        if (mobTypes.isEmpty()) {
            String mobTypeName = plugin.getConfig().getString(basePath + ".mobType");
            if (mobTypeName != null && !mobTypeName.isBlank() && !mobTypeName.equalsIgnoreCase("NONE")) {
                mobTypes = List.of(mobTypeName);
            }
        }
        List<EntityType> resolvedTypes = mobTypes.stream()
                .filter(mobTypeName -> mobTypeName != null && !mobTypeName.isBlank() && !mobTypeName.equalsIgnoreCase("NONE"))
                .map(mobTypeName -> {
                    EntityType mobType = EntityType.fromName(mobTypeName.toLowerCase());
                    if (mobType == null) {
                        try {
                            mobType = EntityType.valueOf(mobTypeName.toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                            return null;
                        }
                    }
                    return mobType != null && mobType.isAlive() ? mobType : null;
                })
                .filter(mobType -> mobType != null)
                .toList();

        List<EntityType> bossTypes;
        if (tier == 3) {
            bossTypes = resolveBossTypes(basePath + ".bossTypes");
            if (!bossTypes.isEmpty()) {
                resolvedTypes = resolvedTypes.stream()
                        .filter(mobType -> !bossTypes.contains(mobType))
                        .toList();
            }
        } else {
            bossTypes = List.of();
        }

        List<PotionEffect> tierPotionEffects = resolveTierPotionEffects(basePath);
        if (!bossTypes.isEmpty()) {
            spawnTierBoss(world, center, bossTypes, tierPotionEffects, tier);
        }
        if (!resolvedTypes.isEmpty() && mobCount > 0) {
            scheduleMobSpawns(world, center, mobCount, resolvedTypes, tierPotionEffects, tier);
        }

        boolean aoeEnabled = plugin.getConfig().getBoolean(basePath + ".aoe.enabled", false);
        if (!aoeEnabled) {
            return;
        }

        String particleName = plugin.getConfig().getString(basePath + ".aoe.particle", "DRAGON_BREATH");
        double radius = plugin.getConfig().getDouble(basePath + ".aoe.radius", 3.0);
        int durationTicks = plugin.getConfig().getInt(basePath + ".aoe.durationTicks", 200);
        int waitTimeTicks = plugin.getConfig().getInt(basePath + ".aoe.waitTimeTicks", 0);

        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            particle = Particle.DRAGON_BREATH;
        }

        Location aoeLocation = findSpawnLocation(center);
        AreaEffectCloud cloud = (AreaEffectCloud) world.spawnEntity(aoeLocation, EntityType.AREA_EFFECT_CLOUD);
        cloud.setBasePotionType(PotionType.WATER);
        cloud.clearCustomEffects();
        setCloudParticle(cloud, particle);
        cloud.setRadius((float) radius);
        cloud.setDuration(durationTicks);
        cloud.setWaitTime(waitTimeTicks);
    }

    private static void setCloudParticle(AreaEffectCloud cloud, Particle particle) {
        Class<?> dataType = particle.getDataType();
        if (dataType == null || dataType == Void.class) {
            cloud.setParticle(particle);
            return;
        }
        if (dataType == Float.class || dataType == float.class) {
            cloud.setParticle(particle, 1.0f);
            return;
        }
        if (dataType == Particle.DustOptions.class) {
            cloud.setParticle(particle, new Particle.DustOptions(Color.fromRGB(255, 85, 85), 1.0f));
            return;
        }
        cloud.setParticle(Particle.DRAGON_BREATH);
    }

    private static List<PotionEffect> resolveTierPotionEffects(String basePath) {
        if (plugin == null) {
            return List.of();
        }
        List<String> effectEntries = plugin.getConfig().getStringList(basePath + ".potionEffects");
        if (effectEntries == null || effectEntries.isEmpty()) {
            return List.of();
        }
        List<PotionEffect> effects = new ArrayList<>();
        for (String entry : effectEntries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase());
            if (type == null) {
                continue;
            }
            int duration = 200;
            int amplifier = 0;
            if (parts.length > 1) {
                try {
                    duration = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    duration = 200;
                }
            }
            if (parts.length > 2) {
                try {
                    amplifier = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException ignored) {
                    amplifier = 0;
                }
            }
            if (duration <= 0) {
                continue;
            }
            effects.add(new PotionEffect(type, duration, amplifier));
        }
        return effects;
    }

    private static List<EntityType> resolveBossTypes(String configPath) {
        if (plugin == null) {
            return List.of();
        }
        List<String> mobTypes = plugin.getConfig().getStringList(configPath);
        if (mobTypes == null || mobTypes.isEmpty()) {
            return List.of();
        }
        return mobTypes.stream()
                .filter(mobTypeName -> mobTypeName != null && !mobTypeName.isBlank() && !mobTypeName.equalsIgnoreCase("NONE"))
                .map(mobTypeName -> {
                    EntityType mobType = EntityType.fromName(mobTypeName.toLowerCase());
                    if (mobType == null) {
                        try {
                            mobType = EntityType.valueOf(mobTypeName.toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                            return null;
                        }
                    }
                    return mobType != null && mobType.isAlive() ? mobType : null;
                })
                .filter(mobType -> mobType != null)
                .toList();
    }

    private static void applyTierPotionEffects(LivingEntity livingEntity, List<PotionEffect> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        for (PotionEffect effect : effects) {
            if (effect != null) {
                livingEntity.addPotionEffect(effect);
            }
        }
    }

    private static void spawnTierBoss(World world, Location center, List<EntityType> bossTypes,
                                      List<PotionEffect> tierPotionEffects, int tier) {
        if (bossTypes == null || bossTypes.isEmpty()) {
            return;
        }
        EntityType bossType = bossTypes.get(ThreadLocalRandom.current().nextInt(bossTypes.size()));
        Location spawnLocation = findSpawnLocation(center);
        if (!(world.spawnEntity(spawnLocation, bossType) instanceof LivingEntity livingEntity)) {
            return;
        }
        livingEntity.setRemoveWhenFarAway(false);
        String keyTag = ASTEROID_KEY_TAG_PREFIX + asteroidKey(center);
        String tierTag = ASTEROID_TIER_TAG_PREFIX + tier;
        livingEntity.addScoreboardTag(ASTEROID_MOB_TAG);
        livingEntity.addScoreboardTag(keyTag);
        livingEntity.addScoreboardTag(tierTag);
        applyAsteroidScale(livingEntity);
        applyAsteroidMobTuning(livingEntity);
        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 72000, 0, true, true, true));
        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 72000, 0, true, true, true));
        applyArmorProjectileProtection(livingEntity);
        applyTierPotionEffects(livingEntity, tierPotionEffects);
        AsteroidEntry entry = ASTEROIDS.get(blockLocation(center));
        if (entry != null) {
            entry.mobs().add(livingEntity.getUniqueId());
        }
    }
    private static void scheduleMobSpawns(World world, Location center, int mobCount, List<EntityType> resolvedTypes,
                                          List<PotionEffect> tierPotionEffects, int tier) {
        if (plugin == null) {
            return;
        }
        int maxIndex = resolvedTypes.size();
        AsteroidEntry entry = ASTEROIDS.get(blockLocation(center));
        String asteroidKey = asteroidKey(center);
        String keyTag = ASTEROID_KEY_TAG_PREFIX + asteroidKey;
        String tierTag = ASTEROID_TIER_TAG_PREFIX + tier;
        int batchSize = 3;
        new BukkitRunnable() {
            private int remaining = mobCount;

            @Override
            public void run() {
                if (remaining <= 0) {
                    cancel();
                    return;
                }
                int spawnNow = Math.min(batchSize, remaining);
                for (int i = 0; i < spawnNow; i++) {
                    EntityType mobType = resolvedTypes.get(ThreadLocalRandom.current().nextInt(maxIndex));
                    Location spawnLocation = findSpawnLocation(center);
                    if (world.spawnEntity(spawnLocation, mobType) instanceof LivingEntity livingEntity) {
                        livingEntity.setRemoveWhenFarAway(false);
                        livingEntity.addScoreboardTag(ASTEROID_MOB_TAG);
                        livingEntity.addScoreboardTag(keyTag);
                        livingEntity.addScoreboardTag(tierTag);
                        applyAsteroidScale(livingEntity);
                        applyAsteroidMobTuning(livingEntity);
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 72000, 0, true, true, true));
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 72000, 0, true, true, true));
                        applyArmorProjectileProtection(livingEntity);
                        applyTierPotionEffects(livingEntity, tierPotionEffects);
                        if (entry != null) {
                            entry.mobs().add(livingEntity.getUniqueId());
                        }
                    }
                }
                remaining -= spawnNow;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static void applyArmorProjectileProtection(LivingEntity livingEntity) {
        EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) {
            return;
        }
        applyProjectileProtectionIfRolled(equipment.getHelmet(), equipment::setHelmet);
        applyProjectileProtectionIfRolled(equipment.getChestplate(), equipment::setChestplate);
        applyProjectileProtectionIfRolled(equipment.getLeggings(), equipment::setLeggings);
        applyProjectileProtectionIfRolled(equipment.getBoots(), equipment::setBoots);
    }

    private static void applyProjectileProtectionIfRolled(ItemStack itemStack, Consumer<ItemStack> setter) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        if (ThreadLocalRandom.current().nextBoolean()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 4, true);
                itemStack.setItemMeta(meta);
                setter.accept(itemStack);
            }
        }
    }

    private static void applyAsteroidMobTuning(LivingEntity livingEntity) {
        if (livingEntity instanceof Mob mob) {
            mob.setAI(true);
            mob.setAware(true);
        }
        applyAttributeMultiplier(livingEntity, Attribute.MOVEMENT_SPEED, 1.3);
        applyFollowRangeTuning(livingEntity);
        applyAttributeMultiplierByName(livingEntity, "WATER_MOVEMENT_EFFICIENCY", 1.3);
    }

    private static void applyFollowRangeTuning(LivingEntity livingEntity) {
        AttributeInstance followRange = livingEntity.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange == null) {
            return;
        }

        EntityType entityType = livingEntity.getType();
        double multiplier = getFollowRangeMultiplier(entityType);
        double tunedValue = followRange.getBaseValue() * multiplier;
        double minimum = getFollowRangeMinimum(entityType);
        if (minimum > 0.0) {
            tunedValue = Math.max(tunedValue, minimum);
        }
        followRange.setBaseValue(tunedValue);
    }

    private static double getFollowRangeMultiplier(EntityType entityType) {
        if (plugin == null) {
            return 1.5;
        }

        String basePath = "asteroid.reactionRangeMultiplier";
        double multiplier = plugin.getConfig().getDouble(basePath + ".default", 1.5);
        if (entityType == EntityType.WITHER_SKELETON) {
            return plugin.getConfig().getDouble(basePath + ".witherSkeleton", Math.max(multiplier, 2.2));
        }
        if (entityType == EntityType.PIGLIN_BRUTE) {
            return plugin.getConfig().getDouble(basePath + ".piglinBrute", Math.max(multiplier, 2.0));
        }
        return multiplier;
    }

    private static double getFollowRangeMinimum(EntityType entityType) {
        if (plugin == null) {
            return 0.0;
        }

        String basePath = "asteroid.reactionRangeMinimum";
        if (entityType == EntityType.WITHER_SKELETON) {
            return plugin.getConfig().getDouble(basePath + ".witherSkeleton", 48.0);
        }
        if (entityType == EntityType.PIGLIN_BRUTE) {
            return plugin.getConfig().getDouble(basePath + ".piglinBrute", 40.0);
        }
        return 0.0;
    }

    private static void applyAttributeMultiplier(LivingEntity livingEntity, Attribute attribute, double multiplier) {
        AttributeInstance instance = livingEntity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.setBaseValue(instance.getBaseValue() * multiplier);
    }

    private static void applyAttributeMultiplierByName(LivingEntity livingEntity, String attributeName, double multiplier) {
        try {
            Attribute attribute = Attribute.valueOf(attributeName);
            applyAttributeMultiplier(livingEntity, attribute, multiplier);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static Location findSpawnLocation(Location center) {
        World world = center.getWorld();
        Location base = center.clone().add(0.5, 1.0, 0.5);
        if (world == null) {
            return base;
        }

        List<Location> candidates = new ArrayList<>();
        int baseY = center.getBlockY();
        for (int offsetX = -2; offsetX <= 2; offsetX++) {
            for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
                int blockX = center.getBlockX() + offsetX;
                int blockZ = center.getBlockZ() + offsetZ;

                for (int yOffset = 3; yOffset >= -3; yOffset--) {
                    int blockY = baseY + yOffset;
                    Location ground = new Location(world, blockX, blockY, blockZ);
                    if (!ground.getBlock().getType().isSolid()) {
                        continue;
                    }

                    Location spawn = ground.clone().add(0.5, 1.0, 0.5);
                    if (spawn.getBlock().isPassable() && spawn.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()) {
                        candidates.add(spawn);
                        break;
                    }
                }
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }

        return base;
    }

    private static void startMobLeashTask() {
        if (plugin == null || mobLeashTask != null) {
            return;
        }
        mobLeashTask = new BukkitRunnable() {
            @Override
            public void run() {
                int leashRadius = getMobLeashRadius();
                for (Map.Entry<Location, AsteroidEntry> entry : ASTEROIDS.entrySet()) {
                    syncAsteroidMarker(entry.getKey());
                    Location center = entry.getKey().clone().add(0.5, 1.0, 0.5);
                    World world = center.getWorld();
                    if (world == null) {
                        continue;
                    }
                    entry.getValue().mobs().removeIf(mobId -> {
                        org.bukkit.entity.Entity entity = world.getEntity(mobId);
                        if (!(entity instanceof LivingEntity mob) || mob.isDead()) {
                            return true;
                        }
                        Location mobLoc = mob.getLocation();
                        if (Math.abs(mobLoc.getBlockX() - center.getBlockX()) > leashRadius
                                || Math.abs(mobLoc.getBlockZ() - center.getBlockZ()) > leashRadius) {
                            if (!mob.getScoreboardTags().contains(ASTEROID_AGGRESSIVE_TAG)) {
                                mob.teleport(center);
                                applyReturnRegeneration(mob);
                            }
                        }
                        if (mob instanceof Mob aggressiveMob && aggressiveMob.getTarget() == null) {
                            Player nearest = findNearestPlayer(center, getMobReactionRadius(leashRadius));
                            if (nearest != null) {
                                aggressiveMob.setTarget(nearest);
                            }
                        }
                        return false;
                    });
                }
            }
        }.runTaskTimer(plugin, 20L, 40L);
    }

    private static Player findNearestPlayer(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        double bestDistance = Double.MAX_VALUE;
        Player best = null;
        for (Player player : world.getNearbyPlayers(center, radius)) {
            double distance = player.getLocation().distanceSquared(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    public static int getMobLeashRadius() {
        if (plugin == null) {
            return 16;
        }
        return plugin.getConfig().getInt("asteroid.mobLeashRadius", 16);
    }

    private static double getMobReactionRadius(int leashRadius) {
        if (plugin == null) {
            return leashRadius;
        }
        double configured = plugin.getConfig().getDouble("asteroid.mobReactionRadius", leashRadius);
        return Math.max(leashRadius, configured);
    }

    private static void applyReturnRegeneration(LivingEntity mob) {
        if (mob.isDead()) {
            return;
        }
        AttributeInstance maxHealthAttribute = mob.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : mob.getMaxHealth();
        double healAmount = Math.max(2.0, maxHealth * 0.05);
        mob.setHealth(Math.min(maxHealth, mob.getHealth() + healAmount));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, true, true));
    }
}
