package com.lastbreath.hc.lastBreathHC.bounty;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class BountyListener implements Listener {

    private static final Duration LOGOUT_EXPIRATION = Duration.ofDays(30);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        BountyRecord record = BountyManager.getBounty(uuid);
        if (record == null) {
            return;
        }

        record.targetName = player.getName();
        if (record.lastLogoutInstant != null) {
            Instant cutoff = Instant.now().minus(LOGOUT_EXPIRATION);
            if (record.lastLogoutInstant.isBefore(cutoff)) {
                BountyManager.removeBounty(uuid, "Removed after 30 days of inactivity.");
                return;
            }
        }

        BountyManager.save();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        BountyRecord record = BountyManager.getBounty(uuid);
        if (record == null) {
            return;
        }

        record.lastLogoutInstant = Instant.now();
        BountyManager.save();
    }
}
