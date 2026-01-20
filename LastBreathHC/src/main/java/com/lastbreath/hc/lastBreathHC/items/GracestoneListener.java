package com.lastbreath.hc.lastBreathHC.items;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class GracestoneListener implements Listener {

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!Gracestone.isGracestone(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        Gracestone.addLives(player, 1);
        consumeInHand(event.getHand(), player, item);
        player.sendMessage("§aGracestone empowered. Lives: " + Gracestone.getLives(player));
    }

    private void consumeInHand(EquipmentSlot hand, Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }

        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
