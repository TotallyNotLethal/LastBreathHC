package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TitleListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerStats stats = StatsManager.get(event.getPlayer().getUniqueId());
        TitleManager.initialize(stats);
        TitleManager.checkTimeBasedTitles(event.getPlayer());
        event.getPlayer().sendMessage("§7Current title: §b" + stats.equippedTitle.displayName());
    }
}
