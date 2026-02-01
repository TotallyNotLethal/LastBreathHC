package com.lastbreath.hc.lastBreathHC.cosmetics;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum BossKillMessage {
    GRAVEWARDEN("Gravewarden's Dirge", "%player% %color%banished%gray% the %boss% beyond the veil.", ChatColor.DARK_PURPLE, Material.SCULK_CATALYST),
    STORM_HERALD("Storm Herald's Wrath", "%player% %color%shattered%gray% the %boss% with lightning.", ChatColor.AQUA, Material.HEART_OF_THE_SEA),
    HOLLOW_COLOSSUS("Hollow Colossus' End", "%player% %color%crushed%gray% the %boss% into dust.", ChatColor.GRAY, Material.ANCIENT_DEBRIS),
    ASHEN_ORACLE("Ashen Oracle's Prophecy", "%player% %color%foretold%gray% the fall of %boss%.", ChatColor.GOLD, Material.FIRE_CHARGE);

    private final String displayName;
    private final String template;
    private final ChatColor color;
    private final Material icon;

    BossKillMessage(String displayName, String template, ChatColor color, Material icon) {
        this.displayName = displayName;
        this.template = template;
        this.color = color;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public String template() {
        return template;
    }

    public ChatColor color() {
        return color;
    }

    public Material icon() {
        return icon;
    }

    public static BossKillMessage fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = normalize(input);
        return Arrays.stream(values())
                .filter(message -> normalize(message.displayName).equals(normalized)
                        || normalize(message.name()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }
}
