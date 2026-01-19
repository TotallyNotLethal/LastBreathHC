package com.lastbreath.hc.lastBreathHC;

import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidListener;
import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidManager;
import com.lastbreath.hc.lastBreathHC.heads.HeadListener;
import com.lastbreath.hc.lastBreathHC.heads.HeadManager;
import com.lastbreath.hc.lastBreathHC.mobs.MobScalingListener;
import com.lastbreath.hc.lastBreathHC.titles.TitleListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.java.JavaPlugin;
import com.lastbreath.hc.lastBreathHC.token.TokenRecipe;
import com.lastbreath.hc.lastBreathHC.gui.ReviveGUI;
import com.lastbreath.hc.lastBreathHC.death.DeathListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public final class LastBreathHC extends JavaPlugin {

    private static LastBreathHC instance;
    private final Random random = new Random();
    private BukkitTask asteroidTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("LastBreathHC enabled.");
        HeadManager.init();

        getServer().getPluginManager().registerEvents(
                new DeathListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new HeadListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ReviveGUI(), this
        );
        getServer().getPluginManager().registerEvents(
                new AsteroidListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new TitleListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new MobScalingListener(), this
        );

        TokenRecipe.register();
        scheduleNextAsteroid();

        getLifecycleManager().registerEventHandler(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> event.registrar().register(
                        "asteroid",
                        new com.lastbreath.hc.lastBreathHC.commands.AsteroidCommand()
                ).register(
                        "titles",
                        new com.lastbreath.hc.lastBreathHC.commands.TitlesCommand()
                )
        );
    }

    @Override
    public void onDisable() {
        if (asteroidTask != null) {
            asteroidTask.cancel();
            asteroidTask = null;
        }
        getLogger().info("LastBreathHC disabled.");
    }

    public static LastBreathHC getInstance() {
        return instance;
    }

    private void scheduleNextAsteroid() {
        if (asteroidTask != null) {
            asteroidTask.cancel();
        }

        int minSeconds = getConfig().getInt("asteroid.spawn.minSeconds");
        int maxSeconds = getConfig().getInt("asteroid.spawn.maxSeconds");
        int countdownSeconds = getConfig().getInt("asteroid.countdownSeconds");

        if (minSeconds <= 0 || maxSeconds <= 0) {
            getLogger().warning("Asteroid spawn interval must be positive. Skipping scheduling.");
            return;
        }

        if (maxSeconds < minSeconds) {
            int swap = minSeconds;
            minSeconds = maxSeconds;
            maxSeconds = swap;
        }

        int delaySeconds = minSeconds + random.nextInt(maxSeconds - minSeconds + 1);

        asteroidTask = new BukkitRunnable() {
            private int remainingSeconds = delaySeconds;

            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    spawnScheduledAsteroid();
                    cancel();
                    scheduleNextAsteroid();
                    return;
                }

                if (remainingSeconds <= countdownSeconds) {
                    Bukkit.broadcastMessage("☄ Asteroid in " + remainingSeconds + " seconds!");
                }

                remainingSeconds--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void spawnScheduledAsteroid() {
        World world = pickAsteroidWorld();
        if (world == null) {
            getLogger().warning("No valid asteroid worlds configured. Skipping asteroid spawn.");
            return;
        }

        Location location = pickAsteroidLocation(world);
        if (location == null) {
            getLogger().warning("Unable to find a valid asteroid location in world " + world.getName() + ".");
            return;
        }

        AsteroidManager.spawnAsteroid(world, location);
    }

    private World pickAsteroidWorld() {
        List<String> worldNames = getConfig().getStringList("asteroid.worlds");
        if (worldNames.isEmpty()) {
            return null;
        }

        World world = null;
        int attempts = worldNames.size();
        for (int i = 0; i < attempts; i++) {
            String name = worldNames.get(random.nextInt(worldNames.size()));
            world = getServer().getWorld(name);
            if (world != null) {
                return world;
            }
        }

        return null;
    }

    private Location pickAsteroidLocation(World world) {
        WorldBorder border = world.getWorldBorder();
        double borderRadius = border.getSize() / 2.0;
        double spawnRadius = Math.min(10_000.0, borderRadius);

        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight() - 1;

        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * spawnRadius;
            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);

            int scanY = world.getHighestBlockYAt(blockX, blockZ);
            if (scanY < minHeight || scanY > maxHeight) {
                continue;
            }

            int groundY = -1;
            for (int y = scanY; y >= minHeight; y--) {
                if (world.getBlockAt(blockX, y, blockZ).getType().isSolid()) {
                    groundY = y;
                    break;
                }
            }

            if (groundY < minHeight || groundY >= maxHeight) {
                continue;
            }

            Location candidate = new Location(world, blockX, groundY, blockZ);
            if (!border.isInside(candidate)) {
                continue;
            }

            return candidate;
        }

        return null;
    }
}
