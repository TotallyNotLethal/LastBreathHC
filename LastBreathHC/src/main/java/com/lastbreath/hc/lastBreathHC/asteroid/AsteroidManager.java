package com.lastbreath.hc.lastBreathHC.asteroid;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class AsteroidManager {

    // Location → Inventory (shared loot)
    public static final Map<Location, Inventory> ASTEROIDS = new HashMap<>();

    public static boolean isAsteroid(Location loc) {
        return ASTEROIDS.containsKey(loc);
    }

    public static Inventory getInventory(Location loc) {
        return ASTEROIDS.get(loc);
    }

    public static void remove(Location loc) {
        ASTEROIDS.remove(loc);
        loc.getBlock().setType(Material.AIR);
    }

    public static void registerAsteroid(Location loc) {
        ASTEROIDS.put(loc, AsteroidLoot.createLoot());
    }


    public static void spawnAsteroid(World world, Location loc) {

        Location center = loc.getBlock().getLocation();

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

        AsteroidManager.registerAsteroid(core);

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

}
