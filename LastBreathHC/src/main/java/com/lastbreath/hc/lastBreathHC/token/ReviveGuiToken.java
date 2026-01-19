package com.lastbreath.hc.lastBreathHC.token;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ReviveGuiToken {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "revive_gui_token");

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§bRevival Name Token");
        meta.setLore(List.of(
                "§7Right-click to revive a name.",
                "§cConsumed on successful revive."
        ));

        meta.getPersistentDataContainer().set(
                KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isToken(ItemStack item) {
        return item != null &&
                item.getType() == Material.NETHER_STAR &&
                item.hasItemMeta() &&
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .has(KEY, PersistentDataType.BYTE);
    }
}
