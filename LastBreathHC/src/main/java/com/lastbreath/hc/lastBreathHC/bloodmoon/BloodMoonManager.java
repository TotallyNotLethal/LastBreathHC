package com.lastbreath.hc.lastBreathHC.bloodmoon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BloodMoonManager {

    private static final long TASK_INTERVAL_TICKS = 100L;
    private static final int EFFECT_DURATION_TICKS = 120;
    private static final int EFFECT_AMPLIFIER = 0;
    private static final int DARKNESS_INTERVAL_TICKS = 20 * 45;
    private static final int DARKNESS_DURATION_TICKS = 80;
    private static final float THUNDER_VOLUME = 0.2f;
    private static final float THUNDER_PITCH = 0.8f;
    private static final double BORDER_OVERLAY_SIZE = 10000.0;
    private static final double MONSTER_SPAWN_LIMIT_MULTIPLIER = 2.0;
    private static final int EXTRA_SPAWN_ATTEMPTS_PER_PLAYER = 2;
    private static final int EXTRA_SPAWN_RADIUS = 28;
    private static final int MIN_PLAYER_DISTANCE_FOR_SPAWN = 12;
    private static final int MAX_SPAWN_BLOCK_LIGHT = 7;
    private static final List<EntityType> BLOOD_MOON_MONSTER_POOL = List.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.HUSK,
            EntityType.DROWNED
    );

    private final Plugin plugin;
    private final Map<UUID, WorldState> worldStates = new HashMap<>();
    private final Map<UUID, Long> lastTriggeredDays = new HashMap<>();
    private boolean active;
    private BukkitTask task;
    private int darknessCountdownTicks = DARKNESS_INTERVAL_TICKS;
    private long activeDay = -1L;

    public BloodMoonManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return active;
    }

    public long getLastTriggeredDay(World world) {
        return lastTriggeredDays.getOrDefault(world.getUID(), -1L);
    }

    public void setLastTriggeredDay(World world, long day) {
        lastTriggeredDays.put(world.getUID(), day);
    }

    public long getActiveDay() {
        return activeDay;
    }

    public void setActiveDay(long day) {
        activeDay = day;
    }

    public void start() {
        if (active) {
            return;
        }

        active = true;
        worldStates.clear();
        darknessCountdownTicks = DARKNESS_INTERVAL_TICKS;

        for (World world : Bukkit.getWorlds()) {
            int monsterSpawnLimit = world.getMonsterSpawnLimit();
            worldStates.put(world.getUID(), new WorldState(world.getTime(), world.hasStorm(), world.isThundering(), monsterSpawnLimit));
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setTime(18000L);
                world.setStorm(true);
                world.setThundering(true);
            }
            if (monsterSpawnLimit > 0) {
                int boostedLimit = (int) Math.round(monsterSpawnLimit * MONSTER_SPAWN_LIMIT_MULTIPLIER);
                world.setMonsterSpawnLimit(Math.max(monsterSpawnLimit, boostedLimit));
            }
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                boolean applyDarknessNow = tickDarkness();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyBloodMoonEffects(player, applyDarknessNow);
                    spawnExtraMonsterNear(player);
                }
            }
        }.runTaskTimer(plugin, 0L, TASK_INTERVAL_TICKS);
    }

    public void stop() {
        if (!active) {
            return;
        }

        active = false;

        if (task != null) {
            task.cancel();
            task = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            clearEffects(player);
        }

        for (World world : Bukkit.getWorlds()) {
            WorldState state = worldStates.get(world.getUID());
            if (state != null) {
                if (shouldRestoreWorldState(world)) {
                    world.setTime(state.time());
                    world.setStorm(state.storm());
                    world.setThundering(state.thunder());
                }
                world.setMonsterSpawnLimit(state.monsterSpawnLimit());
            }
        }

        worldStates.clear();
        activeDay = -1L;
    }

    public void shutdown() {
        stop();
    }

    public void applyBloodMoonEffects(Player player) {
        applyBloodMoonEffects(player, false);
    }

    private void applyBloodMoonEffects(Player player, boolean applyDarknessNow) {
        applyEffects(player, applyDarknessNow);
        applyWorldBorderOverlay(player);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, THUNDER_VOLUME, THUNDER_PITCH);
    }

    private void applyEffects(Player player, boolean applyDarknessNow) {
        if (applyDarknessNow) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, DARKNESS_DURATION_TICKS, EFFECT_AMPLIFIER, true, false, false));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER, true, false, false));
    }

    private void clearEffects(Player player) {
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setWorldBorder(null);
    }

    private boolean tickDarkness() {
        darknessCountdownTicks -= TASK_INTERVAL_TICKS;
        if (darknessCountdownTicks > 0) {
            return false;
        }

        darknessCountdownTicks = DARKNESS_INTERVAL_TICKS;
        return true;
    }

    private void applyWorldBorderOverlay(Player player) {
        WorldBorder overlay = Bukkit.createWorldBorder();
        overlay.setSize(BORDER_OVERLAY_SIZE);
        overlay.setCenter(player.getLocation());
        overlay.setWarningDistance(0);
        overlay.setDamageBuffer(0.0);
        overlay.setDamageAmount(0.0);
        player.setWorldBorder(overlay);
    }

    private boolean shouldRestoreWorldState(World world) {
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }

        long currentDay = (world.getFullTime() / 24000L) + 1L;
        return activeDay > 0 && currentDay == activeDay && world.getTime() >= 13000L;
    }

    private void spawnExtraMonsterNear(Player player) {
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL || player.isDead()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < EXTRA_SPAWN_ATTEMPTS_PER_PLAYER; i++) {
            Location spawnLocation = findSpawnLocationNear(player, random);
            if (spawnLocation == null) {
                continue;
            }

            EntityType type = BLOOD_MOON_MONSTER_POOL.get(random.nextInt(BLOOD_MOON_MONSTER_POOL.size()));
            if (!(world.spawnEntity(spawnLocation, type) instanceof Monster)) {
                continue;
            }
            return;
        }
    }

    private Location findSpawnLocationNear(Player player, ThreadLocalRandom random) {
        World world = player.getWorld();

        int offsetX = random.nextInt(-EXTRA_SPAWN_RADIUS, EXTRA_SPAWN_RADIUS + 1);
        int offsetZ = random.nextInt(-EXTRA_SPAWN_RADIUS, EXTRA_SPAWN_RADIUS + 1);
        if ((offsetX * offsetX) + (offsetZ * offsetZ) < MIN_PLAYER_DISTANCE_FOR_SPAWN * MIN_PLAYER_DISTANCE_FOR_SPAWN) {
            return null;
        }

        int x = player.getLocation().getBlockX() + offsetX;
        int z = player.getLocation().getBlockZ() + offsetZ;
        int y = world.getHighestBlockYAt(x, z);
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 2) {
            return null;
        }

        Block floor = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        if (!floor.getType().isSolid() || feet.getType().isSolid() || head.getType().isSolid()) {
            return null;
        }

        if (feet.getLightLevel() > MAX_SPAWN_BLOCK_LIGHT) {
            return null;
        }

        if (feet.getType() != Material.AIR || head.getType() != Material.AIR) {
            return null;
        }

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private record WorldState(long time, boolean storm, boolean thunder, int monsterSpawnLimit) {
    }
}
