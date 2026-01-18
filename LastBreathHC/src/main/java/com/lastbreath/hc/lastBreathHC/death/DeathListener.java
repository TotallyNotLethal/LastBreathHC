package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.gui.ReviveGUI;
import com.lastbreath.hc.lastBreathHC.token.ReviveToken;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Stop vanilla behavior
        event.setDeathMessage(null);
        event.getDrops().clear();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (hasReviveToken(player)) {
                    triggerReviveFlow(player);
                } else {
                    banPlayer(player, "You died with no revival token.");
                }
            }
        }.runTaskLater(LastBreathHC.getInstance(), 1L);
    }

    private boolean hasReviveToken(Player player) {
        for (ItemStack item : player.getInventory().getContents())
            if (ReviveToken.isToken(item)) return true;

        for (ItemStack item : player.getEnderChest().getContents())
            if (ReviveToken.isToken(item)) return true;

        return false;
    }

    private void triggerReviveFlow(Player player) {
        player.setHealth(1.0);
        player.setGameMode(GameMode.SPECTATOR);

        // Global boom
        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_DEATH,
                1.5f,
                0.6f
        );

        Bukkit.broadcastMessage(
                "§4☠ " + player.getName() + " has fallen..."
        );

        ReviveGUI.open(player);
    }

    public static void banPlayer(Player player, String reason) {
        Bukkit.broadcastMessage(
                "§4☠ " + player.getName() + " has perished permanently."
        );

        Bukkit.getBanList(BanList.Type.NAME)
                .addBan(player.getName(), reason, null, null);

        player.kickPlayer("You have died.\nNo revival token was used.");
    }
}
