package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;

import java.util.Optional;

public class PoliticalProgressionUpdater {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final Rank defaultRank;
    private final String defaultRegion;
    private final String defaultSeatId;
    private final double defaultPromotionScore;
    private final double defaultInfluence;
    private final double minPromotionScore;
    private final double maxPromotionScore;
    private final double minInfluence;
    private final double maxInfluence;
    private final double softDecayStart;
    private final double softDecayFactor;
    private final StructureEventOrchestrator structureEventOrchestrator;

    public PoliticalProgressionUpdater(LastBreathHC plugin, CaptainRegistry registry, StructureEventOrchestrator structureEventOrchestrator) {
        this.plugin = plugin;
        this.registry = registry;
        this.defaultRank = Rank.from(plugin.getConfig().getString("nemesis.political.defaultRank", Rank.CAPTAIN.name()), Rank.CAPTAIN);
        this.defaultRegion = plugin.getConfig().getString("nemesis.political.defaultRegion", "overworld");
        this.defaultSeatId = plugin.getConfig().getString("nemesis.political.defaultSeatId", "");
        this.defaultPromotionScore = plugin.getConfig().getDouble("nemesis.political.defaultPromotionScore", 0.0);
        this.defaultInfluence = plugin.getConfig().getDouble("nemesis.political.defaultInfluence", 0.25);
        this.minPromotionScore = plugin.getConfig().getDouble("nemesis.political.guardrails.minPromotionScore", 0.0);
        this.maxPromotionScore = Math.max(minPromotionScore,
                plugin.getConfig().getDouble("nemesis.political.guardrails.maxPromotionScore", 150.0));
        this.minInfluence = plugin.getConfig().getDouble("nemesis.political.guardrails.minInfluence", 0.0);
        this.maxInfluence = Math.max(minInfluence,
                plugin.getConfig().getDouble("nemesis.political.guardrails.maxInfluence", 2.0));
        this.softDecayStart = plugin.getConfig().getDouble("nemesis.political.guardrails.softDecayStart", 100.0);
        this.softDecayFactor = clampFraction(plugin.getConfig().getDouble("nemesis.political.guardrails.softDecayFactor", 0.15));
        this.structureEventOrchestrator = structureEventOrchestrator;
    }

    public CaptainRecord applyAndPersist(CaptainRecord record, PoliticalGainSource source) {
        CaptainRecord updated = apply(record, source);
        registry.upsert(updated);
        return updated;
    }

    public CaptainRecord apply(CaptainRecord record, PoliticalGainSource source) {
        double scoreGain = plugin.getConfig().getDouble(source.path() + ".promotionScore", source.defaultPromotionScoreGain());
        double influenceGain = plugin.getConfig().getDouble(source.path() + ".influence", source.defaultInfluenceGain());

        CaptainRecord.Political current = record.political().orElseGet(this::defaultPolitical);
        double scoreAfterGain = current.promotionScore() + scoreGain;
        double stabilizedScore = applySoftDecay(scoreAfterGain);
        double clampedScore = clamp(stabilizedScore, minPromotionScore, maxPromotionScore);
        double clampedInfluence = clamp(current.influence() + influenceGain, minInfluence, maxInfluence);

        CaptainRecord.Political updatedPolitical = new CaptainRecord.Political(
                current.rank(),
                current.region(),
                current.seatId(),
                clampedScore,
                clampedInfluence
        );
        maybeEmitScoreMilestones(record.identity().captainId(), current.promotionScore(), clampedScore);
        return withPolitical(record, updatedPolitical);
    }

    public CaptainRecord.Political defaultPolitical() {
        return new CaptainRecord.Political(
                defaultRank.name(),
                defaultRegion,
                defaultSeatId,
                defaultPromotionScore,
                defaultInfluence
        );
    }


    private void maybeEmitScoreMilestones(java.util.UUID captainId, double previousScore, double updatedScore) {
        if (previousScore < 35.0 && updatedScore >= 35.0) {
            structureEventOrchestrator.onPromotion(new CaptainPromotionEvent(
                    captainId,
                    previousScore,
                    updatedScore,
                    35.0,
                    System.currentTimeMillis()
            ));
        }
        if (previousScore < 75.0 && updatedScore >= 75.0) {
            structureEventOrchestrator.onPromotion(new CaptainPromotionEvent(
                    captainId,
                    previousScore,
                    updatedScore,
                    75.0,
                    System.currentTimeMillis()
            ));
        }
    }
    private CaptainRecord withPolitical(CaptainRecord record, CaptainRecord.Political political) {
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
                Optional.ofNullable(political),
                record.social(),
                record.relationships(),
                record.memory(),
                record.persona()
        );
    }

    private double applySoftDecay(double score) {
        if (score <= softDecayStart || softDecayFactor <= 0.0) {
            return score;
        }
        double excess = score - softDecayStart;
        return softDecayStart + (excess * (1.0 - softDecayFactor));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampFraction(double value) {
        return clamp(value, 0.0, 1.0);
    }

    public enum PoliticalGainSource {
        PLAYER_KILL("nemesis.political.gains.playerKill", 2.5, 0.03),
        PLAYER_INTERACTION("nemesis.political.gains.playerInteraction", 0.6, 0.01),
        CAPTAIN_SKIRMISH("nemesis.political.gains.captainSkirmish", 0.9, 0.015),
        COMBAT_TICK("nemesis.political.gains.combatTick", 0.2, 0.0),
        ACTIVE_TICK("nemesis.political.gains.activeTick", 0.1, 0.0),
        CAPTAIN_VICTORY("nemesis.political.gains.captainVictory", 4.0, 0.05),
        MINION_DEATH("nemesis.political.gains.minionDeath", 0.35, 0.0);

        private final String path;
        private final double defaultPromotionScoreGain;
        private final double defaultInfluenceGain;

        PoliticalGainSource(String path, double defaultPromotionScoreGain, double defaultInfluenceGain) {
            this.path = path;
            this.defaultPromotionScoreGain = defaultPromotionScoreGain;
            this.defaultInfluenceGain = defaultInfluenceGain;
        }

        public String path() {
            return path;
        }

        public double defaultPromotionScoreGain() {
            return defaultPromotionScoreGain;
        }

        public double defaultInfluenceGain() {
            return defaultInfluenceGain;
        }
    }
}
