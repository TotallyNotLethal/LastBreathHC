package com.lastbreath.hc.lastBreathHC.worldboss;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public interface WorldBossController {

    LivingEntity getBoss();

    WorldBossType getType();

    void tick();

    void handleBossDamaged(EntityDamageByEntityEvent event);

    void handleBossAttack(EntityDamageByEntityEvent event);

    void handleBlockBreak(BlockBreakEvent event);

    default boolean isBreakableMechanicBlock(org.bukkit.block.Block block) {
        return false;
    }

    void cleanup();

    void rebuildFromPersistent();

    void onArenaEmpty();
}
