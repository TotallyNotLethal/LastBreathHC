package com.lastbreath.hc.lastBreathHC.daily;

import org.bukkit.Particle;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum DailyCosmeticType {
    STARDUST_AURA("Stardust Aura", CosmeticRenderStyle.AURA, Particle.END_ROD),
    SOULFLAME_AURA("Soulflame Aura", CosmeticRenderStyle.AURA, Particle.SOUL_FIRE_FLAME),
    AURORA_TRAIL("Aurora Trail", CosmeticRenderStyle.TRAIL, Particle.HAPPY_VILLAGER),
    EMBER_TRAIL("Ember Trail", CosmeticRenderStyle.TRAIL, Particle.FLAME);

    private final String displayName;
    private final CosmeticRenderStyle style;
    private final Particle particle;

    DailyCosmeticType(String displayName, CosmeticRenderStyle style, Particle particle) {
        this.displayName = displayName;
        this.style = style;
        this.particle = particle;
    }

    public String displayName() {
        return displayName;
    }

    public CosmeticRenderStyle style() {
        return style;
    }

    public Particle particle() {
        return particle;
    }

    public static DailyCosmeticType fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.US).replace('-', '_').replace(' ', '_');
        return Arrays.stream(values())
                .filter(value -> value.name().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public static List<DailyCosmeticType> all() {
        return List.of(values());
    }

    public enum CosmeticRenderStyle {
        AURA,
        TRAIL
    }
}
