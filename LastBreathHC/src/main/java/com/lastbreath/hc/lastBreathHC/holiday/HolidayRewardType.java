package com.lastbreath.hc.lastBreathHC.holiday;

import java.util.Optional;

public enum HolidayRewardType {
    ITEM,
    CUSTOM_ITEM,
    XP,
    COMMAND;

    public static Optional<HolidayRewardType> fromString(String raw) {
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
