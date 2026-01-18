package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class AsteroidCommand implements BasicCommand {

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

        Location loc;

        if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                loc = new Location(player.getWorld(), x, y, z);
            } catch (Exception e) {
                player.sendMessage("§cUsage: /asteroid [x y z]");
                return;
            }
        } else {
            loc = player.getLocation();
        }

        AsteroidManager.spawnAsteroid(player.getWorld(), loc);
    }
}
