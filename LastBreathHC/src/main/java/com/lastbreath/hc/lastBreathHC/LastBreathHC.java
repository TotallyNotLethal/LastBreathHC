package com.lastbreath.hc.lastBreathHC;

import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidListener;
import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidManager;
import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonListener;
import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonManager;
import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonScheduler;
import com.lastbreath.hc.lastBreathHC.bounty.BountyListener;
import com.lastbreath.hc.lastBreathHC.bounty.BountyManager;
import com.lastbreath.hc.lastBreathHC.commands.AsteroidCommand;
import com.lastbreath.hc.lastBreathHC.commands.BloodMoonCommand;
import com.lastbreath.hc.lastBreathHC.commands.BountyCommand;
import com.lastbreath.hc.lastBreathHC.commands.EffectsCommand;
import com.lastbreath.hc.lastBreathHC.commands.TitlesCommand;
import com.lastbreath.hc.lastBreathHC.heads.HeadListener;
import com.lastbreath.hc.lastBreathHC.heads.HeadManager;
import com.lastbreath.hc.lastBreathHC.gui.EffectsStatusGUI;
import com.lastbreath.hc.lastBreathHC.mobs.MobScalingListener;
import com.lastbreath.hc.lastBreathHC.revive.ReviveStateListener;
import com.lastbreath.hc.lastBreathHC.revive.ReviveStateManager;
import com.lastbreath.hc.lastBreathHC.spawners.SpawnerListener;
import com.lastbreath.hc.lastBreathHC.stats.StatsListener;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.TitleListener;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import com.lastbreath.hc.lastBreathHC.token.TokenRecipe;
import com.lastbreath.hc.lastBreathHC.token.ReviveGuiTokenRecipe;
import com.lastbreath.hc.lastBreathHC.gui.ReviveGUI;
import com.lastbreath.hc.lastBreathHC.gui.ReviveNameGUI;
import com.lastbreath.hc.lastBreathHC.death.DeathListener;
import com.lastbreath.hc.lastBreathHC.death.DeathRejoinListener;
import com.lastbreath.hc.lastBreathHC.environment.EnvironmentalEffectsManager;
import com.lastbreath.hc.lastBreathHC.gui.BountyBoardGUI;
import com.lastbreath.hc.lastBreathHC.items.CustomItemRecipes;
import com.lastbreath.hc.lastBreathHC.items.EnhancedGrindstoneListener;
import com.lastbreath.hc.lastBreathHC.items.GracestoneLifeListener;
import com.lastbreath.hc.lastBreathHC.items.GracestoneListener;
import com.lastbreath.hc.lastBreathHC.mobs.ArrowAggroListener;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectApplier;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectManager;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectRegistry;
import com.lastbreath.hc.lastBreathHC.potion.CauldronBrewingListener;
import com.lastbreath.hc.lastBreathHC.potion.PotionHandler;
import com.lastbreath.hc.lastBreathHC.potion.PotionDefinitionRegistry;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public final class LastBreathHC extends JavaPlugin {

    private static LastBreathHC instance;
    private final Random random = new Random();
    private BukkitTask asteroidTask;
    private BukkitTask bountyTimeTask;
    private BukkitTask bountyCleanupTask;
    private BukkitTask bloodMoonTask;
    private BukkitTask titleEffectTask;
    private BloodMoonManager bloodMoonManager;
    private EnvironmentalEffectsManager environmentalEffectsManager;
    private PotionDefinitionRegistry potionDefinitionRegistry;
    private CustomPotionEffectRegistry customPotionEffectRegistry;
    private CustomPotionEffectManager customPotionEffectManager;
    private EffectsStatusGUI effectsStatusGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("LastBreathHC enabled.");
        potionDefinitionRegistry = PotionDefinitionRegistry.load(this, "potion-definitions.yml");
        customPotionEffectRegistry = CustomPotionEffectRegistry.load(this, "custom-effects.yml");
        HeadManager.init();
        BountyManager.load();
        AsteroidManager.initialize(this);
        ReviveStateManager.initialize(this);
        bloodMoonManager = new BloodMoonManager(this);

        getServer().getPluginManager().registerEvents(
                new DeathListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new DeathRejoinListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new HeadListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ReviveGUI(), this
        );
        getServer().getPluginManager().registerEvents(
                new ReviveNameGUI(), this
        );
        getServer().getPluginManager().registerEvents(
                new ReviveStateListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new AsteroidListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new TitleListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new MobScalingListener(bloodMoonManager), this
        );
        getServer().getPluginManager().registerEvents(
                new BountyListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new BountyBoardGUI(), this
        );
        getServer().getPluginManager().registerEvents(
                new SpawnerListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new StatsListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new BloodMoonListener(bloodMoonManager), this
        );
        getServer().getPluginManager().registerEvents(
                new EnhancedGrindstoneListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new GracestoneListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new GracestoneLifeListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ArrowAggroListener(this), this
        );
        environmentalEffectsManager = new EnvironmentalEffectsManager(this);
        getServer().getPluginManager().registerEvents(
                environmentalEffectsManager, this
        );
        PotionHandler potionHandler = new PotionHandler(this, potionDefinitionRegistry);
        getServer().getPluginManager().registerEvents(
                potionHandler, this
        );
        getServer().getPluginManager().registerEvents(
                new CauldronBrewingListener(this, potionHandler, potionDefinitionRegistry), this
        );
        customPotionEffectManager = new CustomPotionEffectManager(this, potionDefinitionRegistry, customPotionEffectRegistry);
        effectsStatusGUI = new EffectsStatusGUI(customPotionEffectManager, customPotionEffectRegistry);
        getServer().getPluginManager().registerEvents(
                customPotionEffectManager, this
        );
        getServer().getPluginManager().registerEvents(
                new CustomPotionEffectApplier(this, customPotionEffectManager), this
        );
        getServer().getPluginManager().registerEvents(
                effectsStatusGUI, this
        );

        TokenRecipe.register();
        ReviveGuiTokenRecipe.register();
        CustomItemRecipes.register();
        scheduleNextAsteroid();
        scheduleBountyTimers();
        scheduleBloodMoonChecks();
        scheduleTitleEffects();

        getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> {
                    event.registrar().register("asteroid", new AsteroidCommand());
                    event.registrar().register("bloodmoon", new BloodMoonCommand());
                    event.registrar().register("titles", new TitlesCommand());
                    event.registrar().register("bounty", new BountyCommand());
                    event.registrar().register("effects", new EffectsCommand(customPotionEffectManager, customPotionEffectRegistry, effectsStatusGUI));
                }
        );
    }

    @Override
    public void onDisable() {
        if (asteroidTask != null) {
            asteroidTask.cancel();
            asteroidTask = null;
        }
        if (bountyTimeTask != null) {
            bountyTimeTask.cancel();
            bountyTimeTask = null;
        }
        if (bountyCleanupTask != null) {
            bountyCleanupTask.cancel();
            bountyCleanupTask = null;
        }
        if (bloodMoonTask != null) {
            bloodMoonTask.cancel();
            bloodMoonTask = null;
        }
        if (titleEffectTask != null) {
            titleEffectTask.cancel();
            titleEffectTask = null;
        }
        if (environmentalEffectsManager != null) {
            environmentalEffectsManager.shutdown();
            environmentalEffectsManager = null;
        }
        if (bloodMoonManager != null) {
            bloodMoonManager.shutdown();
        }
        AsteroidManager.clearAllAsteroids();
        BountyManager.save();
        ReviveStateManager.save();
        StatsManager.saveAll();
        potionDefinitionRegistry = null;
        customPotionEffectRegistry = null;
        customPotionEffectManager = null;
        effectsStatusGUI = null;
        getLogger().info("LastBreathHC disabled.");
    }

    public static LastBreathHC getInstance() {
        return instance;
    }

    public BloodMoonManager getBloodMoonManager() {
        return bloodMoonManager;
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

                /*if (remainingSeconds <= countdownSeconds) {
                    Bukkit.broadcastMessage("☄ Asteroid in " + remainingSeconds + " seconds!");
                }*/

                remainingSeconds--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void scheduleTitleEffects() {
        if (titleEffectTask != null) {
            titleEffectTask.cancel();
        }
        titleEffectTask = new BukkitRunnable() {
            @Override
            public void run() {
                TitleManager.refreshEquippedTitleEffects();
            }
        }.runTaskTimer(this, 20L, TitleManager.getTitleEffectRefreshTicks());
    }

    private void spawnScheduledAsteroid() {
        if (!spawnRandomAsteroid()) {
            getLogger().warning("Unable to find a valid asteroid spawn location.");
        }
    }

    private void scheduleBountyTimers() {
        if (bountyTimeTask != null) {
            bountyTimeTask.cancel();
        }
        if (bountyCleanupTask != null) {
            bountyCleanupTask.cancel();
        }

        bountyTimeTask = new BukkitRunnable() {
            @Override
            public void run() {
                BountyManager.incrementOnlineTimeForOnlinePlayers(20L, 1L);
            }
        }.runTaskTimer(this, 20L, 20L);

        long dailyTicks = 20L * 60L * 60L * 24L;
        bountyCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                Instant cutoff = Instant.now().minus(Duration.ofDays(30));
                int removed = BountyManager.purgeExpiredLogouts(cutoff);
                if (removed > 0) {
                    getLogger().info("Removed " + removed + " expired bounty record(s).");
                }
            }
        }.runTaskTimer(this, dailyTicks, dailyTicks);
    }

    private void scheduleBloodMoonChecks() {
        if (bloodMoonTask != null) {
            bloodMoonTask.cancel();
        }

        bloodMoonTask = new BloodMoonScheduler(bloodMoonManager).runTaskTimer(this, 0L, 100L);
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

    public World resolveAsteroidCommandWorld(CommandSender sender) {
        if (sender instanceof BlockCommandSender blockSender) {
            return blockSender.getBlock().getWorld();
        }
        return pickAsteroidWorld();
    }

    private int pickWeightedAsteroidTier() {
        int roll = random.nextInt(100);
        if (roll < 75) {
            return 1;
        }
        if (roll < 95) {
            return 2;
        }
        return 3;
    }

    public boolean spawnRandomAsteroid() {
        World world = pickAsteroidWorld();
        if (world == null) {
            getLogger().warning("No valid asteroid worlds configured. Skipping asteroid spawn.");
            return false;
        }

        Location location = pickAsteroidLocation(world);
        if (location == null) {
            getLogger().warning("Unable to find a valid asteroid location in world " + world.getName() + ".");
            return false;
        }

        int tier = pickWeightedAsteroidTier();
        AsteroidManager.spawnAsteroid(world, location, tier);
        return true;
    }
}
