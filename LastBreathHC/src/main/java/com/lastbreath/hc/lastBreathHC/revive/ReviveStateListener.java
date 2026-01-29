package com.lastbreath.hc.lastBreathHC.revive;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;

import com.lastbreath.hc.lastBreathHC.heads.HeadManager;

import java.util.UUID;

public class ReviveStateListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!ReviveStateManager.isRevivePending(uuid)) {
            return;
        }

        if (HeadManager.hasPendingRestore(uuid)) {
            Inventory storedInventory = HeadManager.getPendingRestore(uuid);
            if (storedInventory != null) {
                player.getEnderChest().setContents(storedInventory.getContents());
            }
            HeadManager.removePendingRestore(uuid);
        }

        player.setGameMode(GameMode.SURVIVAL);
        ReviveStateManager.clearRevivePending(uuid);
    }
}
