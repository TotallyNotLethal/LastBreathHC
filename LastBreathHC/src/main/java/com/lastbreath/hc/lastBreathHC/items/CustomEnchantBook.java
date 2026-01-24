package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class CustomEnchantBook {

    public static final NamespacedKey ENCHANT_ID_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "enchant_book_id");

    private CustomEnchantBook() {
    }

    public static ItemStack create(String enchantId) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        String displayId = CustomEnchantPage.formatEnchantId(enchantId);
        meta.displayName(
                Component.text("Bound Enchant Tome: " + displayId)
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false)
        );

        meta.lore(List.of(
                Component.text("Six pages bound in starlight.")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Use in an anvil with netherite gear.")
                        .color(NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(ENCHANT_ID_KEY, PersistentDataType.STRING, enchantId);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isEnchantBook(ItemStack item) {
        return item != null
                && item.getType() == Material.BOOK
                && item.hasItemMeta()
                && item.getItemMeta()
                .getPersistentDataContainer()
                .has(ENCHANT_ID_KEY, PersistentDataType.STRING);
    }

    public static String getEnchantId(ItemStack item) {
        if (!isEnchantBook(item)) {
            return null;
        }
        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(ENCHANT_ID_KEY, PersistentDataType.STRING);
    }
}
