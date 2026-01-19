package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ReaperStone {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "reaper_stone");

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§5Reaper Stone");
        meta.setLore(List.of(
                "§7A chilling shard tied to death.",
                "§7Used in forbidden crafting."
        ));

        meta.getPersistentDataContainer().set(
                KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isReaperStone(ItemStack item) {
        return item != null &&
                item.getType() == Material.ECHO_SHARD &&
                item.hasItemMeta() &&
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .has(KEY, PersistentDataType.BYTE);
    }
}
