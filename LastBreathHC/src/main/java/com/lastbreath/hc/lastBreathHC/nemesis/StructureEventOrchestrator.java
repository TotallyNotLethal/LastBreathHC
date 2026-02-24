package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.structures.SpawnContext;
import com.lastbreath.hc.lastBreathHC.structures.StructureManager;
import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureEventOrchestrator {
    private final CaptainRegistry registry;
    private final StructureManager structureManager;
    private final CaptainHabitatService captainHabitatService;
    private final Set<String> processedTriggerKeys = ConcurrentHashMap.newKeySet();

    public StructureEventOrchestrator(CaptainRegistry registry, StructureManager structureManager, CaptainHabitatService captainHabitatService) {
        this.registry = registry;
        this.structureManager = structureManager;
        this.captainHabitatService = captainHabitatService;
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
        spawnForCaptain(event.captainId(), "nemesis-milestone-template", event.key());
    }

    public void onRankChanged(CaptainRankChangedEvent event) {
        if (!markOnce("rank:" + event.captainId() + ":" + event.previousRank() + ":" + event.currentRank())) {
            return;
        }
        applyTelemetry(event.captainId(), "structureTriggers.rankChange");
        structureManager.upgradeLatestStructureForOwner(event.captainId().toString(), event.currentRank().ordinal() + 1, event.occurredAtEpochMillis());
        spawnForCaptain(event.captainId(), "nemesis-rank-" + event.currentRank().name().toLowerCase(), event.currentRank().name());
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
        spawnForCaptain(event.attackerCaptainId(), "nemesis-betrayal-template", event.reason());
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
        spawnForCaptain(candidate.identity().captainId(), "nemesis-raid-" + event.targetType().name().toLowerCase(), event.targetType().name());
    }

    private void spawnForCaptain(UUID captainId, String templateId, String region) {
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null) {
            return;
        }
        Location anchor = new Location(
                org.bukkit.Bukkit.getWorld(record.origin().world()),
                record.origin().spawnX(),
                record.origin().spawnY(),
                record.origin().spawnZ()
        );
        if (anchor.getWorld() == null) {
            return;
        }
        structureManager.spawnStructure(
                templateId,
                anchor,
                new SpawnContext(captainId.toString(), region, System.currentTimeMillis())
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
