package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidManager;
import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AsteroidCommand implements BasicCommand {

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (!source.getSender().isOp()) {
            return List.of();
        }

        if (args.length == 0) {
            return List.of("<x>", "<z>", "clear-mobs", "cleanup", "stop");
        }

        if (args.length == 1) {
            return List.of("clear-mobs", "cleanup", "stop");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("cleanup")) {
            return List.of("stop");
        }

        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {

        CommandSender sender = source.getSender();
        if (!sender.isOp()) {
            sender.sendMessage("§cNo permission.");
            return;
        }

        LastBreathHC plugin = LastBreathHC.getInstance();
        if (plugin == null) {
            sender.sendMessage("§cUnable to find a valid asteroid world.");
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clear-mobs")) {
            int removed = AsteroidManager.purgeAsteroidMobsFromMemory();
            sender.sendMessage("§aRemoved " + removed + " asteroid-tagged mobs and cleared asteroid mob memory.");
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("cleanup")) {
            World world = (sender instanceof Player player) ? player.getWorld() : plugin.resolveAsteroidCommandWorld(sender);
            if (world == null) {
                sender.sendMessage("§cUnable to find a valid asteroid world.");
                return;
            }
            AsteroidManager.startChunkCleanup(sender, world);
            return;
        }

        if ((args.length == 1 && args[0].equalsIgnoreCase("stop"))
                || (args.length == 2 && args[0].equalsIgnoreCase("cleanup") && args[1].equalsIgnoreCase("stop"))) {
            AsteroidManager.stopChunkCleanup(sender);
            return;
        }

        if (args.length == 2) {
            int blockX;
            int blockZ;
            try {
                blockX = Integer.parseInt(args[0]);
                blockZ = Integer.parseInt(args[1]);
            } catch (Exception e) {
                sender.sendMessage("§cUsage: /asteroid [x z|clear-mobs|cleanup|stop]");
                return;
            }
            World world = (sender instanceof Player player) ? player.getWorld() : plugin.resolveAsteroidCommandWorld(sender);
            if (world == null) {
                sender.sendMessage("§cUnable to find a valid asteroid world.");
                return;
            }

            int minHeight = world.getMinHeight();
            int maxHeight = world.getMaxHeight() - 1;
            int scanY = world.getHighestBlockYAt(blockX, blockZ);
            if (scanY < minHeight || scanY > maxHeight) {
                sender.sendMessage("§cUnable to find a valid asteroid spawn location.");
                return;
            }

            int groundY = -1;
            for (int y = scanY; y >= minHeight; y--) {
                if (world.getBlockAt(blockX, y, blockZ).getType().isSolid()) {
                    groundY = y;
                    break;
                }
            }

            if (groundY < minHeight || groundY >= maxHeight) {
                sender.sendMessage("§cUnable to find a valid asteroid spawn location.");
                return;
            }

            Location loc = new Location(world, blockX, groundY, blockZ);
            AsteroidManager.spawnAsteroid(world, loc);
            return;
        }

        if (args.length != 0) {
            sender.sendMessage("§cUsage: /asteroid [x z|clear-mobs|cleanup|stop]");
            return;
        }

        if (!plugin.spawnRandomAsteroid()) {
            sender.sendMessage("§cUnable to find a valid asteroid spawn location.");
        }
    }
}
