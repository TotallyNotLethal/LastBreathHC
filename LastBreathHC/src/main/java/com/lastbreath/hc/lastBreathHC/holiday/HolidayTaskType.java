package com.lastbreath.hc.lastBreathHC.holiday;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public enum HolidayTaskType {
    KILL_ENTITY,
    BREAK_BLOCK,
    COLLECT_ITEM;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]+");

    public static Optional<HolidayTaskType> fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(normalize(raw)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<HolidayTaskType> fromStringOrWarn(String raw, Consumer<String> warningSink, String context) {
        Optional<HolidayTaskType> parsed = fromString(raw);
        if (parsed.isEmpty() && warningSink != null) {
            warningSink.accept("Invalid holiday task type '" + raw + "' in " + context + ". Valid values: KILL_ENTITY, BREAK_BLOCK, COLLECT_ITEM.");
        }
        return parsed;
    }

    private static String normalize(String raw) {
        String normalized = NON_ALPHANUMERIC.matcher(raw.trim().toUpperCase()).replaceAll("_");
        return normalized.replaceAll("_+", "_");
    }
}
