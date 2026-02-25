package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.structures.StructureFootprint;
import com.lastbreath.hc.lastBreathHC.structures.StructureFootprintRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class StructureRaidService implements Listener {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final StructureFootprintRepository footprintRepository;
    private final TerritoryPressureService pressureService;
    private final StructureEventOrchestrator orchestrator;
    private final org.bukkit.NamespacedKey captainIdKey;

    private final Map<String, Long> structureActionCooldowns = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> guardKillWindows = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> influenceLossWindows = new ConcurrentHashMap<>();
    private final Map<String, Long> processedBreakEvents = new ConcurrentHashMap<>();

    private final long bannerCooldownMs;
    private final long throneHeartCooldownMs;
    private final long guardWindowMs;
    private final int guardWindowMaxContributions;
    private final long influenceWindowMs;
    private final double influenceLossCapPerWindow;
    private final double bannerPressureMin;
    private final double bannerPressureMax;
    private final double throneHeartPressureMin;
    private final double throneHeartPressureMax;
    private final double guardKillPressureMin;
    private final double guardKillPressureMax;

    public StructureRaidService(LastBreathHC plugin,
                                CaptainRegistry registry,
                                StructureFootprintRepository footprintRepository,
                                TerritoryPressureService pressureService,
                                StructureEventOrchestrator orchestrator,
                                CaptainEntityBinder binder) {
        this.plugin = plugin;
        this.registry = registry;
        this.footprintRepository = footprintRepository;
        this.pressureService = pressureService;
        this.orchestrator = orchestrator;
        this.captainIdKey = binder.getCaptainIdKey();

        FileConfiguration cfg = plugin.getConfig();
        this.bannerCooldownMs = Math.max(500L, cfg.getLong("nemesis.structures.raidImpact.cooldowns.bannerMs", 15_000L));
        this.throneHeartCooldownMs = Math.max(500L, cfg.getLong("nemesis.structures.raidImpact.cooldowns.throneHeartMs", 30_000L));
        this.guardWindowMs = Math.max(1000L, cfg.getLong("nemesis.structures.raidImpact.guardKill.windowMs", 20_000L));
        this.guardWindowMaxContributions = Math.max(1, cfg.getInt("nemesis.structures.raidImpact.guardKill.maxContributionsPerWindow", 5));
        this.influenceWindowMs = Math.max(1000L, cfg.getLong("nemesis.structures.raidImpact.influenceLoss.windowMs", 60_000L));
        this.influenceLossCapPerWindow = Math.max(0.0, cfg.getDouble("nemesis.structures.raidImpact.influenceLoss.capPerWindow", 0.25));

        this.bannerPressureMin = cfg.getDouble("nemesis.structures.raidImpact.actions.banner.pressureDeltaMin", 4.0);
        this.bannerPressureMax = cfg.getDouble("nemesis.structures.raidImpact.actions.banner.pressureDeltaMax", 8.0);
        this.throneHeartPressureMin = cfg.getDouble("nemesis.structures.raidImpact.actions.throneHeart.pressureDeltaMin", 8.0);
        this.throneHeartPressureMax = cfg.getDouble("nemesis.structures.raidImpact.actions.throneHeart.pressureDeltaMax", 16.0);
        this.guardKillPressureMin = cfg.getDouble("nemesis.structures.raidImpact.actions.guardKill.pressureDeltaMin", 1.0);
        this.guardKillPressureMax = cfg.getDouble("nemesis.structures.raidImpact.actions.guardKill.pressureDeltaMax", 3.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRaidBreak(BlockBreakEvent event) {
        RaidAction action = mapBreakAction(event.getBlock().getType());
        if (action == null) {
            return;
        }
        String breakKey = event.getBlock().getWorld().getName() + ":" + event.getBlock().getX() + ":" + event.getBlock().getY() + ":" + event.getBlock().getZ();
        long now = System.currentTimeMillis();
        Long previous = processedBreakEvents.putIfAbsent(breakKey, now);
        if (previous != null && now - previous < 2000L) {
            return;
        }
        processedBreakEvents.put(breakKey, now);
        applyRaidImpact(event.getPlayer(), event.getBlock().getLocation(), action, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuardKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        Entity dead = event.getEntity();
        if (!dead.getScoreboardTags().contains(MinionController.MINION_SCOREBOARD_TAG)) {
            return;
        }
        applyRaidImpact(killer, dead.getLocation(), RaidAction.GUARD_KILL, dead);
    }

    private synchronized void applyRaidImpact(Player actor, Location location, RaidAction action, Entity deadGuard) {
        Optional<StructureFootprint> footprintOpt = footprintRepository.all().stream()
                .filter(f -> f.worldName().equals(location.getWorld().getName()))
                .filter(f -> f.boundingBox().contains(location.toVector()))
                .findFirst();
        if (footprintOpt.isEmpty()) {
            return;
        }
        StructureFootprint footprint = footprintOpt.get();
        UUID captainId;
        try {
            captainId = UUID.fromString(footprint.ownerCaptainId());
        } catch (Exception ignored) {
            return;
        }
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (isOnCooldown(footprint.structureId(), action, now)) {
            return;
        }
        if (action == RaidAction.GUARD_KILL && !consumeGuardContribution(footprint.structureId(), actor.getUniqueId(), now)) {
            return;
        }

        double pressureDelta = switch (action) {
            case BANNER_BREAK -> randomBetween(bannerPressureMin, bannerPressureMax);
            case THRONE_BREAK, HEART_BREAK -> randomBetween(throneHeartPressureMin, throneHeartPressureMax);
            case GUARD_KILL -> randomBetween(guardKillPressureMin, guardKillPressureMax);
        };
        double influenceLoss = switch (action) {
            case BANNER_BREAK -> 0.03;
            case THRONE_BREAK, HEART_BREAK -> 0.08;
            case GUARD_KILL -> 0.01;
        };
        influenceLoss = capInfluenceLoss(footprint.structureId(), influenceLoss, now);

        CaptainRecord.Political political = record.political().orElse(new CaptainRecord.Political(Rank.CAPTAIN.name(), footprint.region(), "", 0.0, 0.25));
        double newInfluence = Math.max(0.0, political.influence() - influenceLoss);
        CaptainRecord.Political updatedPolitical = new CaptainRecord.Political(
                political.rank(), political.region(), political.seatId(), political.promotionScore(), newInfluence
        );
        CaptainRecord.Social social = record.social().orElse(new CaptainRecord.Social(0.5, 0.2, 0.5, 0.5));
        CaptainRecord.Social updatedSocial = new CaptainRecord.Social(
                Math.max(0.0, social.loyalty() - ((action == RaidAction.THRONE_BREAK || action == RaidAction.HEART_BREAK) ? 0.04 : 0.02)),
                Math.min(1.0, social.fear() + 0.02),
                social.ambition(),
                social.confidence()
        );

        CaptainRecord updated = new CaptainRecord(
                record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(),
                record.naming(), record.traits(), record.minionPack(), record.state(), record.telemetry(),
                Optional.of(updatedPolitical), Optional.of(updatedSocial), record.relationships(), record.memory(), record.persona(), record.habitat()
        );

        pressureService.applyChange(political.region(), "structure_raid_" + action.name().toLowerCase(Locale.ROOT), pressureDelta);
        updated = NemesisTelemetry.incrementCounter(updated, "structureRaid." + action.name().toLowerCase(Locale.ROOT), 1);
        updated = NemesisTelemetry.incrementCounter(updated, "pressureChanges", 1);
        updated = NemesisTelemetry.incrementCounter(updated, "loyaltyShifts", 1);
        registry.upsert(updated);

        orchestrator.onRaidInteraction(new PlayerRaidInteractionEvent(
                actor.getUniqueId(), actor.getName(),
                toRaidTarget(action),
                footprint.region(),
                location,
                now
        ));

        plugin.getLogger().info("[NemesisRaidTelemetry] captainId=" + captainId
                + " structureId=" + footprint.structureId()
                + " action=" + action.name()
                + " pressureDelta=" + String.format(Locale.US, "%.2f", pressureDelta)
                + " influenceLoss=" + String.format(Locale.US, "%.3f", influenceLoss));
    }

    private boolean isOnCooldown(String structureId, RaidAction action, long now) {
        long cooldown = action == RaidAction.BANNER_BREAK ? bannerCooldownMs : throneHeartCooldownMs;
        if (action == RaidAction.GUARD_KILL) {
            cooldown = 0L;
        }
        if (cooldown <= 0L) {
            return false;
        }
        String key = structureId + ":" + action.name();
        long last = structureActionCooldowns.getOrDefault(key, 0L);
        if (now - last < cooldown) {
            return true;
        }
        structureActionCooldowns.put(key, now);
        return false;
    }

    private boolean consumeGuardContribution(String structureId, UUID playerId, long now) {
        String key = structureId + ":" + playerId;
        WindowCounter counter = guardKillWindows.get(key);
        if (counter == null || now - counter.windowStartEpochMs > guardWindowMs) {
            guardKillWindows.put(key, new WindowCounter(now, 1, 0.0));
            return true;
        }
        if (counter.count >= guardWindowMaxContributions) {
            return false;
        }
        guardKillWindows.put(key, new WindowCounter(counter.windowStartEpochMs, counter.count + 1, counter.total));
        return true;
    }

    private double capInfluenceLoss(String structureId, double desired, long now) {
        WindowCounter counter = influenceLossWindows.get(structureId);
        if (counter == null || now - counter.windowStartEpochMs > influenceWindowMs) {
            influenceLossWindows.put(structureId, new WindowCounter(now, 1, Math.max(0.0, desired)));
            return desired;
        }
        double remaining = Math.max(0.0, influenceLossCapPerWindow - counter.total);
        double accepted = Math.max(0.0, Math.min(desired, remaining));
        influenceLossWindows.put(structureId, new WindowCounter(counter.windowStartEpochMs, counter.count + 1, counter.total + accepted));
        return accepted;
    }

    private double randomBetween(double min, double max) {
        if (max < min) {
            double t = min;
            min = max;
            max = t;
        }
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }

    private RaidAction mapBreakAction(Material material) {
        if (material.name().endsWith("_BANNER")) {
            return RaidAction.BANNER_BREAK;
        }
        if (material == Material.LODESTONE) {
            return RaidAction.THRONE_BREAK;
        }
        if (material == Material.RESPAWN_ANCHOR) {
            return RaidAction.HEART_BREAK;
        }
        return null;
    }

    private PlayerRaidInteractionEvent.RaidTargetType toRaidTarget(RaidAction action) {
        return switch (action) {
            case BANNER_BREAK -> PlayerRaidInteractionEvent.RaidTargetType.BANNER;
            case THRONE_BREAK -> PlayerRaidInteractionEvent.RaidTargetType.THRONE;
            case HEART_BREAK, GUARD_KILL -> PlayerRaidInteractionEvent.RaidTargetType.HEART;
        };
    }

    private enum RaidAction {
        BANNER_BREAK,
        THRONE_BREAK,
        HEART_BREAK,
        GUARD_KILL
    }

    private record WindowCounter(long windowStartEpochMs, int count, double total) {
    }
}
