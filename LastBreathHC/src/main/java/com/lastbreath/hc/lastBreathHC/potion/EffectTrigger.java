package com.lastbreath.hc.lastBreathHC.potion;

import java.util.Locale;

public enum EffectTrigger {
    ON_DRINK,
    AFTER_EFFECT;

    public static EffectTrigger fromString(String value, EffectTrigger fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return EffectTrigger.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
