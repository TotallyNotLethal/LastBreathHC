package com.lastbreath.hc.lastBreathHC.asteroid;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AsteroidManager {

    public static final Map<Location, AsteroidEntry> ASTEROIDS = new HashMap<>();
    private static final Set<String> PERSISTED_ASTEROIDS = new HashSet<>();
    private static File dataFile;
    private static JavaPlugin plugin;

    public static class AsteroidEntry {
        private final Inventory inventory;
        private final int tier;

        private AsteroidEntry(Inventory inventory, int tier) {
            this.inventory = inventory;
            this.tier = tier;
        }

        public Inventory inventory() {
            return inventory;
        }

        public int tier() {
            return tier;
        }
    }

    public static boolean isAsteroid(Location loc) {
        return ASTEROIDS.containsKey(blockLocation(loc));
    }

    public static Inventory getInventory(Location loc) {
        AsteroidEntry entry = ASTEROIDS.get(blockLocation(loc));
        return entry == null ? null : entry.inventory();
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
        ASTEROIDS.put(blockLoc, new AsteroidEntry(AsteroidLoot.createLoot(), tier));
        PERSISTED_ASTEROIDS.add(toKey(blockLoc, tier));
        saveAsteroids();
    }

    public static void initialize(JavaPlugin plugin) {
        AsteroidManager.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "asteroids.yml");
        cleanupPersistedAsteroids();
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
        int tier = determineTier(world, center);

        // 🔊 Explosion sound (no damage)
        world.playSound(
                center,
                Sound.ENTITY_GENERIC_EXPLODE,
                5.0f,
                0.8f
        );

        // 💥 Explosion particles only (no block damage)
        world.spawnParticle(
                Particle.EXPLOSION,
                center,
                1
        );

        // 🔥 Fire + scorched ground
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {

                if (Math.random() < 0.6) { // random scatter
                    Location ground = center.clone().add(x, -1, z);

                    // Only replace solid ground
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

        // ☄ Place asteroid core
        Location core = center.clone().add(0, 1, 0);
        core.getBlock().setType(Material.ANCIENT_DEBRIS);

        AsteroidManager.registerAsteroid(core, tier);
        spawnTierEffects(world, core, tier);

        // 📢 Broadcast
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

    private static int determineTier(World world, Location center) {
        if (plugin == null) {
            return 1;
        }
        int tier2Min = plugin.getConfig().getInt("asteroid.tiers.tier2MinDistance", 10000);
        int tier3Min = plugin.getConfig().getInt("asteroid.tiers.tier3MinDistance", 100000);
        double distance = center.distance(world.getSpawnLocation());
        if (distance >= tier3Min) {
            return 3;
        }
        if (distance >= tier2Min) {
            return 2;
        }
        return 1;
    }

    private static void spawnTierEffects(World world, Location center, int tier) {
        if (plugin == null) {
            return;
        }
        String basePath = "asteroid.tiers.effects.tier" + tier;
        String mobTypeName = plugin.getConfig().getString(basePath + ".mobType");
        int mobCount = plugin.getConfig().getInt(basePath + ".mobCount", 0);
        if (mobTypeName != null && !mobTypeName.isBlank() && !mobTypeName.equalsIgnoreCase("NONE")) {
            EntityType mobType = EntityType.fromName(mobTypeName.toLowerCase());
            if (mobType == null) {
                try {
                    mobType = EntityType.valueOf(mobTypeName.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    mobType = null;
                }
            }
            if (mobType != null && mobType.isAlive()) {
                for (int i = 0; i < mobCount; i++) {
                    world.spawnEntity(center, mobType);
                }
            }
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

        AreaEffectCloud cloud = (AreaEffectCloud) world.spawnEntity(center, EntityType.AREA_EFFECT_CLOUD);
        cloud.setParticle(particle);
        cloud.setRadius((float) radius);
        cloud.setDuration(durationTicks);
        cloud.setWaitTime(waitTimeTicks);
    }

}
