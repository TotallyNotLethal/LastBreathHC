package com.lastbreath.hc.lastBreathHC.worldboss;

import java.util.Locale;
import java.util.Optional;

public enum WorldBossType {
    GRAVEWARDEN("Gravewarden"),
    STORM_HERALD("StormHerald"),
    HOLLOW_COLOSSUS("HollowColossus");

    private final String configKey;

    WorldBossType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static Optional<WorldBossType> fromConfigKey(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.replace("_", "").replace(" ", "").toLowerCase(Locale.ROOT);
        for (WorldBossType type : values()) {
            String candidate = type.configKey.replace("_", "").replace(" ", "").toLowerCase(Locale.ROOT);
            if (candidate.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
