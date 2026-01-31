package com.lastbreath.hc.lastBreathHC.cosmetics;

import java.util.Locale;

public enum CosmeticTokenType {
    PREFIX,
    AURA,
    KILL_MESSAGE;

    public static CosmeticTokenType fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        for (CosmeticTokenType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
