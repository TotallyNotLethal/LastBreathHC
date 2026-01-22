package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class RtpUsageListener implements Listener {

    private final NamespacedKey rtpUsedKey;

    public RtpUsageListener(LastBreathHC plugin) {
        this.rtpUsedKey = new NamespacedKey(plugin, "rtp_used");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        player.getPersistentDataContainer().remove(rtpUsedKey);
    }
}
