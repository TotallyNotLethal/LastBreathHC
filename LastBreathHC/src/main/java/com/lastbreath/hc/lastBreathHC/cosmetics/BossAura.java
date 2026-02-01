package com.lastbreath.hc.lastBreathHC.cosmetics;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;

public enum BossAura {
    SOUL_FLAME("Soul Flame", Particle.SOUL_FIRE_FLAME, ChatColor.DARK_PURPLE, Material.SOUL_TORCH),
    STORM_SPARK("Storm Spark", Particle.ELECTRIC_SPARK, ChatColor.AQUA, Material.LIGHTNING_ROD),
    DUSTVEIL("Dustveil", Particle.ASH, ChatColor.GRAY, Material.GRAY_DYE),
    CINDER_VEIL("Cinder Veil", Particle.FLAME, ChatColor.GOLD, Material.FIRE_CHARGE);

    private final String displayName;
    private final Particle particle;
    private final ChatColor color;
    private final Material icon;

    BossAura(String displayName, Particle particle, ChatColor color, Material icon) {
        this.displayName = displayName;
        this.particle = particle;
        this.color = color;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public Particle particle() {
        return particle;
    }

    public ChatColor color() {
        return color;
    }

    public Material icon() {
        return icon;
    }

    public static BossAura fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = normalize(input);
        return Arrays.stream(values())
                .filter(aura -> normalize(aura.displayName).equals(normalized)
                        || normalize(aura.name()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }
}
