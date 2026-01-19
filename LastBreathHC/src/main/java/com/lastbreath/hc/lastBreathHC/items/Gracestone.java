package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class Gracestone {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "gracestone");
    private static final NamespacedKey ACTIVE_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "gracestone_active");

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.END_STONE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§fGracestone");
        meta.setLore(List.of(
                "§7Grants a single normal life.",
                "§7Hardcore rules return on death."
        ));

        meta.getPersistentDataContainer().set(
                KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isGracestone(ItemStack item) {
        return item != null &&
                item.getType() == Material.END_STONE &&
                item.hasItemMeta() &&
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .has(KEY, PersistentDataType.BYTE);
    }

    public static boolean isGraceActive(Player player) {
        return player.getPersistentDataContainer()
                .has(ACTIVE_KEY, PersistentDataType.BYTE);
    }

    public static void activateGrace(Player player) {
        player.getPersistentDataContainer().set(
                ACTIVE_KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );
    }

    public static void clearGrace(Player player) {
        player.getPersistentDataContainer().remove(ACTIVE_KEY);
    }
}
