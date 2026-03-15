package com.lastbreath.hc.lastBreathHC.cosmetics;

import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;

public enum BowTrailType {
    SOUL_STREAK("Soul Streak", ChatColor.DARK_PURPLE, Particle.SOUL_FIRE_FLAME, Material.AMETHYST_SHARD),
    MIST_GLYPH("Mist Glyph", ChatColor.AQUA, Particle.GLOW, Material.PRISMARINE_CRYSTALS),
    THUNDER_SPARK("Thunder Spark", ChatColor.YELLOW, Particle.ELECTRIC_SPARK, Material.LIGHTNING_ROD),
    STORM_FROST("Storm Frost", ChatColor.BLUE, Particle.SNOWFLAKE, Material.ICE),
    SAND_DUST("Sand Dust", ChatColor.GOLD, Particle.CLOUD, Material.SAND),
    GRAVEL_RAIN("Gravel Rain", ChatColor.GRAY, Particle.SMOKE, Material.GRAVEL),
    CINDER_WISP("Cinder Wisp", ChatColor.RED, Particle.FLAME, Material.BLAZE_POWDER),
    ORACLE_ASH("Oracle Ash", ChatColor.DARK_GRAY, Particle.ASH, Material.COAL),
    COSMIC_GLINT("Cosmic Glint", ChatColor.LIGHT_PURPLE, Particle.END_ROD, Material.ENDER_PEARL),
    STARFALL("Starfall", ChatColor.WHITE, Particle.FIREWORK, Material.NETHER_STAR),
    SPORE_SWIRL("Spore Swirl", ChatColor.GREEN, Particle.SPORE_BLOSSOM_AIR, Material.MOSS_BLOCK),
    VOID_DUST("Void Dust", ChatColor.DARK_AQUA, Particle.PORTAL, Material.OBSIDIAN);

    private final String displayName;
    private final ChatColor color;
    private final Particle particle;
    private final Material icon;

    BowTrailType(String displayName, ChatColor color, Particle particle, Material icon) {
        this.displayName = displayName;
        this.color = color;
        this.particle = particle;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor color() {
        return color;
    }

    public Particle particle() {
        return particle;
    }

    public Material icon() {
        return icon;
    }

    public static BowTrailType fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        for (BowTrailType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
