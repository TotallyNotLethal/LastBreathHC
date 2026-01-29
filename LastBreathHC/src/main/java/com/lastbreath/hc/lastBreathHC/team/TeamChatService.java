package com.lastbreath.hc.lastBreathHC.team;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Set;

public class TeamChatService {

    private static final String DEFAULT_FORMAT = "&7[Team] &b%player%&7: &f%message%";

    private final LastBreathHC plugin;
    private final TeamManager teamManager;

    public TeamChatService(LastBreathHC plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public boolean sendTeamMessage(Player sender, String message) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Usage: /t <message>");
            return false;
        }

        Set<Player> recipients = teamManager.getOnlineTeamMembers(sender);
        if (recipients.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "You are not on a team.");
            return false;
        }

        String formatted = formatMessage(sender.getName(), trimmed);
        Runnable sendTask = () -> {
            for (Player recipient : recipients) {
                recipient.sendMessage(formatted);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            sendTask.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, sendTask);
        }

        return true;
    }

    private String formatMessage(String playerName, String message) {
        String raw = plugin.getConfig().getString("teamChat.format", DEFAULT_FORMAT);
        String withPlaceholders = raw
                .replace("%player%", playerName)
                .replace("%message%", message);
        return ChatColor.translateAlternateColorCodes('&', withPlaceholders);
    }
}
