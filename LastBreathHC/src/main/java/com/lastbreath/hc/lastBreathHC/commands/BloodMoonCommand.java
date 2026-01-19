package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

public class BloodMoonCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Players only.");
            return;
        }

        if (!player.isOp()) {
            player.sendMessage("§cNo permission.");
            return;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /bloodmoon <start|stop>");
            return;
        }

        BloodMoonManager manager = LastBreathHC.getInstance().getBloodMoonManager();

        if (args[0].equalsIgnoreCase("start")) {
            manager.start();
            player.sendMessage("§cBlood moon started.");
            return;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            manager.stop();
            player.sendMessage("§cBlood moon stopped.");
            return;
        }

        player.sendMessage("§cUsage: /bloodmoon <start|stop>");
    }
}
