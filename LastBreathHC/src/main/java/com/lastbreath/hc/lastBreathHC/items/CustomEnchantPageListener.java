package com.lastbreath.hc.lastBreathHC.items;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

public class CustomEnchantPageListener implements Listener {

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        if (left == null || right == null) {
            return;
        }
        if (!CustomEnchantBook.isEnchantBook(right)) {
            return;
        }
        if (!CustomEnchantments.isNetheriteTool(left.getType())) {
            event.setResult(null);
            return;
        }

        String enchantId = CustomEnchantBook.getEnchantId(right);
        if (enchantId == null || enchantId.isBlank()) {
            event.setResult(null);
            return;
        }

        ItemStack result = CustomEnchantments.applyEnchant(left, enchantId);
        event.setResult(result);

        AnvilView view = (AnvilView) event.getView();
        view.setRepairCost(1);
    }
}
