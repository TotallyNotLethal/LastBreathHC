package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ListCommand implements BasicCommand {
    private final LastBreathHC plugin;

    public ListCommand(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /list");
            return;
        }

        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        int fakeActive = 0;
        for (FakePlayerRecord record : plugin.getFakePlayerService().listFakePlayers()) {
            if (!record.isActive()) {
                continue;
            }
            fakeActive++;
            names.add(record.getName());
        }

        int realOnline = Bukkit.getOnlinePlayers().size();
        int totalOnline = realOnline + fakeActive;
        int maxPlayers = Bukkit.getMaxPlayers();
        sender.sendMessage(ChatColor.YELLOW + "Online players: " + totalOnline + "/" + maxPlayers);
        if (!names.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + String.join(", ", names));
        }
    }
}
