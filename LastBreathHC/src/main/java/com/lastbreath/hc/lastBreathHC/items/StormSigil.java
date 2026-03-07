package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class StormSigil {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "storm_sigil");

    private StormSigil() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§bStorm Sigil");
        meta.setLore(List.of(
                "§7A sigil pulsing with lightning.",
                "§7Required for Gracestone forging."
        ));

        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isStormSigil(ItemStack item) {
        return item != null
                && item.getType() == Material.HEART_OF_THE_SEA
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }
}
