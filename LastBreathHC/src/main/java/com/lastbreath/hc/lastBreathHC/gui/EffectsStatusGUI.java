package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectManager;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectRegistry;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EffectsStatusGUI implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final String TITLE = "Effect Status";

    private final CustomPotionEffectManager effectManager;
    private final CustomPotionEffectRegistry effectRegistry;

    public EffectsStatusGUI(CustomPotionEffectManager effectManager, CustomPotionEffectRegistry effectRegistry) {
        this.effectManager = effectManager;
        this.effectRegistry = effectRegistry;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, TITLE);

        inventory.setItem(4, buildTitleItem(player));
        List<ItemStack> effectItems = buildEffectItems(player);
        int slot = 9;
        for (ItemStack item : effectItems) {
            if (slot >= INVENTORY_SIZE) {
                break;
            }
            inventory.setItem(slot++, item);
        }
        if (effectItems.isEmpty()) {
            inventory.setItem(22, buildEmptyItem());
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
    }

    private ItemStack buildTitleItem(Player player) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        Title title = TitleManager.getEquippedTitle(player);
        String titleName = title == null ? "None" : title.displayName();
        meta.setDisplayName(ChatColor.GOLD + "Equipped Title" );
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Title: " + ChatColor.WHITE + titleName);
        List<String> titleEffects = TitleManager.getTitleEffects(title);
        if (titleEffects.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "No title effects configured yet.");
        } else {
            lore.add(ChatColor.GRAY + "Title Effects:");
            titleEffects.forEach(effect -> lore.add(ChatColor.GREEN + "- " + effect));
        }
        lore.add(ChatColor.GRAY + "Stacks with active potion effects.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<ItemStack> buildEffectItems(Player player) {
        Map<String, Long> active = effectManager.getActiveEffectRemainingMillis(player);
        List<ItemStack> items = new ArrayList<>();
        active.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> items.add(buildEffectItem(entry.getKey(), entry.getValue())));
        return items;
    }

    private ItemStack buildEffectItem(String effectId, long remainingMillis) {
        CustomPotionEffectRegistry.CustomPotionEffectDefinition definition = effectRegistry.getById(effectId);
        Material material = Material.POTION;
        String displayName = effectId;
        String description = "";
        if (definition != null) {
            displayName = definition.displayName();
            description = definition.description();
            material = definition.category() == com.lastbreath.hc.lastBreathHC.potion.CustomEffectCategory.NEGATIVE
                    ? Material.REDSTONE : Material.EMERALD;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + displayName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "ID: " + ChatColor.WHITE + effectId);
        if (!description.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + description);
        }
        long seconds = Math.max(0, remainingMillis / 1000);
        lore.add(ChatColor.GRAY + "Remaining: " + ChatColor.WHITE + seconds + "s");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildEmptyItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "No active custom effects");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Drink a potion or use /effects give.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
