package com.lastbreath.hc.lastBreathHC.cosmetics;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;

public enum BossAura {
    SOUL_FLAME("Soul Flame", Particle.SOUL_FIRE_FLAME, ChatColor.DARK_PURPLE, Material.SOUL_TORCH, true,
            AuraDisplayMode.PASSIVE, false),
    STORM_SPARK("Storm Spark", Particle.ELECTRIC_SPARK, ChatColor.AQUA, Material.LIGHTNING_ROD, true,
            AuraDisplayMode.PASSIVE, false),
    DUSTVEIL("Dustveil", Particle.ASH, ChatColor.GRAY, Material.GRAY_DYE, true,
            AuraDisplayMode.PASSIVE, false),
    CINDER_VEIL("Cinder Veil", Particle.FLAME, ChatColor.GOLD, Material.FIRE_CHARGE, true,
            AuraDisplayMode.PASSIVE, false),
    STASIS_AURA("Stasis Aura", Particle.DUST, ChatColor.AQUA, Material.ENDER_PEARL, true,
            AuraDisplayMode.ENDER_PEARL_BURST, true),
    ASTRAL_DRIFT("Astral Drift", Particle.END_ROD, ChatColor.AQUA, Material.END_ROD, false,
            AuraDisplayMode.PASSIVE, false),
    ARCANE_GLIMMER("Arcane Glimmer", Particle.ENCHANT, ChatColor.LIGHT_PURPLE, Material.ENCHANTED_BOOK, false,
            AuraDisplayMode.PASSIVE, false),
    SPORE_MIST("Spore Mist", Particle.SPORE_BLOSSOM_AIR, ChatColor.GREEN, Material.SPORE_BLOSSOM, false,
            AuraDisplayMode.PASSIVE, false),
    HUSHED_CLOUD("Hushed Cloud", Particle.CLOUD, ChatColor.WHITE, Material.WHITE_DYE, false,
            AuraDisplayMode.PASSIVE, false),
    ECHO_FLICKER("Echo Flicker", Particle.SCULK_SOUL, ChatColor.DARK_AQUA, Material.SCULK, false,
            AuraDisplayMode.PASSIVE, false),
    SOUL_WISP("Soul Wisp", Particle.SOUL, ChatColor.BLUE, Material.SOUL_LANTERN, false,
            AuraDisplayMode.PASSIVE, false),
    EMBER_HAZE("Ember Haze", Particle.SMOKE, ChatColor.DARK_GRAY, Material.BLACK_DYE, false,
            AuraDisplayMode.PASSIVE, false),
    CINDER_SPARK("Cinder Spark", Particle.FLAME, ChatColor.RED, Material.BLAZE_POWDER, false,
            AuraDisplayMode.PASSIVE, false),
    STELLAR_CRIT("Stellar Crit", Particle.CRIT, ChatColor.YELLOW, Material.GLOWSTONE_DUST, false,
            AuraDisplayMode.PASSIVE, false),
    STATIC_PULSE("Static Pulse", Particle.ELECTRIC_SPARK, ChatColor.AQUA, Material.AMETHYST_SHARD, false,
            AuraDisplayMode.PASSIVE, false),
    COSMIC_BLOOM("Cosmic Bloom", Particle.PORTAL, ChatColor.LIGHT_PURPLE, Material.CHORUS_FLOWER, false,
            AuraDisplayMode.PASSIVE, false),
    AMETHYST_GLITTER("Amethyst Glitter", Particle.GLOW, ChatColor.DARK_PURPLE, Material.AMETHYST_CLUSTER, false,
            AuraDisplayMode.PASSIVE, false),
    VOID_WHISPER("Void Whisper", Particle.REVERSE_PORTAL, ChatColor.DARK_PURPLE, Material.OBSIDIAN, false,
            AuraDisplayMode.PASSIVE, false),
    WITCHLIGHT("Witchlight", Particle.WITCH, ChatColor.LIGHT_PURPLE, Material.PURPLE_CANDLE, false,
            AuraDisplayMode.PASSIVE, false),
    DRAGON_VEIL("Dragon Veil", Particle.ENCHANT, ChatColor.DARK_PURPLE, Material.DRAGON_BREATH, false,
            AuraDisplayMode.PASSIVE, false),
    TWILIGHT_MOTES("Twilight Motes", Particle.ENCHANT, ChatColor.BLUE, Material.LAPIS_LAZULI, false,
            AuraDisplayMode.PASSIVE, false),
    VERDANT_SPARKLE("Verdant Sparkle", Particle.HAPPY_VILLAGER, ChatColor.GREEN, Material.MOSS_BLOCK, false,
            AuraDisplayMode.PASSIVE, false),
    FROST_GLINT("Frost Glint", Particle.SNOWFLAKE, ChatColor.AQUA, Material.PACKED_ICE, false,
            AuraDisplayMode.PASSIVE, false),
    SUNSHARD("Sunshard", Particle.WAX_ON, ChatColor.GOLD, Material.HONEYCOMB, false,
            AuraDisplayMode.PASSIVE, false),
    ROSE_QUARTZ("Rose Quartz", Particle.CHERRY_LEAVES, ChatColor.LIGHT_PURPLE, Material.PINK_PETALS, false,
            AuraDisplayMode.PASSIVE, false);

    private final String displayName;
    private final Particle particle;
    private final ChatColor color;
    private final Material icon;
    private final boolean bossUnlock;
    private final AuraDisplayMode displayMode;
    private final boolean backgroundEffect;

    BossAura(String displayName, Particle particle, ChatColor color, Material icon, boolean bossUnlock,
             AuraDisplayMode displayMode, boolean backgroundEffect) {
        this.displayName = displayName;
        this.particle = particle;
        this.color = color;
        this.icon = icon;
        this.bossUnlock = bossUnlock;
        this.displayMode = displayMode;
        this.backgroundEffect = backgroundEffect;
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

    public AuraDisplayMode displayMode() {
        return displayMode;
    }

    public boolean isPassiveAura() {
        return displayMode == AuraDisplayMode.PASSIVE;
    }

    public boolean isEnderPearlBurst() {
        return displayMode == AuraDisplayMode.ENDER_PEARL_BURST;
    }

    public boolean isBackgroundEffect() {
        return backgroundEffect;
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

    public enum AuraDisplayMode {
        PASSIVE,
        ENDER_PEARL_BURST
    }
}
