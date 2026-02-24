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

    public TerritoryPressureService(LastBreathHC plugin) {
        this.enabled = plugin.getConfig().getBoolean("nemesis.territoryPressure.enabled", true);
        this.highThreshold = plugin.getConfig().getDouble("nemesis.territoryPressure.thresholds.high", 100.0);
        this.lowThreshold = plugin.getConfig().getDouble("nemesis.territoryPressure.thresholds.low", -40.0);
    }

    public boolean enabled() {
        return enabled;
    }

    public double applyChange(String region, String reason, double delta) {
        if (!enabled || region == null || region.isBlank() || delta == 0.0) {
            return getPressure(region);
        }
        double updated = pressureByRegion.merge(region.toLowerCase(), delta, Double::sum);
        if (updated >= highThreshold || updated <= lowThreshold) {
            pressureByRegion.put(region.toLowerCase(), Math.max(lowThreshold, Math.min(highThreshold, updated)));
        }
        return pressureByRegion.get(region.toLowerCase());
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

