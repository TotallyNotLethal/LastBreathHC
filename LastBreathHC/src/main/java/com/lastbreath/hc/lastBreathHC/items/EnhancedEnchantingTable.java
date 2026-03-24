package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class EnhancedEnchantingTable {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "enhanced_enchanting_table");
    public static final NamespacedKey BLOCKS_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "enhanced_enchanting_table_blocks");
    public static final int GUARANTEED_ENCHANT_COST = 50;

    private EnhancedEnchantingTable() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§dEnhanced Enchanting Table");
        meta.setLore(List.of(
                "§7Bottom offer guarantees a legal enchant",
                "§7from nearby chiseled bookshelf books.",
                "§7Costs " + GUARANTEED_ENCHANT_COST + " levels."
        ));

        meta.getPersistentDataContainer().set(
                KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isEnhancedEnchantingTable(ItemStack item) {
        return item != null
                && item.getType() == Material.ENCHANTING_TABLE
                && item.hasItemMeta()
                && item.getItemMeta()
                .getPersistentDataContainer()
                .has(KEY, PersistentDataType.BYTE);
    }
}
