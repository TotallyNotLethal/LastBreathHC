package com.lastbreath.hc.lastBreathHC.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ChatInventoryShareService {

    public static final String PERMISSION = "lastbreathhc.showinv";
    private static final int INVENTORY_SIZE = 54;
    private static final int ENDER_CHEST_SIZE = 27;
    private static final int ITEM_SIZE = 27;
    private static final int SHULKER_VIEW_SIZE = 36;
    private static final int BACK_BUTTON_SLOT = 31;
    private static final int MAIN_OFFHAND_SLOT = 0;
    private static final int[] ARMOR_SLOTS = {5, 6, 7, 8};
    private static final int[] STORAGE_SLOTS = {
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final Material FILLER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;

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
        openShare(viewer, target, type, null);
    }

    public static void openShare(Player viewer, Player target, ShareType type, Component contextMessage) {
        if (viewer == null || target == null || type == null) {
            return;
        }
        Inventory inventory = createRootInventory(target, type);
        if (inventory == null) {
            return;
        }
        viewer.openInventory(inventory);
        if (contextMessage == null) {
            viewer.sendMessage(Component.text("Viewing " + target.getName() + "'s " + type.label(), NamedTextColor.AQUA));
            return;
        }
        viewer.sendMessage(contextMessage);
    }

    public static boolean isShareInventory(InventoryHolder holder) {
        return holder instanceof ShareInventoryHolder;
    }

    public static void handleInventoryClick(Player viewer, ShareInventoryHolder holder, int rawSlot, ItemStack clickedItem) {
        if (viewer == null || holder == null || rawSlot < 0 || clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        if (holder instanceof NestedShareInventoryHolder nestedHolder) {
            if (rawSlot == BACK_BUTTON_SLOT) {
                Player target = Bukkit.getPlayer(nestedHolder.getOwner());
                if (target != null && target.isOnline()) {
                    openShare(viewer, target, nestedHolder.getParentType(), Component.text("Returned to "
                            + target.getName() + "'s " + nestedHolder.getParentType().label() + ".", NamedTextColor.AQUA));
                } else {
                    viewer.closeInventory();
                    viewer.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
                }
            }
            return;
        }

        if (!(holder instanceof RootShareInventoryHolder rootHolder) || !isShulker(clickedItem)) {
            return;
        }

        Inventory nestedView = createShulkerView(rootHolder, clickedItem);
        if (nestedView == null) {
            return;
        }
        viewer.openInventory(nestedView);
    }

    private static Inventory createRootInventory(Player target, ShareType type) {
        return switch (type) {
            case INVENTORY -> createInventoryView(target);
            case ENDERCHEST -> createEnderChestView(target);
            case ITEM -> createItemView(target);
        };
    }

    private static Inventory createInventoryView(Player target) {
        RootShareInventoryHolder holder = new RootShareInventoryHolder(target.getUniqueId(), ShareType.INVENTORY);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, target.getName() + " Inventory");
        holder.setInventory(inventory);

        fillInventory(inventory, fillerPane());

        ItemStack[] storage = target.getInventory().getStorageContents();
        for (int i = 9; i < storage.length && i < 36; i++) {
            inventory.setItem(STORAGE_SLOTS[i - 9], cloneItem(storage[i]));
        }
        for (int i = 0; i < storage.length && i < 9; i++) {
            inventory.setItem(STORAGE_SLOTS[27 + i], cloneItem(storage[i]));
        }

        inventory.setItem(MAIN_OFFHAND_SLOT, cloneItem(target.getInventory().getItemInOffHand()));

        ItemStack[] armor = target.getInventory().getArmorContents();
        if (armor.length >= 4) {
            inventory.setItem(ARMOR_SLOTS[0], cloneItem(armor[0]));
            inventory.setItem(ARMOR_SLOTS[1], cloneItem(armor[1]));
            inventory.setItem(ARMOR_SLOTS[2], cloneItem(armor[2]));
            inventory.setItem(ARMOR_SLOTS[3], cloneItem(armor[3]));
        }

        inventory.setItem(4, createInfoItem(target.getName(), ShareType.INVENTORY,
                List.of("Storage is shown below.", "Click a shulker to inspect it.")));
        return inventory;
    }

    private static Inventory createEnderChestView(Player target) {
        RootShareInventoryHolder holder = new RootShareInventoryHolder(target.getUniqueId(), ShareType.ENDERCHEST);
        Inventory inventory = Bukkit.createInventory(holder, ENDER_CHEST_SIZE, target.getName() + " Ender Chest");
        holder.setInventory(inventory);

        ItemStack[] contents = target.getEnderChest().getContents();
        for (int i = 0; i < contents.length && i < ENDER_CHEST_SIZE; i++) {
            inventory.setItem(i, cloneItem(contents[i]));
        }

        return inventory;
    }

    private static Inventory createItemView(Player target) {
        RootShareInventoryHolder holder = new RootShareInventoryHolder(target.getUniqueId(), ShareType.ITEM);
        Inventory inventory = Bukkit.createInventory(holder, ITEM_SIZE, target.getName() + " Item");
        holder.setInventory(inventory);

        fillInventory(inventory, fillerPane());

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            inventory.setItem(13, createEmptyPlaceholder());
        } else {
            inventory.setItem(13, cloneItem(item));
        }

        inventory.setItem(4, createInfoItem(target.getName(), ShareType.ITEM,
                List.of("Main hand item preview.", "Click a shulker to inspect it.")));
        return inventory;
    }

    private static Inventory createShulkerView(RootShareInventoryHolder parentHolder, ItemStack shulkerItem) {
        ShulkerBox shulkerBox = getShulkerBox(shulkerItem);
        if (shulkerBox == null) {
            return null;
        }

        NestedShareInventoryHolder holder = new NestedShareInventoryHolder(parentHolder.getOwner(), parentHolder.getType());
        Inventory inventory = Bukkit.createInventory(holder, SHULKER_VIEW_SIZE, buildShulkerTitle(shulkerItem, parentHolder.getType()));
        holder.setInventory(inventory);

        ItemStack[] contents = shulkerBox.getInventory().getContents();
        for (int i = 0; i < 27 && i < contents.length; i++) {
            inventory.setItem(i, cloneItem(contents[i]));
        }

        for (int slot = 27; slot < SHULKER_VIEW_SIZE; slot++) {
            inventory.setItem(slot, fillerPane());
        }
        inventory.setItem(BACK_BUTTON_SLOT, createBackButton(parentHolder.getType()));
        return inventory;
    }

    private static String buildShulkerTitle(ItemStack shulkerItem, ShareType parentType) {
        String itemName = shulkerItem.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String titleName = Character.toUpperCase(itemName.charAt(0)) + itemName.substring(1);
        return titleName + " • " + parentType.label();
    }

    private static ItemStack createInfoItem(String targetName, ShareType type, List<String> extraLore) {
        ItemStack info = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(targetName + " " + type.label(), NamedTextColor.GOLD));
            meta.lore(List.of(
                    Component.text("Read-only preview.", NamedTextColor.GRAY),
                    Component.text(extraLore.get(0), NamedTextColor.DARK_GRAY),
                    Component.text(extraLore.get(1), NamedTextColor.DARK_GRAY)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            info.setItemMeta(meta);
        }
        return info;
    }

    private static ItemStack createBackButton(ShareType parentType) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Back", NamedTextColor.GOLD));
            meta.lore(List.of(Component.text("Return to the shared " + parentType.label() + ".", NamedTextColor.GRAY)));
            back.setItemMeta(meta);
        }
        return back;
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

    private static ItemStack fillerPane() {
        ItemStack filler = new ItemStack(FILLER_MATERIAL);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
            filler.setItemMeta(meta);
        }
        return filler;
    }

    private static void fillInventory(Inventory inventory, ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item.clone());
        }
    }

    private static boolean isShulker(ItemStack item) {
        return getShulkerBox(item) != null;
    }

    private static ShulkerBox getShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        if (!(item.getItemMeta() instanceof BlockStateMeta meta) || !(meta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return null;
        }
        return shulkerBox;
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

    public abstract static class ShareInventoryHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        protected ShareInventoryHolder(UUID owner) {
            this.owner = owner;
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
    }

    public static final class RootShareInventoryHolder extends ShareInventoryHolder {
        private final ShareType type;

        public RootShareInventoryHolder(UUID owner, ShareType type) {
            super(owner);
            this.type = type;
        }

        public ShareType getType() {
            return type;
        }
    }

    public static final class NestedShareInventoryHolder extends ShareInventoryHolder {
        private final ShareType parentType;

        public NestedShareInventoryHolder(UUID owner, ShareType parentType) {
            super(owner);
            this.parentType = parentType;
        }

        public ShareType getParentType() {
            return parentType;
        }
    }
}
