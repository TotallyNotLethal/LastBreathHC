package com.lastbreath.hc.lastBreathHC.team;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class TeamChatListener implements Listener {

    private final TeamChatService teamChatService;

    public TeamChatListener(TeamChatService teamChatService) {
        this.teamChatService = teamChatService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        if (message == null) {
            return;
        }

        String trimmed = message.trim();
        if (!trimmed.startsWith("/t")) {
            return;
        }

        String content = trimmed.length() > 2 ? trimmed.substring(2).trim() : "";
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (content.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Usage: /t <message>");
            return;
        }

        teamChatService.sendTeamMessage(player, content);
    }
}
