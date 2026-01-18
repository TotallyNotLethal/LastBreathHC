package com.lastbreath.hc.lastBreathHC.stats;

import java.util.*;

public class StatsManager {

    private static final Map<UUID, PlayerStats> stats = new HashMap<>();

    public static PlayerStats get(UUID uuid) {
        return stats.computeIfAbsent(uuid, k -> {
            PlayerStats s = new PlayerStats();
            s.uuid = k;
            return s;
        });
    }
}
