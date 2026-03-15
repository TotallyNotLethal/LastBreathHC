package com.lastbreath.hc.lastBreathHC.holiday;

import java.util.Optional;

public enum HolidayTaskType {
    KILL_ENTITY,
    BREAK_BLOCK,
    COLLECT_ITEM;

    public static Optional<HolidayTaskType> fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
