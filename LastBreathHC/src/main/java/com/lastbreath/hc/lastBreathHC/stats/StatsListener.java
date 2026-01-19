package com.lastbreath.hc.lastBreathHC.stats;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StatsListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        StatsManager.load(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        StatsManager.save(event.getPlayer().getUniqueId());
    }
}
