package com.lastbreath.hc.lastBreathHC.titles;

import java.util.Arrays;
import java.util.Locale;

public enum Title {
    WANDERER("Wanderer"),
    THE_FALLEN("The Fallen"),
    SOUL_RECLAIMER("Soul Reclaimer"),
    LAST_SURVIVOR("Last Survivor"),
    REVIVED("Revived"),
    ASTEROID_HUNTER("Asteroid Hunter"),
    STAR_FORGER("Star Forger"),
    VOID_WALKER("Void Walker"),
    IRON_WILL("Iron Will"),
    DEATH_DEFIER("Death Defier"),
    TIME_TOUCHED("Time Touched"),
    RELIC_SEEKER("Relic Seeker");

    private final String displayName;

    Title(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Title fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = normalize(input);
        return Arrays.stream(values())
                .filter(title -> normalize(title.displayName).equals(normalized)
                        || normalize(title.name()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }
}
