package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class TitlesCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Players only.");
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§6Unlocked titles: " + TitleManager.formatTitleList(player));
            return;
        }

        if (args[0].equalsIgnoreCase("equip")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /titles equip <title>");
                return;
            }
            String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Title title = Title.fromInput(input);
            if (title == null) {
                player.sendMessage("§cUnknown title. Use /titles list to see options.");
                return;
            }
            if (!TitleManager.equipTitle(player, title)) {
                player.sendMessage("§cYou have not unlocked that title yet.");
                return;
            }
            player.sendMessage("§aEquipped title: " + title.displayName());
            return;
        }

        player.sendMessage("§cUsage: /titles [list|equip <title>]");
    }
}
