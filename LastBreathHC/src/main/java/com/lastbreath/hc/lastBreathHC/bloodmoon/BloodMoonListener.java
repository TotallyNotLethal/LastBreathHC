package com.lastbreath.hc.lastBreathHC.bloodmoon;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BloodMoonListener implements Listener {

    private final BloodMoonManager bloodMoonManager;

    public BloodMoonListener(BloodMoonManager bloodMoonManager) {
        this.bloodMoonManager = bloodMoonManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bloodMoonManager.isActive()) {
            bloodMoonManager.applyBloodMoonEffects(event.getPlayer());
        }
    }
}
