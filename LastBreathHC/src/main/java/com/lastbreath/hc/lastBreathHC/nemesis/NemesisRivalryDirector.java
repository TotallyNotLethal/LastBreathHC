package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NemesisRivalryDirector {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;

    private final long tickPeriod;
    private final double triggerChance;
    private final double maxDistance;
    private BukkitTask task;

    public NemesisRivalryDirector(LastBreathHC plugin, CaptainRegistry registry, CaptainEntityBinder binder) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.tickPeriod = Math.max(40L, plugin.getConfig().getLong("nemesis.rivalries.tickPeriodTicks", 200L));
        this.triggerChance = clampChance(plugin.getConfig().getDouble("nemesis.rivalries.triggerChancePerTick", 0.08));
        this.maxDistance = Math.max(24.0, plugin.getConfig().getDouble("nemesis.rivalries.maxDistance", 96.0));
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, tickPeriod, tickPeriod);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (Math.random() > triggerChance) {
            return;
        }

        List<CaptainRecord> active = registry.getAll().stream()
                .filter(record -> record.state() != null && record.state().state() == CaptainState.ACTIVE)
                .toList();
        if (active.size() < 2) {
            return;
        }

        CaptainRecord firstRecord = active.get(ThreadLocalRandom.current().nextInt(active.size()));
        LivingEntity first = binder.resolveLiveKillerEntity(firstRecord);
        if (!(first instanceof Mob firstMob) || !first.isValid()) {
            return;
        }

        List<CaptainRecord> candidates = new ArrayList<>();
        for (CaptainRecord other : active) {
            if (other.identity().captainId().equals(firstRecord.identity().captainId())) {
                continue;
            }
            LivingEntity second = binder.resolveLiveKillerEntity(other);
            if (!(second instanceof Mob) || !second.isValid() || !second.getWorld().equals(first.getWorld())) {
                continue;
            }
            if (second.getLocation().distanceSquared(first.getLocation()) > maxDistance * maxDistance) {
                continue;
            }
            candidates.add(other);
        }
        if (candidates.isEmpty()) {
            return;
        }

        CaptainRecord secondRecord = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        LivingEntity second = binder.resolveLiveKillerEntity(secondRecord);
        if (!(second instanceof Mob secondMob) || !second.isValid()) {
            return;
        }
        firstMob.setTarget(second);
        secondMob.setTarget(first);

        String[] reasons = {
                "blood battle",
                "revenge for a blood brother",
                "captain raid",
                "territory challenge"
        };
        String reason = reasons[ThreadLocalRandom.current().nextInt(reasons.length)];
        String firstName = firstRecord.naming() == null ? "Nemesis Captain" : firstRecord.naming().displayName();
        String secondName = secondRecord.naming() == null ? "Nemesis Captain" : secondRecord.naming().displayName();
        Bukkit.broadcastMessage("§4⚔ §cNemesis rivalry ignites: §6" + firstName + " §7vs §6" + secondName + " §8(" + reason + ")");
    }

    private double clampChance(double raw) {
        if (raw < 0.0) {
            return 0.0;
        }
        return Math.min(raw, 1.0);
    }
}
