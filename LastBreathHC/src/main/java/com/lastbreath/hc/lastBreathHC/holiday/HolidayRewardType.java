package com.lastbreath.hc.lastBreathHC.holiday;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public enum HolidayRewardType {
    ITEM,
    CUSTOM_ITEM,
    XP,
    COMMAND;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]+");

    public static Optional<HolidayRewardType> fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(normalize(raw)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<HolidayRewardType> fromStringOrWarn(String raw, Consumer<String> warningSink, String context) {
        Optional<HolidayRewardType> parsed = fromString(raw);
        if (parsed.isEmpty() && warningSink != null) {
            warningSink.accept("Invalid holiday reward type '" + raw + "' in " + context + ". Valid values: ITEM, CUSTOM_ITEM, XP, COMMAND.");
        }
        return parsed;
    }

    private static String normalize(String raw) {
        String normalized = NON_ALPHANUMERIC.matcher(raw.trim().toUpperCase()).replaceAll("_");
        return normalized.replaceAll("_+", "_");
    }
}
