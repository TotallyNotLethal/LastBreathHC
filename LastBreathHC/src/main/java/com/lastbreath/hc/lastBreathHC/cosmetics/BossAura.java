package com.lastbreath.hc.lastBreathHC.cosmetics;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;

public enum BossAura {
    SOUL_FLAME("Soul Flame", Particle.SOUL_FIRE_FLAME, ChatColor.DARK_PURPLE, Material.SOUL_TORCH, true),
    STORM_SPARK("Storm Spark", Particle.ELECTRIC_SPARK, ChatColor.AQUA, Material.LIGHTNING_ROD, true),
    DUSTVEIL("Dustveil", Particle.ASH, ChatColor.GRAY, Material.GRAY_DYE, true),
    CINDER_VEIL("Cinder Veil", Particle.FLAME, ChatColor.GOLD, Material.FIRE_CHARGE, true),
    ASTRAL_DRIFT("Astral Drift", Particle.END_ROD, ChatColor.AQUA, Material.END_ROD, false),
    ARCANE_GLIMMER("Arcane Glimmer", Particle.ENCHANT, ChatColor.LIGHT_PURPLE, Material.ENCHANTED_BOOK, false),
    SPORE_MIST("Spore Mist", Particle.SPORE_BLOSSOM_AIR, ChatColor.GREEN, Material.SPORE_BLOSSOM, false),
    HUSHED_CLOUD("Hushed Cloud", Particle.CLOUD, ChatColor.WHITE, Material.WHITE_DYE, false),
    ECHO_FLICKER("Echo Flicker", Particle.SCULK_SOUL, ChatColor.DARK_AQUA, Material.SCULK, false),
    SOUL_WISP("Soul Wisp", Particle.SOUL, ChatColor.BLUE, Material.SOUL_LANTERN, false),
    EMBER_HAZE("Ember Haze", Particle.SMOKE, ChatColor.DARK_GRAY, Material.BLACK_DYE, false),
    CINDER_SPARK("Cinder Spark", Particle.FLAME, ChatColor.RED, Material.BLAZE_POWDER, false),
    STELLAR_CRIT("Stellar Crit", Particle.CRIT, ChatColor.YELLOW, Material.GLOWSTONE_DUST, false),
    STATIC_PULSE("Static Pulse", Particle.ELECTRIC_SPARK, ChatColor.AQUA, Material.AMETHYST_SHARD, false),
    COSMIC_BLOOM("Cosmic Bloom", Particle.PORTAL, ChatColor.LIGHT_PURPLE, Material.CHORUS_FLOWER, false),
    AMETHYST_GLITTER("Amethyst Glitter", Particle.GLOW, ChatColor.DARK_PURPLE, Material.AMETHYST_CLUSTER, false),
    VOID_WHISPER("Void Whisper", Particle.REVERSE_PORTAL, ChatColor.DARK_PURPLE, Material.OBSIDIAN, false),
    WITCHLIGHT("Witchlight", Particle.WITCH, ChatColor.LIGHT_PURPLE, Material.PURPLE_CANDLE, false),
    DRAGON_VEIL("Dragon Veil", Particle.ENCHANT, ChatColor.DARK_PURPLE, Material.DRAGON_BREATH, false),
    TWILIGHT_MOTES("Twilight Motes", Particle.ENCHANT, ChatColor.BLUE, Material.LAPIS_LAZULI, false),
    VERDANT_SPARKLE("Verdant Sparkle", Particle.HAPPY_VILLAGER, ChatColor.GREEN, Material.MOSS_BLOCK, false),
    FROST_GLINT("Frost Glint", Particle.SNOWFLAKE, ChatColor.AQUA, Material.PACKED_ICE, false),
    SUNSHARD("Sunshard", Particle.WAX_ON, ChatColor.GOLD, Material.HONEYCOMB, false),
    ROSE_QUARTZ("Rose Quartz", Particle.CHERRY_LEAVES, ChatColor.LIGHT_PURPLE, Material.PINK_PETALS, false);

    private final String displayName;
    private final Particle particle;
    private final ChatColor color;
    private final Material icon;
    private final boolean bossUnlock;

    BossAura(String displayName, Particle particle, ChatColor color, Material icon, boolean bossUnlock) {
        this.displayName = displayName;
        this.particle = particle;
        this.color = color;
        this.icon = icon;
        this.bossUnlock = bossUnlock;
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

    public boolean isBossUnlock() {
        return bossUnlock;
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
