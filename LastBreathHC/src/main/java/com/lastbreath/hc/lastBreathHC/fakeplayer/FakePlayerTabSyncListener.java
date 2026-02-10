package com.lastbreath.hc.lastBreathHC.fakeplayer;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Re-broadcasts active fake tab entries when a real player joins so new viewers always receive
 * the visual fake-player rows.
 */
public final class FakePlayerTabSyncListener implements Listener {
    private final LastBreathHC plugin;
    private final FakePlayerService fakePlayerService;

    public FakePlayerTabSyncListener(LastBreathHC plugin, FakePlayerService fakePlayerService) {
        this.plugin = plugin;
        this.fakePlayerService = fakePlayerService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshActiveFakes, 20L);
    }

    private void refreshActiveFakes() {
        for (FakePlayerRecord record : fakePlayerService.listFakePlayers()) {
            if (!record.isActive()) {
                continue;
            }
            fakePlayerService.refreshVisual(record.getUuid());
        }
    }
}
