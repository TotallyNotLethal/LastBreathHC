package com.lastbreath.hc.lastBreathHC.death;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLastMessageTracker implements Listener {

    private final Map<UUID, String> lastMessagesByPlayer = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        lastMessagesByPlayer.put(event.getPlayer().getUniqueId(), message);
    }

    public String getLastMessage(UUID playerId) {
        return lastMessagesByPlayer.get(playerId);
    }
}
