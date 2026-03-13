package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.mobs.MobScalingData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class WorldScalerDamageListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        applyWorldScalerOutgoingDamage(event);
        applyWorldScalerIncomingDamage(event);
    }

    private void applyWorldScalerOutgoingDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null || !TitleManager.isWorldScalerEnabled(attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        double scalingMultiplier = MobScalingData.getScalingMultiplier(target);
        if (scalingMultiplier <= 1.0) {
            return;
        }

        event.setDamage(event.getDamage() * scalingMultiplier);
    }

    private void applyWorldScalerIncomingDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!TitleManager.isWorldScalerEnabled(player)) {
            return;
        }

        LivingEntity attacker = resolveLivingAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        double damageMultiplier = MobScalingData.getDamageScalingMultiplier(attacker);
        if (damageMultiplier <= 1.0) {
            return;
        }

        event.setDamage(event.getDamage() / damageMultiplier);
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private LivingEntity resolveLivingAttacker(Entity damager) {
        if (damager instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }
}
