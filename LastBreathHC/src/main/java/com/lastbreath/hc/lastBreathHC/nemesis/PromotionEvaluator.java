package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;

public class PromotionEvaluator {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final long tickPeriod;
    private final double warchiefThreshold;
    private final double overlordThreshold;

    private BukkitTask task;

    public PromotionEvaluator(LastBreathHC plugin, CaptainRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.tickPeriod = Math.max(40L, plugin.getConfig().getLong("nemesis.political.promotionTickPeriodTicks", 200L));
        this.warchiefThreshold = plugin.getConfig().getDouble("nemesis.political.thresholds.warchief", 35.0);
        this.overlordThreshold = plugin.getConfig().getDouble("nemesis.political.thresholds.overlord", 75.0);
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::evaluateTransitions, tickPeriod, tickPeriod);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void evaluateTransitions() {
        Rank defaultRank = Rank.from(plugin.getConfig().getString("nemesis.political.defaultRank", Rank.CAPTAIN.name()), Rank.CAPTAIN);
        for (CaptainRecord record : registry.getAll()) {
            CaptainRecord.Political political = record.political().orElseGet(() -> new CaptainRecord.Political(
                    defaultRank.name(),
                    plugin.getConfig().getString("nemesis.political.defaultRegion", "overworld"),
                    plugin.getConfig().getString("nemesis.political.defaultSeatId", ""),
                    plugin.getConfig().getDouble("nemesis.political.defaultPromotionScore", 0.0),
                    plugin.getConfig().getDouble("nemesis.political.defaultInfluence", 0.25)
            ));

            Rank currentRank = Rank.from(political.rank(), defaultRank);
            Rank targetRank = resolveRankForScore(political.promotionScore(), defaultRank);
            if (targetRank == currentRank) {
                continue;
            }

            CaptainRecord.Political updatedPolitical = new CaptainRecord.Political(
                    targetRank.name(),
                    political.region(),
                    political.seatId(),
                    political.promotionScore(),
                    political.influence()
            );
            registry.upsert(withPolitical(record, updatedPolitical));
        }
    }

    private Rank resolveRankForScore(double score, Rank fallback) {
        if (score >= overlordThreshold) {
            return Rank.OVERLORD;
        }
        if (score >= warchiefThreshold) {
            return Rank.WARCHIEF;
        }
        return fallback == Rank.OVERLORD || fallback == Rank.WARCHIEF ? Rank.CAPTAIN : fallback;
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
}
