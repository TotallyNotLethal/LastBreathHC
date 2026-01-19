package com.lastbreath.hc.lastBreathHC.bloodmoon;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class BloodMoonScheduler extends BukkitRunnable {

    private static final long DAY_LENGTH_TICKS = 24000L;
    private static final long NIGHT_START_TICKS = 13000L;
    private static final int BLOOD_MOON_DAY_INTERVAL = 25;

    private final BloodMoonManager bloodMoonManager;

    public BloodMoonScheduler(BloodMoonManager bloodMoonManager) {
        this.bloodMoonManager = bloodMoonManager;
    }

    @Override
    public void run() {
        if (Bukkit.getWorlds().isEmpty()) {
            return;
        }

        if (bloodMoonManager.isActive()) {
            if (shouldStopBloodMoon()) {
                bloodMoonManager.stop();
            }
            return;
        }

        TriggerCandidate candidate = findStartCandidate();
        if (candidate != null) {
            bloodMoonManager.setActiveDay(candidate.day());
            bloodMoonManager.start();
        }
    }

    private TriggerCandidate findStartCandidate() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            long day = (world.getFullTime() / DAY_LENGTH_TICKS) + 1;
            long time = world.getTime();
            if (time < NIGHT_START_TICKS) {
                continue;
            }

            if (day % BLOOD_MOON_DAY_INTERVAL != 0) {
                continue;
            }

            long lastTriggered = bloodMoonManager.getLastTriggeredDay(world);
            if (day == lastTriggered) {
                continue;
            }

            bloodMoonManager.setLastTriggeredDay(world, day);
            return new TriggerCandidate(day);
        }

        return null;
    }

    private boolean shouldStopBloodMoon() {
        long activeDay = bloodMoonManager.getActiveDay();
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            long day = (world.getFullTime() / DAY_LENGTH_TICKS) + 1;
            if (world.getTime() < NIGHT_START_TICKS) {
                return true;
            }
            if (activeDay > 0 && day != activeDay) {
                return true;
            }
        }

        return false;
    }

    private record TriggerCandidate(long day) {
    }
}
