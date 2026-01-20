package com.lastbreath.hc.lastBreathHC.stats;

import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
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

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        PlayerStats stats = StatsManager.get(killer.getUniqueId());
        stats.mobsKilled++;
        TitleManager.checkProgressTitles(killer);
    }
}
