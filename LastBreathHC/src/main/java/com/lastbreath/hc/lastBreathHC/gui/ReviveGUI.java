package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.death.DeathListener;
import com.lastbreath.hc.lastBreathHC.token.ReviveToken;
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
            consumeToken(player);

            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(
                    player.getBedSpawnLocation() != null
                            ? player.getBedSpawnLocation()
                            : player.getWorld().getSpawnLocation()
            );

            Bukkit.broadcastMessage(
                    "§6⚡ " + player.getName() + " defied death!"
            );

            player.closeInventory();
        } else {
            DeathListener.banPlayer(player, "Player declined revival.");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getView().getTitle().equals("Use Revival Token?")) {
            DeathListener.banPlayer(p, "Player closed revive menu.");
        }
    }

    private void consumeToken(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (ReviveToken.isToken(item)) {
                player.getInventory().setItem(i, null);
                return;
            }
        }
    }
}
