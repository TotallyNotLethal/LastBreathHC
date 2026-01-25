package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class EnhancedGrindstone {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "enhanced_grindstone");

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.GRINDSTONE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§dEnhanced Grindstone");
        meta.setLore(List.of(
                "§7Place to open a custom anvil interface.",
                "§7Imprint enchants onto books."
        ));

        meta.getPersistentDataContainer().set(
                KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isEnhancedGrindstone(ItemStack item) {
        return item != null &&
                item.getType() == Material.GRINDSTONE &&
                item.hasItemMeta() &&
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .has(KEY, PersistentDataType.BYTE);
    }
}
