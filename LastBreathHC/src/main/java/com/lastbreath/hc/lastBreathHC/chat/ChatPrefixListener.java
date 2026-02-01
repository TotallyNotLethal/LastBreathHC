package com.lastbreath.hc.lastBreathHC.chat;

import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticManager;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatPrefixListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        PlayerStats stats = StatsManager.get(event.getPlayer().getUniqueId());
        String nickname = stats.nickname;
        String displayName = nickname != null && !nickname.isBlank() ? nickname : event.getPlayer().getName();
        String prefixTag = CosmeticManager.getPrefixTag(event.getPlayer(), false);
        String format = prefixTag + displayName + ChatColor.GRAY + ": " + ChatColor.WHITE + "%2$s";
        event.setFormat(format);
    }
}
