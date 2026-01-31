package com.lastbreath.hc.lastBreathHC.listeners;

import com.lastbreath.hc.lastBreathHC.items.CustomEnchant;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantBook;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantments;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

import java.util.Map;

public class CustomEnchantAnvilListener implements Listener {

    private static final int RESULT_SLOT = 2;
    private static final int LEVEL_COST = 30;

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
        String enchantId = CustomEnchantBook.getEnchantId(right);
        if (!CustomEnchant.isAllowedEnchantId(enchantId)) {
            event.setResult(null);
            return;
        }
        if (!CustomEnchant.isAllowedForItem(enchantId, left.getType())) {
            event.setResult(null);
            return;
        }
        if (CustomEnchantments.hasExclusiveConflict(left, enchantId)) {
            CustomEnchantments.logEnchantRejection(
                    "Exclusive guard enchant already present.",
                    left,
                    enchantId,
                    CustomEnchantments.getEnchantIds(left.getItemMeta())
            );
            event.setResult(null);
            return;
        }

        ItemStack result = CustomEnchantments.applyEnchant(left, enchantId);
        event.setResult(result);

        AnvilView view = (AnvilView) event.getView();
        view.setRepairCost(LEVEL_COST);
        view.setMaximumRepairCost(LEVEL_COST);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory inventory)) {
            return;
        }
        if (event.getRawSlot() != RESULT_SLOT) {
            return;
        }
        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        if (left == null || right == null) {
            return;
        }
        if (!CustomEnchantBook.isEnchantBook(right)) {
            return;
        }
        String enchantId = CustomEnchantBook.getEnchantId(right);
        if (!CustomEnchant.isAllowedEnchantId(enchantId)) {
            event.setCancelled(true);
            return;
        }
        if (!CustomEnchant.isAllowedForItem(enchantId, left.getType())) {
            event.setCancelled(true);
            return;
        }
        if (CustomEnchantments.hasExclusiveConflict(left, enchantId)) {
            CustomEnchantments.logEnchantRejection(
                    "Exclusive guard enchant already present.",
                    left,
                    enchantId,
                    CustomEnchantments.getEnchantIds(left.getItemMeta())
            );
            event.setCancelled(true);
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isCreative(player) && player.getLevel() < LEVEL_COST) {
            event.setCancelled(true);
            return;
        }

        if (!event.isShiftClick()) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                return;
            }
        }

        ItemStack result = CustomEnchantments.applyEnchant(left, enchantId);
        if (result == null || result.getType() == Material.AIR) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (event.isShiftClick()) {
            Map<Integer, ItemStack> remaining = player.getInventory().addItem(result);
            if (!remaining.isEmpty()) {
                for (ItemStack leftover : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        } else {
            event.setCursor(result);
        }

        if (!isCreative(player)) {
            player.giveExpLevels(-LEVEL_COST);
        }

        inventory.setItem(0, null);
        consumeBook(inventory, right);
        inventory.setItem(RESULT_SLOT, null);
    }

    private boolean isCreative(Player player) {
        return player.getGameMode() == GameMode.CREATIVE;
    }

    private void consumeBook(AnvilInventory inventory, ItemStack book) {
        if (book.getAmount() <= 1) {
            inventory.setItem(1, null);
            return;
        }
        ItemStack updated = book.clone();
        updated.setAmount(book.getAmount() - 1);
        inventory.setItem(1, updated);
    }
}
