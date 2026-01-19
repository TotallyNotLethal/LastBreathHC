package com.lastbreath.hc.lastBreathHC.bloodmoon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
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
    private static final float HURT_ANIMATION_INTENSITY = 0.75f;
    private static final float THUNDER_VOLUME = 0.2f;
    private static final float THUNDER_PITCH = 0.8f;

    private final Plugin plugin;
    private final Map<UUID, WorldState> worldStates = new HashMap<>();
    private boolean active;
    private BukkitTask task;
    private int darknessCountdownTicks = DARKNESS_INTERVAL_TICKS;

    public BloodMoonManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return active;
    }

    public void start() {
        if (active) {
            return;
        }

        active = true;
        worldStates.clear();
        darknessCountdownTicks = DARKNESS_INTERVAL_TICKS;

        for (World world : Bukkit.getWorlds()) {
            worldStates.put(world.getUID(), new WorldState(world.getTime(), world.hasStorm(), world.isThundering()));
            world.setTime(18000L);
            world.setStorm(true);
            world.setThundering(true);
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                boolean applyDarknessNow = tickDarkness();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyBloodMoonEffects(player, applyDarknessNow);
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
            world.setStorm(false);
            world.setThundering(false);
            WorldState state = worldStates.get(world.getUID());
            if (state != null) {
                world.setTime(state.time());
                world.setStorm(state.storm());
                world.setThundering(state.thunder());
            }
        }

        worldStates.clear();
    }

    public void shutdown() {
        stop();
    }

    public void applyBloodMoonEffects(Player player) {
        applyBloodMoonEffects(player, false);
    }

    private void applyBloodMoonEffects(Player player, boolean applyDarknessNow) {
        applyEffects(player, applyDarknessNow);
        player.playHurtAnimation(HURT_ANIMATION_INTENSITY);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, THUNDER_VOLUME, THUNDER_PITCH);
    }

    private void applyEffects(Player player, boolean applyDarknessNow) {
        if (applyDarknessNow) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, DARKNESS_DURATION_TICKS, EFFECT_AMPLIFIER, true, false, false));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER, true, false, false));
    }

    private void clearEffects(Player player) {
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.SLOW);
    }

    private boolean tickDarkness() {
        darknessCountdownTicks -= TASK_INTERVAL_TICKS;
        if (darknessCountdownTicks > 0) {
            return false;
        }

        darknessCountdownTicks = DARKNESS_INTERVAL_TICKS;
        return true;
    }

    private record WorldState(long time, boolean storm, boolean thunder) {
    }
}
