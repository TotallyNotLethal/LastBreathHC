package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NemesisWarbandCoordinator {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;
    private final DialogueEngine dialogueEngine;
    private final double conversationRangeMeters;
    private final double conversationChancePerPair;
    private final double regroupDistanceSq;
    private final double leashDistanceSq;
    private final long feudBroadcastCooldownMs;
    private final long fightPitBroadcastCooldownMs;
    private final Map<String, Long> armyFeudCooldowns = new HashMap<>();
    private final Map<String, Long> armyFightPitCooldowns = new HashMap<>();
    private BukkitTask task;

    public NemesisWarbandCoordinator(LastBreathHC plugin, CaptainRegistry registry, CaptainEntityBinder binder, DialogueEngine dialogueEngine) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.dialogueEngine = dialogueEngine;
        this.conversationRangeMeters = Math.max(4.0, plugin.getConfig().getDouble("nemesis.warband.conversationRangeMeters", 18.0));
        this.conversationChancePerPair = Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("nemesis.warband.conversationChancePerPair", 0.2)));
        this.regroupDistanceSq = Math.pow(Math.max(8.0, plugin.getConfig().getDouble("nemesis.warband.regroupDistanceMeters", 10.0)), 2);
        this.leashDistanceSq = Math.pow(Math.max(24.0, plugin.getConfig().getDouble("nemesis.warband.leashDistanceMeters", 40.0)), 2);
        this.feudBroadcastCooldownMs = Math.max(5_000L, plugin.getConfig().getLong("nemesis.warband.feudBroadcastCooldownMs", 45_000L));
        this.fightPitBroadcastCooldownMs = Math.max(10_000L, plugin.getConfig().getLong("nemesis.warband.fightPitBroadcastCooldownMs", 60_000L));
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
        armyFeudCooldowns.clear();
        armyFightPitCooldowns.clear();
    }

    private void tick() {
        Map<String, List<CaptainRecord>> armies = collectArmies();
        for (Map.Entry<String, List<CaptainRecord>> armyEntry : armies.entrySet()) {
            List<CaptainRecord> members = armyEntry.getValue();
            if (members.size() < 2) {
                continue;
            }
            CaptainRecord leaderRecord = resolveLeader(members);
            LivingEntity leader = resolve(leaderRecord);
            if (leader == null) {
                continue;
            }

            for (CaptainRecord memberRecord : members) {
                if (memberRecord.identity().captainId().equals(leaderRecord.identity().captainId())) {
                    continue;
                }
                LivingEntity member = resolve(memberRecord);
                if (member == null || !member.getWorld().equals(leader.getWorld())) {
                    continue;
                }
                coordinateMember(armyEntry.getKey(), leaderRecord, memberRecord, leader, member);
            }

            maybeBroadcastFightPit(armyEntry.getKey(), leaderRecord, members, leader.getLocation());
        }
    }

    private Map<String, List<CaptainRecord>> collectArmies() {
        Map<String, List<CaptainRecord>> armies = new HashMap<>();
        for (CaptainRecord record : registry.getAll()) {
            String seatId = record.political().map(CaptainRecord.Political::seatId).orElse("");
            if (seatId == null || seatId.isBlank()) {
                continue;
            }
            armies.computeIfAbsent(seatId.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(record);
        }
        return armies;
    }

    private CaptainRecord resolveLeader(List<CaptainRecord> members) {
        return members.stream()
                .max(Comparator.comparingInt((CaptainRecord record) -> Rank.from(record.political().map(CaptainRecord.Political::rank).orElse("CAPTAIN"), Rank.CAPTAIN).ordinal())
                        .thenComparingDouble(record -> record.political().map(CaptainRecord.Political::promotionScore).orElse(0.0)))
                .orElse(members.get(0));
    }

    private void coordinateMember(String armyId, CaptainRecord leaderRecord, CaptainRecord memberRecord, LivingEntity leader, LivingEntity member) {
        double distanceSq = member.getLocation().distanceSquared(leader.getLocation());
        if (distanceSq > leashDistanceSq) {
            member.teleport(leader.getLocation().clone().add(2.0, 0.0, 2.0));
            return;
        }

        if (distanceSq > regroupDistanceSq && member instanceof Mob mob) {
            Location followOffset = leader.getLocation().clone().add((Math.random() - 0.5) * 5.0, 0.0, (Math.random() - 0.5) * 5.0);
            mob.getPathfinder().moveTo(followOffset);
        }

        syncCombatTargets(leader, member);
        maybeRunConversation(leaderRecord, memberRecord, leader.getLocation(), distanceSq);
        maybeBroadcastFeud(armyId, leaderRecord, memberRecord, leader.getLocation());
    }

    private void syncCombatTargets(LivingEntity leader, LivingEntity member) {
        if (!(leader instanceof Mob leaderMob) || !(member instanceof Mob memberMob)) {
            return;
        }
        LivingEntity leaderTarget = leaderMob.getTarget();
        LivingEntity memberTarget = memberMob.getTarget();

        if (leaderTarget != null && leaderTarget.isValid() && memberTarget == null) {
            memberMob.setTarget(leaderTarget);
            return;
        }
        if (memberTarget != null && memberTarget.isValid() && leaderTarget == null) {
            leaderMob.setTarget(memberTarget);
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

    private void maybeBroadcastFeud(String armyId, CaptainRecord speaker, CaptainRecord listener, Location origin) {
        long now = System.currentTimeMillis();
        if (now < armyFeudCooldowns.getOrDefault(armyId, 0L)) {
            return;
        }
        if (!dialogueEngine.emitEventConversation("warband_feud", speaker, listener, "", origin)) {
            return;
        }
        armyFeudCooldowns.put(armyId, now + feudBroadcastCooldownMs);
    }

    private void maybeBroadcastFightPit(String armyId, CaptainRecord speaker, List<CaptainRecord> members, Location origin) {
        long now = System.currentTimeMillis();
        if (now < armyFightPitCooldowns.getOrDefault(armyId, 0L)) {
            return;
        }
        if (members.size() < 3 || Math.random() > 0.2) {
            return;
        }
        CaptainRecord sparringPartner = members.get((int) (Math.random() * members.size()));
        if (!dialogueEngine.emitEventConversation("fight_pit", speaker, sparringPartner, "", origin)) {
            return;
        }
        armyFightPitCooldowns.put(armyId, now + fightPitBroadcastCooldownMs);
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
