package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class ColossusFragment {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "colossus_fragment");

    private ColossusFragment() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.ANCIENT_DEBRIS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6Colossus Fragment");
        meta.setLore(List.of(
                "§7A shard of a Hollow Colossus plate.",
                "§7Required for Gracestone forging."
        ));

        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isColossusFragment(ItemStack item) {
        return item != null
                && item.getType() == Material.ANCIENT_DEBRIS
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }
}
