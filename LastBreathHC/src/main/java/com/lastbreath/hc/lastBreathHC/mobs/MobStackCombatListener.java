package com.lastbreath.hc.lastBreathHC.mobs;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MobStackCombatListener implements Listener {

    private final MobStackManager stackManager;

    public MobStackCombatListener(MobStackManager stackManager) {
        this.stackManager = stackManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        int stackCount = stackManager.getStackCount(entity);
        if (stackCount <= 1) {
            return;
        }

        if (event.isSweepAttack()) {
            handleSweepAttack(event, entity, player, stackCount);
            return;
        }

        double finalDamage = event.getFinalDamage();
        if (finalDamage < entity.getHealth()) {
            return;
        }

        event.setCancelled(true);
        stackManager.applyLootForKills(entity, player, 1);
        stackManager.decrementStack(entity, 1);
        if (entity.isValid()) {
            entity.setHealth(getMaxHealth(entity));
        }
    }

    private void handleSweepAttack(EntityDamageByEntityEvent event, LivingEntity entity, Player player, int stackCount) {
        double maxHealth = getMaxHealth(entity);
        double totalHealth = (stackCount - 1) * maxHealth + entity.getHealth();
        double remainingHealth = totalHealth - event.getFinalDamage();
        event.setCancelled(true);

        if (remainingHealth <= 0) {
            stackManager.applyLootForKills(entity, player, stackCount);
            entity.remove();
            return;
        }

        int remainingCount = (int) Math.ceil(remainingHealth / maxHealth);
        int kills = Math.max(0, stackCount - remainingCount);
        double representativeHealth = remainingHealth - (remainingCount - 1) * maxHealth;
        if (representativeHealth <= 0) {
            representativeHealth = maxHealth;
        }

        if (kills > 0) {
            stackManager.applyLootForKills(entity, player, kills);
        }
        stackManager.setStackCount(entity, remainingCount);
        entity.setHealth(representativeHealth);
    }

    private double getMaxHealth(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) {
            return entity.getHealth();
        }
        return attribute.getValue();
    }
}
