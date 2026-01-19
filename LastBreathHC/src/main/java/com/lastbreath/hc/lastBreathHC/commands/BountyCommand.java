package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.gui.BountyBoardGUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

public class BountyCommand implements BasicCommand {

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
}
