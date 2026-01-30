package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.worldboss.WorldBossManager;
import com.lastbreath.hc.lastBreathHC.worldboss.WorldBossType;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class WorldBossCommand implements BasicCommand {

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (!source.getSender().isOp()) {
            return List.of();
        }
        if (args.length == 0) {
            return List.of("spawn", "portal", "enable", "disable");
        }
        if ("spawn".equalsIgnoreCase(args[0]) && args.length == 1) {
            return List.of("Gravewarden", "StormHerald", "HollowColossus");
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
        if (plugin == null || plugin.getWorldBossManager() == null) {
            sender.sendMessage("§cWorld boss manager is not available.");
            return;
        }
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /worldboss <spawn|portal|enable|disable> [type]");
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        WorldBossManager manager = plugin.getWorldBossManager();

        switch (sub) {
            case "portal" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can place a portal.");
                    return;
                }
                manager.createPortalAt(player.getLocation());
                sender.sendMessage("§aWorld boss portal created.");
            }
            case "spawn" -> {
                World world = (sender instanceof Player player) ? player.getWorld() : plugin.getServer().getWorlds().stream().findFirst().orElse(null);
                if (world == null) {
                    sender.sendMessage("§cNo world available for spawning.");
                    return;
                }
                WorldBossType type = null;
                if (args.length >= 2) {
                    type = WorldBossType.fromConfigKey(args[1]).orElse(null);
                    if (type == null) {
                        sender.sendMessage("§cUnknown boss type.");
                        return;
                    }
                }
                boolean success = manager.spawnTestBoss(world, sender instanceof Player player ? player.getLocation() : world.getSpawnLocation(), type);
                if (success) {
                    sender.sendMessage("§aWorld boss spawned.");
                } else {
                    sender.sendMessage("§cUnable to spawn world boss.");
                }
            }
            case "enable" -> {
                manager.enableBosses();
                sender.sendMessage("§aWorld bosses enabled.");
            }
            case "disable" -> {
                manager.disableBosses();
                sender.sendMessage("§eWorld bosses disabled and existing bosses removed.");
            }
            default -> sender.sendMessage("§cUsage: /worldboss <spawn|portal|enable|disable> [type]");
        }
    }
}
