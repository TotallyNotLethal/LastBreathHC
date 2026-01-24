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
import java.util.Locale;

public class CustomEnchantPage {

    public static final NamespacedKey ENCHANT_ID_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "enchant_page_id");

    private CustomEnchantPage() {
    }

    public static ItemStack create(String enchantId) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        String displayId = formatEnchantId(enchantId);
        meta.displayName(
                Component.text("Enchant Page: " + displayId)
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false)
        );

        meta.lore(List.of(
                Component.text("Ancient knowledge etched in stardust.")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Asteroid-only treasure.")
                        .color(NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(ENCHANT_ID_KEY, PersistentDataType.STRING, enchantId);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isEnchantPage(ItemStack item) {
        return item != null
                && item.getType() == Material.PAPER
                && item.hasItemMeta()
                && item.getItemMeta()
                .getPersistentDataContainer()
                .has(ENCHANT_ID_KEY, PersistentDataType.STRING);
    }

    public static String getEnchantId(ItemStack item) {
        if (!isEnchantPage(item)) {
            return null;
        }
        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(ENCHANT_ID_KEY, PersistentDataType.STRING);
    }

    public static String formatEnchantId(String enchantId) {
        if (enchantId == null || enchantId.isBlank()) {
            return "Unknown";
        }
        String cleaned = enchantId.contains(":")
                ? enchantId.substring(enchantId.indexOf(':') + 1)
                : enchantId;
        String spaced = cleaned.replace('_', ' ');
        String[] parts = spaced.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.length() == 0 ? "Unknown" : builder.toString();
    }
}
