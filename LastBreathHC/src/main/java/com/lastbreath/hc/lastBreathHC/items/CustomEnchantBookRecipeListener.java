package com.lastbreath.hc.lastBreathHC.items;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class CustomEnchantBookRecipeListener implements Listener {

    private static final int[] STAR_SLOTS = {0, 3, 6};
    private static final int[] PAGE_SLOTS = {1, 2, 4, 5, 7, 8};

    @EventHandler(ignoreCancelled = true)
    public void onPrepareEnchantBookCraft(PrepareItemCraftEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }

        ItemStack[] matrix = inventory.getMatrix();
        if (!matchesPattern(matrix)) {
            return;
        }

        String enchantId = extractEnchantId(matrix);
        if (enchantId == null || enchantId.isBlank()) {
            inventory.setResult(null);
            return;
        }

        inventory.setResult(CustomEnchantBook.create(enchantId));
    }

    private boolean matchesPattern(ItemStack[] matrix) {
        if (matrix.length < 9) {
            return false;
        }
        for (int slot : STAR_SLOTS) {
            ItemStack item = matrix[slot];
            if (item == null || item.getType() != Material.NETHER_STAR) {
                return false;
            }
        }
        for (int slot : PAGE_SLOTS) {
            ItemStack item = matrix[slot];
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private String extractEnchantId(ItemStack[] matrix) {
        String enchantId = null;
        for (int slot : PAGE_SLOTS) {
            ItemStack item = matrix[slot];
            if (!CustomEnchantPage.isEnchantPage(item)) {
                return null;
            }
            String pageId = CustomEnchantPage.getEnchantId(item);
            if (pageId == null || pageId.isBlank()) {
                return null;
            }
            if (enchantId == null) {
                enchantId = pageId;
            } else if (!Objects.equals(enchantId, pageId)) {
                return null;
            }
        }
        return enchantId;
    }
}
