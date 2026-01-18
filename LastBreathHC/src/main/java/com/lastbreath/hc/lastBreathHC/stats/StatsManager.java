package com.lastbreath.hc.lastBreathHC.stats;

import com.lastbreath.hc.lastBreathHC.titles.TitleManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private static final Map<UUID, PlayerStats> stats = new HashMap<>();

    public static PlayerStats get(UUID uuid) {
        return stats.computeIfAbsent(uuid, k -> {
            PlayerStats s = new PlayerStats();
            s.uuid = k;
            TitleManager.initialize(s);
            return s;
        });
    }
}
