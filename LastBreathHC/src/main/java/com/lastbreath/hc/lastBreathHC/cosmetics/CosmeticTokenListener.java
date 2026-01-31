package com.lastbreath.hc.lastBreathHC.cosmetics;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CosmeticTokenListener implements Listener {

    @EventHandler
    public void onUseToken(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        CosmeticTokenType type = CosmeticTokenHelper.getTokenType(item);
        if (type == null) {
            return;
        }
        String id = CosmeticTokenHelper.getTokenId(item);
        if (id == null || id.isBlank()) {
            return;
        }
        Player player = event.getPlayer();
        switch (type) {
            case PREFIX -> {
                BossPrefix prefix = BossPrefix.fromInput(id);
                if (prefix == null) {
                    return;
                }
                CosmeticManager.unlockPrefix(player, prefix, "Equip it from /cosmetics.");
                consumeOne(player, item);
            }
            case AURA -> {
                BossAura aura = BossAura.fromInput(id);
                if (aura == null) {
                    return;
                }
                CosmeticManager.unlockAura(player, aura, "Equip it from /cosmetics.");
                consumeOne(player, item);
            }
            case KILL_MESSAGE -> {
                BossKillMessage message = BossKillMessage.fromInput(id);
                if (message == null) {
                    return;
                }
                CosmeticManager.unlockKillMessage(player, message, "Equip it from /cosmetics.");
                consumeOne(player, item);
            }
        }
        event.setCancelled(true);
        player.sendMessage(ChatColor.GRAY + "Use /cosmetics to equip your new unlock.");
    }

    private void consumeOne(Player player, ItemStack item) {
        int amount = item.getAmount();
        if (amount <= 1) {
            player.getInventory().removeItem(item);
        } else {
            item.setAmount(amount - 1);
        }
    }
}
