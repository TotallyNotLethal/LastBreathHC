package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.Locale;

public enum Rank {
    CAPTAIN(8),
    WARCHIEF(3),
    OVERLORD(1);

    private final int slots;

    Rank(int slots) {
        this.slots = slots;
    }

    public int slots() {
        return slots;
    }

    public static Rank from(String raw, Rank fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Rank.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
