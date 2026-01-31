package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class WorldBossPortalCompass {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "world_boss_portal_compass");

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§bWorld Boss Portal Compass");
        meta.setLore(List.of(
                "§7Points toward the nearest world boss portal.",
                "§7The needle hums with a distant pull."
        ));

        meta.getPersistentDataContainer().set(
                KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWorldBossPortalCompass(ItemStack item) {
        return item != null &&
                item.getType() == Material.COMPASS &&
                item.hasItemMeta() &&
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .has(KEY, PersistentDataType.BYTE);
    }
}
