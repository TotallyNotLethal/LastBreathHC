package com.lastbreath.hc.lastBreathHC.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.UUID;

public final class ChatInventoryShareService {

    public static final String PERMISSION = "lastbreathhc.showinv";
    private static final int INVENTORY_SIZE = 54;
    private static final int ENDER_CHEST_SIZE = 27;
    private static final int ITEM_SIZE = 9;

    private ChatInventoryShareService() {
    }

    public static boolean canShare(Player player) {
        return player.hasPermission(PERMISSION);
    }

    public static ShareType parseType(String token) {
        if (token == null) {
            return null;
        }
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "inv", "inventory" -> ShareType.INVENTORY;
            case "ec", "enderchest" -> ShareType.ENDERCHEST;
            case "item" -> ShareType.ITEM;
            default -> null;
        };
    }

    public static void openShare(Player viewer, Player target, ShareType type) {
        if (viewer == null || target == null || type == null) {
            return;
        }
        Inventory inventory = switch (type) {
            case INVENTORY -> createInventoryView(target);
            case ENDERCHEST -> createEnderChestView(target);
            case ITEM -> createItemView(target);
        };
        if (inventory == null) {
            return;
        }
        viewer.openInventory(inventory);
        viewer.sendMessage(Component.text("Viewing " + target.getName() + "'s " + type.label(), NamedTextColor.AQUA));
    }

    private static Inventory createInventoryView(Player target) {
        ShareInventoryHolder holder = new ShareInventoryHolder(target.getUniqueId(), ShareType.INVENTORY);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, target.getName() + "'s Inventory");
        holder.setInventory(inventory);

        ItemStack[] storage = target.getInventory().getStorageContents();
        for (int i = 0; i < storage.length && i < 36; i++) {
            inventory.setItem(i, cloneItem(storage[i]));
        }

        ItemStack[] armor = target.getInventory().getArmorContents();
        if (armor.length >= 4) {
            inventory.setItem(45, cloneItem(armor[3]));
            inventory.setItem(46, cloneItem(armor[2]));
            inventory.setItem(47, cloneItem(armor[1]));
            inventory.setItem(48, cloneItem(armor[0]));
        }

        return inventory;
    }

    private static Inventory createEnderChestView(Player target) {
        ShareInventoryHolder holder = new ShareInventoryHolder(target.getUniqueId(), ShareType.ENDERCHEST);
        Inventory inventory = Bukkit.createInventory(holder, ENDER_CHEST_SIZE, target.getName() + "'s Ender Chest");
        holder.setInventory(inventory);

        ItemStack[] contents = target.getEnderChest().getContents();
        for (int i = 0; i < contents.length && i < ENDER_CHEST_SIZE; i++) {
            inventory.setItem(i, cloneItem(contents[i]));
        }

        return inventory;
    }

    private static Inventory createItemView(Player target) {
        ShareInventoryHolder holder = new ShareInventoryHolder(target.getUniqueId(), ShareType.ITEM);
        Inventory inventory = Bukkit.createInventory(holder, ITEM_SIZE, target.getName() + "'s Item");
        holder.setInventory(inventory);

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            inventory.setItem(4, createEmptyPlaceholder());
        } else {
            inventory.setItem(4, cloneItem(item));
        }

        return inventory;
    }

    private static ItemStack createEmptyPlaceholder() {
        ItemStack placeholder = new ItemStack(Material.BARRIER);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Empty", NamedTextColor.RED));
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    public enum ShareType {
        ITEM("item"),
        INVENTORY("inventory"),
        ENDERCHEST("ender chest");

        private final String label;

        ShareType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static final class ShareInventoryHolder implements InventoryHolder {
        private final UUID owner;
        private final ShareType type;
        private Inventory inventory;

        public ShareInventoryHolder(UUID owner, ShareType type) {
            this.owner = owner;
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public UUID getOwner() {
            return owner;
        }

        public ShareType getType() {
            return type;
        }
    }
}
