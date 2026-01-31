package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CustomEnchantments {

    private static final NamespacedKey ENCHANTS_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "custom_enchants");

    private static final Component LORE_HEADER = Component.text("Custom Enchants")
            .color(NamedTextColor.DARK_PURPLE)
            .decoration(TextDecoration.ITALIC, false);

    private CustomEnchantments() {
    }

    public static ItemStack applyEnchant(ItemStack tool, String enchantId) {
        if (tool == null || enchantId == null || enchantId.isBlank()) {
            return tool;
        }
        if (!CustomEnchant.isAllowedEnchantId(enchantId)) {
            return tool;
        }
        if (!CustomEnchant.isAllowedForItem(enchantId, tool.getType())) {
            return tool;
        }
        ItemStack result = tool.clone();
        ItemMeta meta = result.getItemMeta();
        List<String> ids = new ArrayList<>(getEnchantIds(meta));
        if (!ids.contains(enchantId)) {
            ids.add(enchantId);
        }
        updateEnchantData(meta, ids);
        updateLore(meta, ids);
        result.setItemMeta(meta);
        return result;
    }

    public static List<String> getEnchantIds(ItemMeta meta) {
        if (meta == null) {
            return List.of();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String raw = container.get(ENCHANTS_KEY, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] parts = raw.split(",");
        Set<String> ids = new LinkedHashSet<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                ids.add(trimmed);
            }
        }
        return List.copyOf(ids);
    }

    private static void updateEnchantData(ItemMeta meta, List<String> ids) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (ids.isEmpty()) {
            container.remove(ENCHANTS_KEY);
        } else {
            container.set(ENCHANTS_KEY, PersistentDataType.STRING, String.join(",", ids));
        }
    }

    private static void updateLore(ItemMeta meta, List<String> ids) {
        List<Component> lore = meta.lore();
        List<Component> updated = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
        updated.removeIf(CustomEnchantments::isCustomEnchantLore);
        if (!ids.isEmpty()) {
            updated.add(LORE_HEADER);
            for (String id : ids) {
                updated.add(
                        Component.text("• " + CustomEnchantPage.formatEnchantId(id))
                                .color(NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, false)
                );
            }
        }
        meta.lore(updated.isEmpty() ? null : updated);
    }

    private static boolean isCustomEnchantLore(Component component) {
        if (component instanceof TextComponent textComponent) {
            String content = textComponent.content().toLowerCase(Locale.ROOT);
            return content.equals("custom enchants") || content.startsWith("• ");
        }
        return false;
    }
}
