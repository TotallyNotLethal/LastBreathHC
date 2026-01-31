package com.lastbreath.hc.lastBreathHC.listeners;

import com.lastbreath.hc.lastBreathHC.items.CustomEnchant;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantments;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class CustomEnchantDamageListener implements Listener {

    private static final double REDUCTION_PER_PIECE = 0.25;
    private static final int MAX_PIECES = 4;

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            applyReduction(event, player, CustomEnchant.STORM_GUARD.getId());
        } else if (cause == EntityDamageEvent.DamageCause.WITHER) {
            applyReduction(event, player, CustomEnchant.WITHER_GUARD.getId());
        }
    }

    private void applyReduction(EntityDamageEvent event, Player player, String enchantId) {
        int pieces = Math.min(CustomEnchantments.countArmorPiecesWithEnchant(player, enchantId), MAX_PIECES);
        if (pieces <= 0) {
            return;
        }
        double reduction = Math.min(1.0, pieces * REDUCTION_PER_PIECE);
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }
}
