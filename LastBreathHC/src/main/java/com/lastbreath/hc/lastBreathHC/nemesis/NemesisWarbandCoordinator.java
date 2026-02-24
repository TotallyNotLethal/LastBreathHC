package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;

public final class NemesisWarbandCoordinator {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;
    private final DialogueEngine dialogueEngine;
    private final double conversationRangeMeters;
    private final double conversationChancePerPair;
    private BukkitTask task;

    public NemesisWarbandCoordinator(LastBreathHC plugin, CaptainRegistry registry, CaptainEntityBinder binder, DialogueEngine dialogueEngine) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.dialogueEngine = dialogueEngine;
        this.conversationRangeMeters = Math.max(4.0, plugin.getConfig().getDouble("nemesis.warband.conversationRangeMeters", 18.0));
        this.conversationChancePerPair = Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("nemesis.warband.conversationChancePerPair", 0.2)));
    }

    public void start() {
        stop();
        long period = Math.max(20L, plugin.getConfig().getLong("nemesis.warband.tickPeriodTicks", 40L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (CaptainRecord leaderRecord : registry.getAll()) {
            LivingEntity leader = resolve(leaderRecord);
            if (leader == null) {
                continue;
            }
            Rank rank = Rank.from(leaderRecord.political().map(CaptainRecord.Political::rank).orElse("CAPTAIN"), Rank.CAPTAIN);
            if (rank == Rank.CAPTAIN) {
                continue;
            }
            String leaderRegion = leaderRecord.political().map(CaptainRecord.Political::region).orElse("unassigned").toLowerCase(Locale.ROOT);
            for (CaptainRecord memberRecord : registry.getAll()) {
                if (memberRecord.identity().captainId().equals(leaderRecord.identity().captainId())) {
                    continue;
                }
                String memberRegion = memberRecord.political().map(CaptainRecord.Political::region).orElse("unassigned").toLowerCase(Locale.ROOT);
                if (!leaderRegion.equals(memberRegion)) {
                    continue;
                }
                LivingEntity member = resolve(memberRecord);
                if (member == null || !member.getWorld().equals(leader.getWorld())) {
                    continue;
                }
                double distanceSq = member.getLocation().distanceSquared(leader.getLocation());
                if (distanceSq > 1600.0) {
                    member.teleport(leader.getLocation().clone().add(2.0, 0.0, 2.0));
                    continue;
                }
                if (distanceSq > 100.0 && member instanceof Mob mob) {
                    Location followOffset = leader.getLocation().clone().add((Math.random() - 0.5) * 5.0, 0.0, (Math.random() - 0.5) * 5.0);
                    mob.getPathfinder().moveTo(followOffset);
                }
                maybeRunConversation(leaderRecord, memberRecord, leader.getLocation(), distanceSq);
            }
        }
    }

    private void maybeRunConversation(CaptainRecord leaderRecord, CaptainRecord memberRecord, Location origin, double distanceSq) {
        if (Math.random() > conversationChancePerPair) {
            return;
        }
        if (distanceSq > conversationRangeMeters * conversationRangeMeters) {
            return;
        }
        dialogueEngine.emitNpcConversation(leaderRecord, memberRecord, origin);
    }

    private LivingEntity resolve(CaptainRecord record) {
        if (record == null || record.state() == null || record.state().runtimeEntityUuid() == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(record.state().runtimeEntityUuid());
        if (entity instanceof LivingEntity living && living.getScoreboardTags().contains(CaptainEntityBinder.CAPTAIN_SCOREBOARD_TAG)) {
            return living;
        }
        return binder.resolveEntity(record.identity().captainId()).orElse(null);
    }
}
