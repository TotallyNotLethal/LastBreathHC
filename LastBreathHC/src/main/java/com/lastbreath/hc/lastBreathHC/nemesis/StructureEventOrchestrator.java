package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.structures.SpawnContext;
import com.lastbreath.hc.lastBreathHC.structures.StructureManager;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureEventOrchestrator {
    private final CaptainRegistry registry;
    private final StructureManager structureManager;
    private final CaptainHabitatService captainHabitatService;
    private final ArmyGraphService armyGraphService;
    private final Set<String> processedTriggerKeys = ConcurrentHashMap.newKeySet();

    public StructureEventOrchestrator(CaptainRegistry registry, StructureManager structureManager, CaptainHabitatService captainHabitatService, ArmyGraphService armyGraphService) {
        this.registry = registry;
        this.structureManager = structureManager;
        this.captainHabitatService = captainHabitatService;
        this.armyGraphService = armyGraphService;
    }


    public void onPromotion(CaptainPromotionEvent event) {
        if (!markOnce("promotion:" + event.captainId() + ":" + event.crossedThreshold())) {
            return;
        }
        applyTelemetry(event.captainId(), "structureTriggers.promotionScoreThreshold");
        onCaptainMilestone(new CaptainMilestoneEvent(
                event.captainId(),
                CaptainMilestoneEvent.MilestoneType.PROMOTION_SCORE_THRESHOLD,
                "promotionScore:" + event.crossedThreshold(),
                event.previousPromotionScore(),
                event.currentPromotionScore(),
                event.occurredAtEpochMillis()
        ));
    }

    public void onCaptainMilestone(CaptainMilestoneEvent event) {
        if (!markOnce("milestone:" + event.captainId() + ":" + event.milestoneType() + ":" + event.key())) {
            return;
        }
        applyTelemetry(event.captainId(), "structureTriggers.milestone");
        spawnForCaptainAndWarband(event.captainId(), "nemesis-milestone-template", event.key());
    }

    public void onRankChanged(CaptainRankChangedEvent event) {
        if (!markOnce("rank:" + event.captainId() + ":" + event.previousRank() + ":" + event.currentRank())) {
            return;
        }
        applyTelemetry(event.captainId(), "structureTriggers.rankChange");
        structureManager.upgradeLatestStructureForOwner(event.captainId().toString(), event.currentRank().ordinal() + 1, event.occurredAtEpochMillis());
        spawnForCaptainAndWarband(event.captainId(), rankTemplate(event.currentRank()), event.currentRank().name());
        if (event.currentRank() == Rank.OVERLORD) {
            foldCaptainsIntoOverlordArmy(event.captainId());
        }
    }


    public void onPressureThresholdCrossed(TerritoryPressureThresholdCrossedEvent event) {
        if (!markOnce("pressure:" + event.region() + ":" + event.threshold() + ":" + event.currentPressure())) {
            return;
        }
        CaptainRecord candidate = registry.getAll().stream()
                .filter(record -> event.region().equalsIgnoreCase(record.political().map(CaptainRecord.Political::region).orElse("unassigned")))
                .findFirst()
                .orElse(null);
        if (candidate == null) {
            return;
        }
        applyTelemetry(candidate.identity().captainId(), "structureTriggers.pressureThreshold");
        onCaptainMilestone(new CaptainMilestoneEvent(
                candidate.identity().captainId(),
                CaptainMilestoneEvent.MilestoneType.PRESSURE_THRESHOLD,
                event.region() + ":" + event.threshold(),
                event.previousPressure(),
                event.currentPressure(),
                event.occurredAtEpochMillis()
        ));
    }

    public void onBetrayal(CaptainBetrayalEvent event) {
        if (!markOnce("betrayal:" + event.attackerCaptainId() + ":" + event.victimCaptainId() + ":" + event.occurredAtEpochMillis())) {
            return;
        }
        applyTelemetry(event.attackerCaptainId(), "structureTriggers.betrayal");
        spawnForCaptainAndWarband(event.attackerCaptainId(), "nemesis-betrayal-template", event.reason());
    }

    public void onRaidInteraction(PlayerRaidInteractionEvent event) {
        if (!markOnce("raid:" + event.playerId() + ":" + event.targetType() + ":" + event.location().getBlockX() + ":" + event.location().getBlockY() + ":" + event.location().getBlockZ())) {
            return;
        }
        CaptainRecord candidate = registry.getAll().stream()
                .filter(record -> event.region().equalsIgnoreCase(record.political().map(CaptainRecord.Political::region).orElse("unassigned")))
                .findFirst()
                .orElse(null);
        if (candidate == null) {
            return;
        }
        applyTelemetry(candidate.identity().captainId(), "structureTriggers.raidInteraction");
        spawnForCaptainAndWarband(candidate.identity().captainId(), "nemesis-raid-" + event.targetType().name().toLowerCase(), event.targetType().name());
    }

    public int spawnFortificationWave(UUID captainId, String reason) {
        if (captainId == null) {
            return 0;
        }
        String templateId = "nemesis-admin-fortification";
        if (reason != null && reason.toLowerCase().contains("betray")) {
            templateId = "nemesis-betrayal-template";
        }
        return spawnForCaptainAndWarband(captainId, templateId, reason == null ? "admin" : reason);
    }

    private void foldCaptainsIntoOverlordArmy(UUID overlordId) {
        CaptainRecord overlord = registry.getByCaptainUuid(overlordId);
        if (overlord == null || overlord.political().isEmpty()) {
            return;
        }

        CaptainRecord.Political overlordPolitical = overlord.political().get();
        String armySeatId = overlord.identity().captainId().toString();
        List<CaptainRecord> updatedRecords = new ArrayList<>();

        for (CaptainRecord candidate : registry.getAll()) {
            if (candidate == null || candidate.identity() == null) {
                continue;
            }
            if (candidate.identity().captainId().equals(overlord.identity().captainId())) {
                updatedRecords.add(ensureArmyMetadata(candidate, overlordPolitical.region(), armySeatId, null));
                continue;
            }

            Location rallyPoint = buildRallyPoint(overlord, updatedRecords.size());
            updatedRecords.add(ensureArmyMetadata(candidate, overlordPolitical.region(), armySeatId, rallyPoint));
        }

        for (CaptainRecord updated : updatedRecords) {
            registry.upsert(updated);
        }
    }

    private CaptainRecord ensureArmyMetadata(CaptainRecord record, String region, String armySeatId, Location rallyPoint) {
        CaptainRecord.Political political = record.political().orElse(new CaptainRecord.Political(Rank.CAPTAIN.name(), region, armySeatId, 0.0, 0.25));
        CaptainRecord.Political updatedPolitical = new CaptainRecord.Political(
                political.rank(),
                region,
                armySeatId,
                political.promotionScore(),
                political.influence()
        );

        List<UUID> allies = new ArrayList<>(record.relationships().map(CaptainRecord.Relationships::allies).orElse(List.of()));
        UUID overlordId = UUID.fromString(armySeatId);
        if (!record.identity().captainId().equals(overlordId) && !allies.contains(overlordId)) {
            allies.add(overlordId);
        }
        CaptainRecord.Relationships existing = record.relationships().orElse(new CaptainRecord.Relationships(List.of(), List.of(), null, null));
        CaptainRecord.Relationships updatedRelationships = new CaptainRecord.Relationships(
                allies,
                existing.rivals(),
                record.identity().captainId().equals(overlordId) ? existing.bodyguardOf() : overlordId,
                existing.bloodBrotherOf()
        );

        CaptainRecord.Origin origin = record.origin();
        if (rallyPoint != null && rallyPoint.getWorld() != null) {
            origin = new CaptainRecord.Origin(
                    rallyPoint.getWorld().getName(),
                    rallyPoint.getChunk().getX(),
                    rallyPoint.getChunk().getZ(),
                    rallyPoint.getX(),
                    rallyPoint.getY(),
                    rallyPoint.getZ(),
                    rallyPoint.getBlock().getBiome().name()
            );
        }

        return new CaptainRecord(
                record.identity(),
                origin,
                record.victims(),
                record.nemesisScores(),
                record.progression(),
                record.naming(),
                record.traits(),
                record.minionPack(),
                record.state(),
                record.telemetry(),
                Optional.of(updatedPolitical),
                record.social(),
                Optional.of(updatedRelationships),
                record.memory(),
                record.persona(),
                record.habitat()
        );
    }

    private Location buildRallyPoint(CaptainRecord overlord, int index) {
        if (overlord.origin() == null) {
            return null;
        }
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(overlord.origin().world());
        if (world == null) {
            return null;
        }
        double offsetX = (index % 6) * 3.0 + ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
        double offsetZ = (index / 6) * 3.0 + ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
        return new Location(world,
                overlord.origin().spawnX() + offsetX,
                overlord.origin().spawnY(),
                overlord.origin().spawnZ() + offsetZ);
    }

    private String rankTemplate(Rank rank) {
        return switch (rank) {
            case CAPTAIN -> "nemesis-captain-hut";
            case WARCHIEF -> "nemesis-warchief-settlement";
            case OVERLORD -> "nemesis-overlord-stronghold";
        };
    }

    private int spawnForCaptainAndWarband(UUID captainId, String templateId, String region) {
        int index = 0;
        int spawned = 0;
        for (UUID memberId : warbandMembers(captainId)) {
            CaptainRecord record = registry.getByCaptainUuid(memberId);
            if (record == null) {
                continue;
            }
            if (spawnSingle(record, templateId, region, index++)) {
                spawned++;
            }
        }
        return spawned;
    }

    private Set<UUID> warbandMembers(UUID captainId) {
        Set<UUID> ids = new LinkedHashSet<>();
        ids.add(captainId);
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record != null && record.relationships().isPresent()) {
            ids.addAll(record.relationships().get().allies());
        }
        ArmyGraphService.ArmyLinks links = armyGraphService.linksOf(captainId);
        if (links.bodyguardOf() != null) {
            ids.add(links.bodyguardOf());
        }
        if (links.bloodBrotherOf() != null) {
            ids.add(links.bloodBrotherOf());
        }
        return ids;
    }

    private boolean spawnSingle(CaptainRecord record, String templateId, String region, int cohortIndex) {
        Location anchor = new Location(
                org.bukkit.Bukkit.getWorld(record.origin().world()),
                record.origin().spawnX() + (cohortIndex * 4.0),
                record.origin().spawnY(),
                record.origin().spawnZ() + ((cohortIndex % 2 == 0 ? 1 : -1) * 3.0)
        );
        if (anchor.getWorld() == null) {
            return false;
        }
        return structureManager.spawnStructure(
                templateId,
                anchor,
                new SpawnContext(record.identity().captainId().toString(), region, System.currentTimeMillis())
        ).map(footprint -> {
            captainHabitatService.linkCaptainToStructure(record, footprint);
            return true;
        }).orElse(false);
    }

    private boolean markOnce(String key) {
        return processedTriggerKeys.add(key);
    }

    private void applyTelemetry(UUID captainId, String key) {
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null) {
            return;
        }
        registry.upsert(NemesisTelemetry.incrementCounter(record, key, 1));
    }
}
