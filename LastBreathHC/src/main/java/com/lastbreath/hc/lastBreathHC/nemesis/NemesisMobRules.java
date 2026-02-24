package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidManager;
import com.lastbreath.hc.lastBreathHC.worldboss.WorldBossConstants;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

public final class NemesisMobRules {
    private static final Set<EntityType> KNOWN_HOSTILE_TYPES = Set.of(
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.GIANT
    );

    private NemesisMobRules() {
    }

    public static boolean isHostileOrAggressive(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Monster || entity instanceof Enemy || KNOWN_HOSTILE_TYPES.contains(entity.getType())) {
            return true;
        }
        if (entity instanceof Mob mob) {
            return mob.getTarget() instanceof Player;
        }
        return false;
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
        if (entity.customName() != null) {
            return true;
        }
        return worldBossTypeKey != null && entity.getPersistentDataContainer().has(worldBossTypeKey, PersistentDataType.STRING);
    }

    public static boolean isNemesisMinion(Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(MinionController.MINION_SCOREBOARD_TAG);
    }
}
