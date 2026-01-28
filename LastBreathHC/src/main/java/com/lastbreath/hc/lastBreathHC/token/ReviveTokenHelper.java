package com.lastbreath.hc.lastBreathHC.token;

import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class ReviveTokenHelper {

    public static boolean hasToken(Player player) {
        return hasToken(player.getInventory()) || hasToken(player.getEnderChest());
    }

    public static boolean consumeToken(Player player) {
        return consumeToken(player.getInventory()) || consumeToken(player.getEnderChest());
    }

    private static boolean hasToken(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        for (ItemStack item : inventory.getContents()) {
            if (ReviveToken.isToken(item)) {
                return true;
            }
            if (hasTokenInShulker(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeToken(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (ReviveToken.isToken(item)) {
                consumeStack(inventory, slot, item);
                return true;
            }
            if (consumeTokenFromShulker(inventory, slot, item)) {
                return true;
            }
        }
        return false;
    }

    private static void consumeStack(Inventory inventory, int slot, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            inventory.setItem(slot, item);
        } else {
            inventory.setItem(slot, null);
        }
    }

    private static boolean hasTokenInShulker(ItemStack item) {
        ShulkerBox shulkerBox = getShulkerBox(item);
        if (shulkerBox == null) {
            return false;
        }

        for (ItemStack nested : shulkerBox.getInventory().getContents()) {
            if (ReviveToken.isToken(nested)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeTokenFromShulker(Inventory inventory, int slot, ItemStack item) {
        ShulkerBox shulkerBox = getShulkerBox(item);
        if (shulkerBox == null) {
            return false;
        }

        ItemStack[] contents = shulkerBox.getInventory().getContents();
        for (int index = 0; index < contents.length; index++) {
            ItemStack nested = contents[index];
            if (!ReviveToken.isToken(nested)) {
                continue;
            }

            if (nested.getAmount() > 1) {
                nested.setAmount(nested.getAmount() - 1);
                shulkerBox.getInventory().setItem(index, nested);
            } else {
                shulkerBox.getInventory().setItem(index, null);
            }

            BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
            meta.setBlockState(shulkerBox);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            return true;
        }
        return false;
    }

    private static ShulkerBox getShulkerBox(ItemStack item) {
        if (item == null) {
            return null;
        }

        if (!Tag.SHULKER_BOXES.isTagged(item.getType())) {
            return null;
        }

        if (!(item.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
            return null;
        }

        BlockState state = blockStateMeta.getBlockState();
        if (!(state instanceof ShulkerBox shulkerBox)) {
            return null;
        }
        return shulkerBox;
    }
}
