package com.lastbreath.hc.lastBreathHC.cosmetics;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum BossPrefix {
    GRAVEBORN("Graveborn", "Grave", "Grv", ChatColor.DARK_PURPLE, Material.ECHO_SHARD),
    STORMFORGED("Stormforged", "Storm", "Str", ChatColor.AQUA, Material.PRISMARINE_SHARD),
    HOLLOWED("Hollowed", "Hollow", "Hol", ChatColor.GRAY, Material.BONE),
    ASHEN("Ashen", "Ashen", "Ash", ChatColor.DARK_RED, Material.BLAZE_POWDER);

    private final String displayName;
    private final String chatTag;
    private final String tabTag;
    private final ChatColor color;
    private final Material icon;

    BossPrefix(String displayName, String chatTag, String tabTag, ChatColor color, Material icon) {
        this.displayName = displayName;
        this.chatTag = chatTag;
        this.tabTag = tabTag;
        this.color = color;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public String chatTag() {
        return color + chatTag + ChatColor.GRAY;
    }

    public String tabTag() {
        return color + tabTag + ChatColor.GRAY;
    }

    public ChatColor color() {
        return color;
    }

    public Material icon() {
        return icon;
    }

    public static BossPrefix fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = normalize(input);
        return Arrays.stream(values())
                .filter(prefix -> normalize(prefix.displayName).equals(normalized)
                        || normalize(prefix.name()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }
}
