package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.structures.SpawnContext;
import com.lastbreath.hc.lastBreathHC.structures.StructureManager;
import org.bukkit.Location;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
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

    private String rankTemplate(Rank rank) {
        return switch (rank) {
            case CAPTAIN -> "nemesis-captain-hut";
            case WARCHIEF -> "nemesis-warchief-settlement";
            case OVERLORD -> "nemesis-overlord-stronghold";
        };
    }

    private void spawnForCaptainAndWarband(UUID captainId, String templateId, String region) {
        int index = 0;
        for (UUID memberId : warbandMembers(captainId)) {
            CaptainRecord record = registry.getByCaptainUuid(memberId);
            if (record == null) {
                continue;
            }
            spawnSingle(record, templateId, region, index++);
        }
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

    private void spawnSingle(CaptainRecord record, String templateId, String region, int cohortIndex) {
        Location anchor = new Location(
                org.bukkit.Bukkit.getWorld(record.origin().world()),
                record.origin().spawnX() + (cohortIndex * 4.0),
                record.origin().spawnY(),
                record.origin().spawnZ() + ((cohortIndex % 2 == 0 ? 1 : -1) * 3.0)
        );
        if (anchor.getWorld() == null) {
            return;
        }
        structureManager.spawnStructure(
                templateId,
                anchor,
                new SpawnContext(record.identity().captainId().toString(), region, System.currentTimeMillis())
        ).ifPresent(footprint -> captainHabitatService.linkCaptainToStructure(record, footprint));
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
