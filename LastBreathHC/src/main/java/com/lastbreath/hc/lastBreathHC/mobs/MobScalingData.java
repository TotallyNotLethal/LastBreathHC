package com.lastbreath.hc.lastBreathHC.mobs;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class MobScalingData {

    private static final String SCALING_KEY = "world_scaling_multiplier";

    private MobScalingData() {
    }

    public static void setScalingMultiplier(LivingEntity entity, double multiplier) {
        if (entity == null) {
            return;
        }
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (multiplier <= 1.0) {
            container.remove(getScalingKey());
            return;
        }
        container.set(getScalingKey(), PersistentDataType.DOUBLE, multiplier);
    }

    public static double getScalingMultiplier(LivingEntity entity) {
        if (entity == null) {
            return 1.0;
        }
        Double value = entity.getPersistentDataContainer().get(getScalingKey(), PersistentDataType.DOUBLE);
        return value == null ? 1.0 : Math.max(1.0, value);
    }

    private static NamespacedKey getScalingKey() {
        return new NamespacedKey(LastBreathHC.getInstance(), SCALING_KEY);
    }
}
