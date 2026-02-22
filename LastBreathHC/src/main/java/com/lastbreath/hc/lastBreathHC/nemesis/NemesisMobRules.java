package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidManager;
import com.lastbreath.hc.lastBreathHC.worldboss.WorldBossConstants;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

public final class NemesisMobRules {
    private NemesisMobRules() {
    }

    public static boolean isExcludedFromCaptainPromotion(LivingEntity entity, NamespacedKey worldBossTypeKey) {
        if (entity == null) {
            return true;
        }
        if (entity.getScoreboardTags().contains(CaptainEntityBinder.CAPTAIN_SCOREBOARD_TAG)
                || entity.getScoreboardTags().contains(MinionController.MINION_SCOREBOARD_TAG)) {
            return true;
        }
        if (entity.getScoreboardTags().contains(AsteroidManager.ASTEROID_MOB_TAG)
                || entity.getScoreboardTags().contains(AsteroidManager.ASTEROID_AGGRESSIVE_TAG)
                || entity.getScoreboardTags().contains(WorldBossConstants.WORLD_BOSS_MINION_TAG)) {
            return true;
        }
        return worldBossTypeKey != null && entity.getPersistentDataContainer().has(worldBossTypeKey, PersistentDataType.STRING);
    }

    public static boolean isNemesisMinion(Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(MinionController.MINION_SCOREBOARD_TAG);
    }
}
