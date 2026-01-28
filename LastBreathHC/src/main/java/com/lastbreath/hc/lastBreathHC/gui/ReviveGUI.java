package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.death.DeathListener;
import com.lastbreath.hc.lastBreathHC.revive.ReviveStateManager;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import com.lastbreath.hc.lastBreathHC.token.ReviveTokenHelper;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ReviveGUI implements Listener {

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "Use Revival Token?");

        ItemStack yes = new ItemStack(Material.LIME_WOOL);
        ItemMeta yMeta = yes.getItemMeta();
        yMeta.setDisplayName("§aYES — Use Token");
        yes.setItemMeta(yMeta);

        ItemStack no = new ItemStack(Material.RED_WOOL);
        ItemMeta nMeta = no.getItemMeta();
        nMeta.setDisplayName("§cNO — Accept Death");
        no.setItemMeta(nMeta);

        inv.setItem(3, yes);
        inv.setItem(5, no);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("Use Revival Token?")) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getType() == Material.LIME_WOOL) {
            if (!ReviveTokenHelper.consumeToken(player)) {
                player.sendMessage("§cNo revive token found to consume.");
                DeathListener.banPlayer(player, "Revival token missing at time of death.", null);
                return;
            }
            PlayerStats stats = StatsManager.get(player.getUniqueId());
            stats.revives++;
            TitleManager.unlockTitle(player, Title.REVIVED, "You returned from the brink.");
            if (stats.revives >= 3) {
                TitleManager.unlockTitle(player, Title.SOUL_RECLAIMER, "You have reclaimed your soul multiple times.");
            }

            player.setGameMode(GameMode.SURVIVAL);
            ReviveStateManager.markRevivePending(player.getUniqueId());
            player.teleport(
                    player.getBedSpawnLocation() != null
                            ? player.getBedSpawnLocation()
                            : player.getWorld().getSpawnLocation()
            );

            Bukkit.broadcastMessage(
                    "§6⚡ " + TitleManager.getTitleTag(player) + player.getName() + " defied death!"
            );

            player.closeInventory();
        } else {
            DeathListener.banPlayer(player, "Player declined revival.", null);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getView().getTitle().equals("Use Revival Token?")) {
            if (!ReviveStateManager.isRevivePending(p.getUniqueId())) {
                DeathListener.banPlayer(p, "Player closed revive menu.", null);
            }
        }
    }
}
