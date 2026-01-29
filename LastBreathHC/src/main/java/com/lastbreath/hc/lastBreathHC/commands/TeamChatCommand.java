package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.team.TeamChatService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class TeamChatCommand implements BasicCommand {

    private final TeamChatService teamChatService;

    public TeamChatCommand(TeamChatService teamChatService) {
        this.teamChatService = teamChatService;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(ChatColor.RED + "Only players can use team chat.");
            return;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /t <message>");
            return;
        }

        String message = String.join(" ", args);
        teamChatService.sendTeamMessage(player, message);
    }
}
