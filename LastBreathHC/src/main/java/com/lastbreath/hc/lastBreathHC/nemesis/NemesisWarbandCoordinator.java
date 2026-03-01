package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class NemesisWarbandCoordinator {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;
    private final DialogueEngine dialogueEngine;
    private final double conversationRangeMeters;
    private final double conversationChancePerPair;
    private final long conversationIntervalMinMs;
    private final long conversationIntervalMaxMs;
    private final double regroupDistanceSq;
    private final double leashDistanceSq;
    private final long feudBroadcastCooldownMs;
    private final long fightPitBroadcastCooldownMs;
    private final long warStateCooldownMs;
    private final double interArmyWarChance;
    private final double playerHuntRange;
    private final double armyRecruitRange;
    private final List<String> armyColorPalette;
    private final Map<String, Long> armyFeudCooldowns = new HashMap<>();
    private final Map<String, Long> armyFightPitCooldowns = new HashMap<>();
    private final Map<String, Long> interArmyWarCooldowns = new HashMap<>();
    private final Map<String, Long> pairConversationCooldowns = new HashMap<>();
    private final Map<String, String> armyColors = new HashMap<>();
    private BukkitTask task;

    public NemesisWarbandCoordinator(LastBreathHC plugin, CaptainRegistry registry, CaptainEntityBinder binder, DialogueEngine dialogueEngine) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.dialogueEngine = dialogueEngine;
        this.conversationRangeMeters = Math.max(4.0, plugin.getConfig().getDouble("nemesis.warband.conversationRangeMeters", 18.0));
        this.conversationChancePerPair = Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("nemesis.warband.conversationChancePerPair", 0.2)));
        long configuredConversationIntervalMin = Math.max(5_000L, plugin.getConfig().getLong("nemesis.warband.conversationIntervalMinMs", 20_000L));
        long configuredConversationIntervalMax = Math.max(5_000L, plugin.getConfig().getLong("nemesis.warband.conversationIntervalMaxMs", 45_000L));
        this.conversationIntervalMinMs = Math.min(configuredConversationIntervalMin, configuredConversationIntervalMax);
        this.conversationIntervalMaxMs = Math.max(configuredConversationIntervalMin, configuredConversationIntervalMax);
        this.regroupDistanceSq = Math.pow(Math.max(8.0, plugin.getConfig().getDouble("nemesis.warband.regroupDistanceMeters", 10.0)), 2);
        this.leashDistanceSq = Math.pow(Math.max(24.0, plugin.getConfig().getDouble("nemesis.warband.leashDistanceMeters", 40.0)), 2);
        this.feudBroadcastCooldownMs = Math.max(5_000L, plugin.getConfig().getLong("nemesis.warband.feudBroadcastCooldownMs", 45_000L));
        this.fightPitBroadcastCooldownMs = Math.max(10_000L, plugin.getConfig().getLong("nemesis.warband.fightPitBroadcastCooldownMs", 60_000L));
        this.warStateCooldownMs = Math.max(15_000L, plugin.getConfig().getLong("nemesis.warband.warStateCooldownMs", 60_000L));
        this.interArmyWarChance = Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("nemesis.warband.interArmyWarChance", 0.25)));
        this.playerHuntRange = Math.max(32.0, plugin.getConfig().getDouble("nemesis.warband.playerHuntRangeMeters", 96.0));
        this.armyRecruitRange = Math.max(24.0, plugin.getConfig().getDouble("nemesis.warband.armyRecruitRangeMeters", 80.0));
        this.armyColorPalette = plugin.getConfig().getStringList("nemesis.warband.armyColorPalette").isEmpty()
                ? List.of("§c", "§9", "§2", "§6", "§5", "§3")
                : plugin.getConfig().getStringList("nemesis.warband.armyColorPalette");
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
        interArmyWarCooldowns.clear();
        pairConversationCooldowns.clear();
        armyColors.clear();
    }

    private void tick() {
        recruitNearbyUnassignedCaptains();
        Map<String, List<CaptainRecord>> armies = collectArmies();
        resolveArmyWars(armies);
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

            huntNearbyPlayers(leaderRecord, leader, members);

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
        applyArmyColor(memberRecord, member, armyId);
    }

    private void huntNearbyPlayers(CaptainRecord leaderRecord, LivingEntity leader, List<CaptainRecord> members) {
        if (!(leader instanceof Mob leaderMob)) {
            return;
        }
        Player target = leader.getWorld().getPlayers().stream()
                .filter(Player::isOnline)
                .filter(player -> player.getGameMode() == org.bukkit.GameMode.SURVIVAL || player.getGameMode() == org.bukkit.GameMode.ADVENTURE)
                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(leader.getLocation())))
                .orElse(null);
        if (target == null) {
            return;
        }
        if (target.getLocation().distanceSquared(leader.getLocation()) > playerHuntRange * playerHuntRange) {
            leaderMob.getPathfinder().moveTo(target.getLocation());
            return;
        }
        leaderMob.setTarget(target);
        applyArmyColor(leaderRecord, leader, leaderRecord.political().map(CaptainRecord.Political::seatId).orElse(""));
        for (CaptainRecord member : members) {
            LivingEntity memberEntity = resolve(member);
            if (memberEntity instanceof Mob memberMob) {
                memberMob.setTarget(target);
            }
        }
    }

    private void resolveArmyWars(Map<String, List<CaptainRecord>> armies) {
        List<String> ids = armies.keySet().stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                String leftId = ids.get(i);
                String rightId = ids.get(j);
                if (ThreadLocalRandom.current().nextDouble() > interArmyWarChance) {
                    continue;
                }
                String pairKey = leftId + "::" + rightId;
                long now = System.currentTimeMillis();
                if (now < interArmyWarCooldowns.getOrDefault(pairKey, 0L)) {
                    continue;
                }
                engageWar(armies.get(leftId), armies.get(rightId));
                interArmyWarCooldowns.put(pairKey, now + warStateCooldownMs);
            }
        }
    }

    private void engageWar(List<CaptainRecord> leftArmy, List<CaptainRecord> rightArmy) {
        if (leftArmy == null || rightArmy == null || leftArmy.isEmpty() || rightArmy.isEmpty()) {
            return;
        }
        CaptainRecord leftLeader = resolveLeader(leftArmy);
        CaptainRecord rightLeader = resolveLeader(rightArmy);
        LivingEntity leftEntity = resolve(leftLeader);
        LivingEntity rightEntity = resolve(rightLeader);
        if (!(leftEntity instanceof Mob leftMob) || !(rightEntity instanceof Mob rightMob)) {
            return;
        }
        if (!leftEntity.getWorld().equals(rightEntity.getWorld())) {
            return;
        }
        leftMob.setTarget(rightEntity);
        rightMob.setTarget(leftEntity);
        maybeBroadcastFeud(leftLeader.political().map(CaptainRecord.Political::seatId).orElse("army-left"), leftLeader, rightLeader, leftEntity.getLocation());
    }

    private void recruitNearbyUnassignedCaptains() {
        List<CaptainRecord> all = registry.getAll();
        List<CaptainRecord> leaders = all.stream()
                .filter(record -> record.political().map(CaptainRecord.Political::seatId).orElse("") != null)
                .filter(record -> !record.political().map(CaptainRecord.Political::seatId).orElse("").isBlank())
                .toList();
        for (CaptainRecord candidate : all) {
            CaptainRecord.Political political = candidate.political().orElse(null);
            if (political == null || (political.seatId() != null && !political.seatId().isBlank())) {
                continue;
            }
            LivingEntity entity = resolve(candidate);
            if (entity == null) {
                continue;
            }
            CaptainRecord nearestLeader = leaders.stream()
                    .filter(leader -> resolve(leader) != null)
                    .filter(leader -> resolve(leader).getWorld().equals(entity.getWorld()))
                    .min(Comparator.comparingDouble(leader -> resolve(leader).getLocation().distanceSquared(entity.getLocation())))
                    .orElse(null);
            if (nearestLeader == null) {
                continue;
            }
            LivingEntity leaderEntity = resolve(nearestLeader);
            if (leaderEntity == null || leaderEntity.getLocation().distanceSquared(entity.getLocation()) > armyRecruitRange * armyRecruitRange) {
                continue;
            }
            String seatId = nearestLeader.political().map(CaptainRecord.Political::seatId).orElse("");
            CaptainRecord updated = withSeat(candidate, seatId);
            registry.upsert(updated);
        }
    }

    private CaptainRecord withSeat(CaptainRecord record, String seatId) {
        CaptainRecord.Political base = record.political().orElse(new CaptainRecord.Political(Rank.CAPTAIN.name(), "unknown", "", 0.0, 0.0));
        CaptainRecord.Political political = new CaptainRecord.Political(base.rank(), base.region(), seatId, base.promotionScore() + 10.0, Math.max(base.influence(), 0.1));
        return new CaptainRecord(
                record.identity(),
                record.origin(),
                record.victims(),
                record.nemesisScores(),
                record.progression(),
                record.naming(),
                record.traits(),
                record.minionPack(),
                record.state(),
                record.telemetry(),
                java.util.Optional.of(political),
                record.social(),
                record.relationships(),
                record.memory(),
                record.persona(),
                record.habitat()
        );
    }

    private void applyArmyColor(CaptainRecord record, LivingEntity entity, String armyId) {
        if (record == null || entity == null || record.naming() == null || armyId == null || armyId.isBlank()) {
            return;
        }
        String color = armyColors.computeIfAbsent(armyId, this::pickColor);
        String display = color + record.naming().displayName();
        entity.setCustomName(display);
        entity.setCustomNameVisible(true);
    }

    private String pickColor(String armyId) {
        int index = Math.floorMod(armyId.hashCode(), armyColorPalette.size());
        return armyColorPalette.get(index);
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
        String pairKey = buildPairKey(leaderRecord, memberRecord);
        if (pairKey == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < pairConversationCooldowns.getOrDefault(pairKey, 0L)) {
            return;
        }
        if (Math.random() > conversationChancePerPair) {
            scheduleNextConversation(pairKey, now);
            return;
        }
        if (distanceSq > conversationRangeMeters * conversationRangeMeters) {
            scheduleNextConversation(pairKey, now);
            return;
        }
        if (dialogueEngine.emitNpcConversation(leaderRecord, memberRecord, origin)) {
            scheduleNextConversation(pairKey, now);
            return;
        }
        pairConversationCooldowns.put(pairKey, now + 5_000L);
    }

    private void scheduleNextConversation(String pairKey, long now) {
        long delay = conversationIntervalMinMs;
        if (conversationIntervalMaxMs > conversationIntervalMinMs) {
            delay += ThreadLocalRandom.current().nextLong(conversationIntervalMaxMs - conversationIntervalMinMs + 1L);
        }
        pairConversationCooldowns.put(pairKey, now + delay);
    }

    private String buildPairKey(CaptainRecord left, CaptainRecord right) {
        if (left == null || right == null || left.identity() == null || right.identity() == null
                || left.identity().captainId() == null || right.identity().captainId() == null) {
            return null;
        }
        UUID leftId = left.identity().captainId();
        UUID rightId = right.identity().captainId();
        return leftId.compareTo(rightId) <= 0 ? leftId + ":" + rightId : rightId + ":" + leftId;
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
        return binder.resolveLiveKillerEntity(record);
    }
}
