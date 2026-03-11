package com.lastbreath.hc.lastBreathHC.asteroid;

import com.lastbreath.hc.lastBreathHC.items.CustomEnchant;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantBook;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantPage;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AsteroidLootBoxGUI implements Listener {

    private static final Component GUI_TITLE = Component.text("Asteroid Loot Box", NamedTextColor.DARK_PURPLE);
    private static final int GUI_SIZE = 54;

    private static final Map<UUID, Session> ACTIVE_SESSIONS = new HashMap<>();

    private record Session(Inventory inventory, boolean claimed) {
        Session withClaimed(boolean newClaimed) {
            return new Session(inventory, newClaimed);
        }
    }

    public static void tryOpen(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        int earnedBoxes = stats.asteroidLoots / 100;
        int remaining = earnedBoxes - Math.max(stats.asteroidLootBoxClaims, 0);
        if (remaining <= 0) {
            return;
        }

        Session existing = ACTIVE_SESSIONS.get(player.getUniqueId());
        if (existing != null) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        fillInventory(inventory);
        ACTIVE_SESSIONS.put(player.getUniqueId(), new Session(inventory, false));

        player.sendMessage("§d§lLoot Box §7» §fYou unlocked a loot box! Pick one custom enchant book.");
        player.openInventory(inventory);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(com.lastbreath.hc.lastBreathHC.LastBreathHC.getInstance(),
                () -> tryOpen(player),
                20L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLootBoxClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Session session = ACTIVE_SESSIONS.get(player.getUniqueId());
        if (session == null || !event.getView().getTopInventory().equals(session.inventory())) {
            return;
        }

        event.setCancelled(true);

        if (session.claimed()) {
            return;
        }

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (!CustomEnchantBook.isEnchantBook(clicked)) {
            return;
        }

        String enchantId = CustomEnchantBook.getEnchantId(clicked);
        if (!CustomEnchant.isAllowedEnchantId(enchantId)) {
            return;
        }

        PlayerStats stats = StatsManager.get(player.getUniqueId());
        int earnedBoxes = stats.asteroidLoots / 100;
        if (stats.asteroidLootBoxClaims >= earnedBoxes) {
            player.sendMessage("§cYou do not have any unclaimed asteroid loot boxes.");
            player.closeInventory();
            return;
        }

        stats.asteroidLootBoxClaims++;
        StatsManager.markDirty(player.getUniqueId());
        ACTIVE_SESSIONS.put(player.getUniqueId(), session.withClaimed(true));

        ItemStack reward = CustomEnchantBook.create(enchantId);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward);
        if (!overflow.isEmpty()) {
            for (ItemStack item : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        player.sendMessage("§aYou selected §f" + CustomEnchantPage.formatEnchantId(enchantId) + "§a and received the enchant book.");
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(com.lastbreath.hc.lastBreathHC.LastBreathHC.getInstance(),
                () -> tryOpen(player),
                1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLootBoxDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Session session = ACTIVE_SESSIONS.get(player.getUniqueId());
        if (session == null || !event.getView().getTopInventory().equals(session.inventory())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onLootBoxClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Session session = ACTIVE_SESSIONS.get(player.getUniqueId());
        if (session == null || !event.getInventory().equals(session.inventory())) {
            return;
        }
        ACTIVE_SESSIONS.remove(player.getUniqueId());
    }

    private static void fillInventory(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, fillerPane());
        }

        List<CustomEnchant> enchants = List.of(CustomEnchant.values());
        int slot = 10;
        for (CustomEnchant enchant : enchants) {
            if (enchant.isPvp()) {
                continue;
            }
            while (slot < inventory.getSize() && (slot % 9 == 0 || slot % 9 == 8)) {
                slot++;
            }
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot, CustomEnchantBook.create(enchant.getId()));
            slot++;
        }
    }

    private static ItemStack fillerPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.BLACK).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
