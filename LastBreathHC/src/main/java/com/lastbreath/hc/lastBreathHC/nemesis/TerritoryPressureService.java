package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TerritoryPressureService {
    private final boolean enabled;
    private final double highThreshold;
    private final double lowThreshold;
    private final Map<String, Double> pressureByRegion = new ConcurrentHashMap<>();
    private final StructureEventOrchestrator structureEventOrchestrator;

    public TerritoryPressureService(LastBreathHC plugin, StructureEventOrchestrator structureEventOrchestrator) {
        this.enabled = plugin.getConfig().getBoolean("nemesis.territoryPressure.enabled", true);
        this.highThreshold = plugin.getConfig().getDouble("nemesis.territoryPressure.thresholds.high", 100.0);
        this.lowThreshold = plugin.getConfig().getDouble("nemesis.territoryPressure.thresholds.low", -40.0);
        this.structureEventOrchestrator = structureEventOrchestrator;
    }

    public boolean enabled() {
        return enabled;
    }

    public double applyChange(String region, String reason, double delta) {
        if (!enabled || region == null || region.isBlank() || delta == 0.0) {
            return getPressure(region);
        }
        String key = region.toLowerCase();
        double previous = pressureByRegion.getOrDefault(key, 0.0);
        double updated = pressureByRegion.merge(key, delta, Double::sum);
        if (updated >= highThreshold || updated <= lowThreshold) {
            updated = Math.max(lowThreshold, Math.min(highThreshold, updated));
            pressureByRegion.put(key, updated);
        }
        if (previous < highThreshold && updated >= highThreshold) {
            structureEventOrchestrator.onPressureThresholdCrossed(new TerritoryPressureThresholdCrossedEvent(
                    key,
                    reason,
                    previous,
                    updated,
                    highThreshold,
                    System.currentTimeMillis()
            ));
        } else if (previous > lowThreshold && updated <= lowThreshold) {
            structureEventOrchestrator.onPressureThresholdCrossed(new TerritoryPressureThresholdCrossedEvent(
                    key,
                    reason,
                    previous,
                    updated,
                    lowThreshold,
                    System.currentTimeMillis()
            ));
        }
        return pressureByRegion.get(key);
    }

    public double getPressure(String region) {
        if (region == null) {
            return 0.0;
        }
        return pressureByRegion.getOrDefault(region.toLowerCase(), 0.0);
    }

    public void resetAll() {
        pressureByRegion.clear();
    }

    public Map<String, Double> snapshot() {
        return Collections.unmodifiableMap(pressureByRegion);
    }
}
