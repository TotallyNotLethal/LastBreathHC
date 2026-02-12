package com.lastbreath.hc.lastBreathHC.titles;

import java.util.Arrays;
import java.util.Locale;

public enum Title {
    WANDERER("Wanderer", "Wand", "Default starter title.", TitleCategory.PROGRESSION),
    THE_FALLEN("The Fallen", "Fall", "Die once.", TitleCategory.PROGRESSION),
    SOUL_RECLAIMER("Soul Reclaimer", "Soul", "Revive yourself 3 times.", TitleCategory.PROGRESSION),
    LAST_SURVIVOR("Last Survivor", "Last", "Survive for at least 100 hours.", TitleCategory.PROGRESSION),
    REVIVED("Revived", "Rev", "Revive yourself once.", TitleCategory.PROGRESSION),
    ASTEROID_HUNTER("Asteroid Hunter", "Astro", "Loot your first asteroid.", TitleCategory.PROGRESSION),
    STAR_FORGER("Star Forger", "Forge", "Loot 100 asteroids.", TitleCategory.PROGRESSION),
    VOID_WALKER("Void Walker", "Void", "Survive for at least 25 hours.", TitleCategory.PROGRESSION),
    IRON_WILL("Iron Will", "Will", "Survive for at least 10 hours.", TitleCategory.PROGRESSION),
    DEATH_DEFIER("Death Defier", "Defy", "Die at least 3 times.", TitleCategory.PROGRESSION),
    TIME_TOUCHED("Time Touched", "Time", "Survive for at least 1 hour.", TitleCategory.PROGRESSION),
    RELIC_SEEKER("Relic Seeker", "Relic", "Loot 25 asteroids.", TitleCategory.PROGRESSION),
    MONSTER_HUNTER("Monster Hunter", "Hunt", "Slay 250 mobs.", TitleCategory.COMBAT),
    SOUL_REAPER("Soul Reaper", "Reap", "Slay 1000 mobs.", TitleCategory.COMBAT),
    DOOMBRINGER("Doombringer", "Doom", "Slay 5000 mobs.", TitleCategory.COMBAT),
    TRAILBLAZER("Trailblazer", "Trail", "Travel over 100 km.", TitleCategory.PROGRESSION),
    HARVESTER("Harvester", "Harvest", "Harvest 500 crops.", TitleCategory.PROGRESSION),
    DEEP_DELVER("Deep Delver", "Delve", "Mine 3000 deep blocks.", TitleCategory.PROGRESSION),
    PROSPECTOR("Prospector", "Pros", "Mine 100 rare ores.", TitleCategory.PROGRESSION),
    ANGLER("Angler", "Angl", "Catch 200 fish.", TitleCategory.PROGRESSION),
    SKYBOUND("Skybound", "Sky", "Glide over 25 km.", TitleCategory.PROGRESSION),
    STARFORGED("Starforged", "Star", "Complete 500 asteroid raids.", TitleCategory.PROGRESSION),
    AGELESS("Ageless", "Ages", "Survive for at least 250 hours.", TitleCategory.PROGRESSION),
    GRAVEWARDEN_BANE("Gravewarden Bane", "Grave", "Defeat the Gravewarden world boss.", TitleCategory.BOSSES),
    STORM_HERALD("Storm Herald", "Storm", "Defeat the Storm Herald world boss.", TitleCategory.BOSSES),
    HOLLOW_COLOSSUS("Hollow Colossus", "Hollow", "Defeat the Hollow Colossus world boss.", TitleCategory.BOSSES),
    ASHEN_ORACLE("Ashen Oracle", "Ashen", "Defeat the Ashen Oracle world boss.", TitleCategory.BOSSES);

    private final String displayName;
    private final String tabTag;
    private final String requirementDescription;
    private final TitleCategory category;

    Title(String displayName, String tabTag, String requirementDescription, TitleCategory category) {
        this.displayName = displayName;
        this.tabTag = tabTag;
        this.requirementDescription = requirementDescription;
        this.category = category;
    }

    public String displayName() {
        return displayName;
    }

    public String tabTag() {
        return tabTag;
    }

    public String requirementDescription() {
        return requirementDescription;
    }

    public TitleCategory category() {
        return category;
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

    public enum TitleCategory {
        ALL("All"),
        COMBAT("Combat"),
        PROGRESSION("Progression"),
        BOSSES("Bosses");

        private final String label;

        TitleCategory(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
