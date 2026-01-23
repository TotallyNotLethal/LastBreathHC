package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.gui.BountyBoardGUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class BountyCommand implements BasicCommand {

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player)) {
            return List.of();
        }

        if (args.length == 0) {
            return List.of("1");
        }

        if (args.length == 1) {
            return filterByPrefix(args[0], List.of("1"));
        }

        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Players only.");
            return;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                player.sendMessage("§cUsage: /bounty [page]");
                return;
            }
        }

        BountyBoardGUI.open(player, page);
    }

    private List<String> filterByPrefix(String input, List<String> options) {
        if (input == null || input.isBlank()) {
            return options;
        }
        String lowered = input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowered))
                .toList();
    }
}
