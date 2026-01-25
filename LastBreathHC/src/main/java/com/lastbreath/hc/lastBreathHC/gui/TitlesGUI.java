package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class TitlesGUI implements Listener {

    private static final String TITLE = "Titles";
    private static final int INVENTORY_SIZE = 54;

    private final NamespacedKey titleKey;

    public TitlesGUI() {
        this.titleKey = new NamespacedKey(LastBreathHC.getInstance(), "title-id");
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, TITLE);
        List<Title> unlockedTitles = TitleManager.getUnlockedTitles(player);
        int slot = 0;
        for (Title title : unlockedTitles) {
            if (slot >= INVENTORY_SIZE) {
                break;
            }
            inventory.setItem(slot++, buildTitleItem(player, title));
        }
        if (unlockedTitles.isEmpty()) {
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
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || !current.hasItemMeta()) {
            return;
        }
        ItemMeta meta = current.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String titleId = container.get(titleKey, PersistentDataType.STRING);
        if (titleId == null) {
            return;
        }
        Title title = Title.fromInput(titleId);
        if (title == null) {
            return;
        }
        if (TitleManager.getEquippedTitle(player) == title) {
            player.sendMessage("§e" + title.displayName() + " is already equipped.");
            return;
        }
        if (!TitleManager.equipTitle(player, title)) {
            player.sendMessage("§cYou have not unlocked that title yet.");
            return;
        }
        player.sendMessage("§aEquipped title: " + title.displayName());
        open(player);
    }

    private ItemStack buildTitleItem(Player player, Title title) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        boolean equipped = title == TitleManager.getEquippedTitle(player);
        String displayName = equipped
                ? ChatColor.GREEN + title.displayName() + ChatColor.GRAY + " (Equipped)"
                : ChatColor.AQUA + title.displayName();
        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<>();
        List<String> effects = TitleManager.getTitleEffects(title);
        if (effects.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "No title effects configured yet.");
        } else {
            lore.add(ChatColor.GRAY + "Title Effects:");
            effects.forEach(effect -> lore.add(ChatColor.GREEN + "- " + effect));
        }
        lore.add(" ");
        lore.add(equipped
                ? ChatColor.YELLOW + "Currently equipped"
                : ChatColor.GOLD + "Click to equip");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(titleKey, PersistentDataType.STRING, title.displayName());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildEmptyItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "No titles unlocked");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Keep playing to earn titles.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
