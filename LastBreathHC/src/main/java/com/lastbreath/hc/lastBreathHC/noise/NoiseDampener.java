package com.lastbreath.hc.lastBreathHC.noise;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class NoiseDampener {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "noise_dampener");

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§bNoise Dampener");
        meta.setLore(List.of(
                "§7An enchanted piece of wool.",
                "§7Right-click to tune nearby sounds.",
                "§7Dampens selected noises in a 10x10 area."
        ));

        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(
                KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isNoiseDampener(ItemStack item) {
        return item != null &&
                item.getType() == Material.WHITE_WOOL &&
                item.hasItemMeta() &&
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .has(KEY, PersistentDataType.BYTE);
    }
}
