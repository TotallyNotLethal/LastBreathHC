package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Locale;

public enum DefeatSignature {
    FIRE,
    EXPLOSION,
    PROJECTILE,
    MELEE,
    FALL,
    MAGIC,
    ENVIRONMENT;

    public static DefeatSignature fromDamage(EntityDamageEvent.DamageCause cause, KillerResolver.SourceType sourceType) {
        if (cause == null) {
            return ENVIRONMENT;
        }
        if (sourceType == KillerResolver.SourceType.EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return EXPLOSION;
        }
        if (sourceType == KillerResolver.SourceType.PROJECTILE || sourceType == KillerResolver.SourceType.POTION) {
            return PROJECTILE;
        }
        return switch (cause) {
            case FIRE, FIRE_TICK, HOT_FLOOR, LAVA -> FIRE;
            case FALL, FLY_INTO_WALL -> FALL;
            case MAGIC, POISON, WITHER, DRAGON_BREATH -> MAGIC;
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, THORNS, CONTACT -> MELEE;
            default -> ENVIRONMENT;
        };
    }

    public static DefeatSignature fromString(String raw, DefeatSignature fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return DefeatSignature.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}

