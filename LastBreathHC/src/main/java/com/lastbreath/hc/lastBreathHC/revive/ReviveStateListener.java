package com.lastbreath.hc.lastBreathHC.revive;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class ReviveStateListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!ReviveStateManager.isRevivePending(uuid)) {
            return;
        }

        player.setGameMode(GameMode.SURVIVAL);
        ReviveStateManager.clearRevivePending(uuid);
    }
}
