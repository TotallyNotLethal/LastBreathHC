package com.lastbreath.hc.lastBreathHC.titles;

import java.util.Arrays;
import java.util.Locale;

public enum Title {
    WANDERER("Wanderer", "Wand"),
    THE_FALLEN("The Fallen", "Fall"),
    SOUL_RECLAIMER("Soul Reclaimer", "Soul"),
    LAST_SURVIVOR("Last Survivor", "Last"),
    REVIVED("Revived", "Rev"),
    ASTEROID_HUNTER("Asteroid Hunter", "Astro"),
    STAR_FORGER("Star Forger", "Forge"),
    VOID_WALKER("Void Walker", "Void"),
    IRON_WILL("Iron Will", "Will"),
    DEATH_DEFIER("Death Defier", "Defy"),
    TIME_TOUCHED("Time Touched", "Time"),
    RELIC_SEEKER("Relic Seeker", "Relic"),
    MONSTER_HUNTER("Monster Hunter", "Hunt"),
    SOUL_REAPER("Soul Reaper", "Reap"),
    DOOMBRINGER("Doombringer", "Doom"),
    TRAILBLAZER("Trailblazer", "Trail"),
    HARVESTER("Harvester", "Harvest"),
    DEEP_DELVER("Deep Delver", "Delve"),
    PROSPECTOR("Prospector", "Pros"),
    ANGLER("Angler", "Angl"),
    SKYBOUND("Skybound", "Sky"),
    STARFORGED("Starforged", "Star"),
    AGELESS("Ageless", "Ages"),
    GRAVEWARDEN_BANE("Gravewarden Bane", "Grave"),
    STORM_HERALD("Storm Herald", "Storm"),
    HOLLOW_COLOSSUS("Hollow Colossus", "Hollow");

    private final String displayName;
    private final String tabTag;

    Title(String displayName, String tabTag) {
        this.displayName = displayName;
        this.tabTag = tabTag;
    }

    public String displayName() {
        return displayName;
    }

    public String tabTag() {
        return tabTag;
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
