package com.lastbreath.hc.lastBreathHC.asteroid;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
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

    public static final Map<Location, AsteroidEntry> ASTEROIDS = new HashMap<>();
    public static final String ASTEROID_MOB_TAG = "asteroid_mob";
    public static final String ASTEROID_KEY_TAG_PREFIX = "asteroid_key:";
    public static final String ASTEROID_SCALE_TAG_PREFIX = "asteroid_scale:";
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
        blockLoc.getBlock().setType(Material.AIR);
    }

    public static void registerAsteroid(Location loc, int tier) {
        Location blockLoc = blockLocation(loc);
        ASTEROIDS.put(blockLoc, new AsteroidEntry(AsteroidLoot.createLoot(tier), tier));
        PERSISTED_ASTEROIDS.add(toKey(blockLoc, tier));
        saveAsteroids();
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
        }
        ASTEROIDS.clear();
        PERSISTED_ASTEROIDS.clear();
        saveAsteroids();
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
        Location center = loc.getBlock().getLocation();
        int tier = determineTierByDistance(world, center);
        spawnAsteroid(world, center, tier);
    }

    public static void spawnAsteroid(World world, Location loc, int tier) {
        Location center = loc.getBlock().getLocation();
        int resolvedTier = Math.max(1, Math.min(3, tier));
        enqueueAsteroidSpawn(new AsteroidSpawnSequence(world, center, resolvedTier));
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
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.6) {
                        Location ground = center.clone().add(x, 0, z);
                        if (ground.getBlock().getType().isSolid()) {
                            ground.getBlock().setType(Material.NETHERRACK);
                            Location fire = ground.clone().add(0, 1, 0);
                            if (fire.getBlock().getType() == Material.AIR) {
                                fire.getBlock().setType(Material.FIRE);
                            }
                        }
                    }
                }
            }

            Location core = center.clone().add(0, 1, 0);
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
        double scale = ThreadLocalRandom.current().nextDouble(1.0, 2.5);
        AttributeInstance scaleAttribute = livingEntity.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }
        AttributeInstance maxHealthAttribute = livingEntity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double newMax = maxHealthAttribute.getBaseValue() * scale;
            maxHealthAttribute.setBaseValue(newMax);
            livingEntity.setHealth(newMax);
        }
        AttributeInstance attackAttribute = livingEntity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            attackAttribute.setBaseValue(attackAttribute.getBaseValue() * scale);
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

        List<PotionEffect> tierPotionEffects = resolveTierPotionEffects(basePath);
        if (!resolvedTypes.isEmpty() && mobCount > 0) {
            scheduleMobSpawns(world, center, mobCount, resolvedTypes, tierPotionEffects);
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

    private static void scheduleMobSpawns(World world, Location center, int mobCount, List<EntityType> resolvedTypes,
                                          List<PotionEffect> tierPotionEffects) {
        if (plugin == null) {
            return;
        }
        int maxIndex = resolvedTypes.size();
        AsteroidEntry entry = ASTEROIDS.get(blockLocation(center));
        String asteroidKey = asteroidKey(center);
        String keyTag = ASTEROID_KEY_TAG_PREFIX + asteroidKey;
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
                        applyAsteroidScale(livingEntity);
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
                            mob.teleport(center);
                        }
                        return false;
                    });
                }
            }
        }.runTaskTimer(plugin, 20L, 40L);
    }

    public static int getMobLeashRadius() {
        if (plugin == null) {
            return 16;
        }
        return plugin.getConfig().getInt("asteroid.mobLeashRadius", 16);
    }
}