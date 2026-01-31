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
import java.util.stream.Collectors;

public class CustomEnchantments {

    private static final NamespacedKey ENCHANTS_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "custom_enchants");

    private static final Component LORE_HEADER = Component.text("Custom Enchants")
            .color(NamedTextColor.DARK_PURPLE)
            .decoration(TextDecoration.ITALIC, false);

    private static final Set<String> EXCLUSIVE_GUARD_ENCHANTS = Set.of(
            "lb:wither_guard",
            "lb:storm_guard",
            "lb:lifesteal_ward"
    );

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
        if (hasExclusiveConflict(ids, enchantId)) {
            logEnchantRejection("Exclusive guard enchant already present.", tool, enchantId, ids);
            return tool;
        }
        if (!ids.contains(enchantId)) {
            ids.add(enchantId);
        }
        updateEnchantData(meta, ids);
        updateLore(meta, ids);
        result.setItemMeta(meta);
        return result;
    }

    public static boolean hasExclusiveConflict(ItemStack tool, String enchantId) {
        if (tool == null) {
            return false;
        }
        return hasExclusiveConflict(getEnchantIds(tool.getItemMeta()), enchantId);
    }

    public static void logEnchantRejection(String reason, ItemStack tool, String enchantId, List<String> existingIds) {
        String itemType = tool == null ? "unknown" : tool.getType().name();
        String existingSummary = existingIds == null
                ? "none"
                : existingIds.stream()
                        .filter(CustomEnchantments::isExclusiveGuardEnchant)
                        .collect(Collectors.joining(","));
        LastBreathHC.getInstance().getLogger().fine(
                "[CustomEnchants] Rejected enchant '" + enchantId + "' on " + itemType + ": " + reason
                        + " Existing exclusive enchants: " + existingSummary
        );
    }

    private static boolean hasExclusiveConflict(List<String> existingIds, String enchantId) {
        if (!isExclusiveGuardEnchant(enchantId) || existingIds == null || existingIds.isEmpty()) {
            return false;
        }
        for (String existingId : existingIds) {
            if (isExclusiveGuardEnchant(existingId) && !existingId.equalsIgnoreCase(enchantId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExclusiveGuardEnchant(String id) {
        if (id == null) {
            return false;
        }
        return EXCLUSIVE_GUARD_ENCHANTS.contains(id.toLowerCase(Locale.ROOT));
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
