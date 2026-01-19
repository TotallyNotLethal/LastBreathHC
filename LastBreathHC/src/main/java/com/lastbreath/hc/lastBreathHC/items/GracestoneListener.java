package com.lastbreath.hc.lastBreathHC.items;

import org.bukkit.GameMode;
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
        if (Gracestone.isGraceActive(player)) {
            player.sendMessage("§cYour Gracestone grace is already active.");
            return;
        }

        Gracestone.activateGrace(player);
        consumeInHand(event.getHand(), player, item);
        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        player.sendMessage("§aGracestone activated. Your next death will be normal survival.");
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
