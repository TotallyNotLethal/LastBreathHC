package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.token.ReviveGuiToken;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReviveNameGUI implements Listener {

    private static final Map<UUID, Boolean> pendingEntries = new ConcurrentHashMap<>();

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!ReviveGuiToken.isToken(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (pendingEntries.putIfAbsent(player.getUniqueId(), Boolean.TRUE) != null) {
            player.sendMessage("§cYou already have a revive prompt open.");
            return;
        }

        player.sendMessage("§eType the player name to revive in chat.");
        player.sendMessage("§7Type \"cancel\" to abort.");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!pendingEntries.containsKey(playerId)) {
            return;
        }

        event.setCancelled(true);
        String input = event.getMessage().trim();
        pendingEntries.remove(playerId);

        if (input.isBlank() || input.equalsIgnoreCase("cancel")) {
            event.getPlayer().sendMessage("§7Revive cancelled.");
            return;
        }

        Bukkit.getScheduler().runTask(
                LastBreathHC.getInstance(),
                () -> handleRevive(event.getPlayer(), input)
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingEntries.remove(event.getPlayer().getUniqueId());
    }

    private void handleRevive(Player player, String inputName) {
        String resolvedName = resolvePlayerName(inputName);
        if (resolvedName == null) {
            player.sendMessage("§cNo known player named \"" + inputName + "\".");
            return;
        }

        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        if (!banList.isBanned(resolvedName)) {
            player.sendMessage("§c" + resolvedName + " is not currently banned.");
            return;
        }

        banList.pardon(resolvedName);
        if (!consumeToken(player)) {
            player.sendMessage("§cNo revive token found to consume.");
            return;
        }

        player.sendMessage("§aSuccessfully revived " + resolvedName + "!");
    }

    private String resolvePlayerName(String inputName) {
        Player online = Bukkit.getPlayerExact(inputName);
        if (online != null) {
            return online.getName();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(inputName);
        if (offline != null && offline.getName() != null) {
            return offline.getName();
        }

        return null;
    }

    private boolean consumeToken(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (!ReviveGuiToken.isToken(item)) {
                continue;
            }

            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItem(i, item);
            } else {
                player.getInventory().setItem(i, null);
            }
            return true;
        }

        return false;
    }
}
