package com.lastbreath.hc.lastBreathHC.mobs;

import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonManager;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobScalingListener implements Listener {

    private static final double START_RADIUS = 1_000.0;
    private static final double END_RADIUS = 500_000.0;
    private static final double START_HEALTH_MULTIPLIER = 0.75;
    private static final double END_HEALTH_MULTIPLIER = 10.0;
    private static final double START_DAMAGE_MULTIPLIER = 0.5;
    private static final double END_DAMAGE_HEARTS = 25.0;
    private static final double HEART_TO_DAMAGE = 2.0;
    private static final double BLOOD_MOON_HEALTH_MULTIPLIER = 1.35;
    private static final double BLOOD_MOON_DAMAGE_MULTIPLIER = 1.25;

    private final BloodMoonManager bloodMoonManager;

    public MobScalingListener(BloodMoonManager bloodMoonManager) {
        this.bloodMoonManager = bloodMoonManager;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (entity instanceof Player) {
            return;
        }

        Location spawn = entity.getWorld().getSpawnLocation();
        double distance = entity.getLocation().distance(spawn);

        double healthMultiplier = interpolate(distance, START_RADIUS, END_RADIUS,
                START_HEALTH_MULTIPLIER, END_HEALTH_MULTIPLIER);
        boolean applyBloodMoonBuff = shouldApplyBloodMoonBuff(entity);
        double damageMultiplier = 0.0;

        if (applyBloodMoonBuff) {
            healthMultiplier *= BLOOD_MOON_HEALTH_MULTIPLIER;
        }

        AttributeInstance attackDamage = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) {
            double baseDamage = attackDamage.getBaseValue();
            double endDamageMultiplier = baseDamage > 0.0
                    ? (END_DAMAGE_HEARTS * HEART_TO_DAMAGE) / baseDamage
                    : START_DAMAGE_MULTIPLIER;
            damageMultiplier = interpolate(distance, START_RADIUS, END_RADIUS,
                    START_DAMAGE_MULTIPLIER, endDamageMultiplier);
            if (applyBloodMoonBuff) {
                damageMultiplier *= BLOOD_MOON_DAMAGE_MULTIPLIER;
            }
            attackDamage.setBaseValue(Math.max(0.0, baseDamage * damageMultiplier));
        }

        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double baseHealth = maxHealth.getBaseValue();
            double newMaxHealth = Math.max(1.0, baseHealth * healthMultiplier);
            maxHealth.setBaseValue(newMaxHealth);
            entity.setHealth(newMaxHealth);
        }
    }

    private boolean shouldApplyBloodMoonBuff(LivingEntity entity) {
        return bloodMoonManager.isActive() && entity instanceof Monster;
    }

    private double interpolate(double distance, double startRadius, double endRadius,
                               double startValue, double endValue) {
        if (distance <= startRadius) {
            return startValue;
        }
        if (distance >= endRadius) {
            return endValue;
        }
        double progress = (distance - startRadius) / (endRadius - startRadius);
        return startValue + (endValue - startValue) * progress;
    }
}
