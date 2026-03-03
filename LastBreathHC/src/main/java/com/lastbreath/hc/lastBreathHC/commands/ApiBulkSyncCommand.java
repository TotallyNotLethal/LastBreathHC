package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.integrations.lastbreath.ApiEventListener;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class ApiBulkSyncCommand implements BasicCommand {

    private static final String PERMISSION = "lastbreathhc.api.bulkstats";

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (!hasAccess(source.getSender())) {
            return List.of();
        }

        List<String> options = List.of("bulkstats");
        if (args.length == 0) {
            return options;
        }
        if (args.length == 1) {
            String lowered = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
            return options.stream()
                    .filter(option -> option.startsWith(lowered))
                    .toList();
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!hasAccess(sender)) {
            sender.sendMessage("§cNo permission.");
            return;
        }

        if (args.length != 1 || !"bulkstats".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§cUsage: /lbapi bulkstats");
            return;
        }

        LastBreathHC plugin = LastBreathHC.getInstance();
        ApiEventListener listener = plugin.getApiEventListener();
        if (listener == null) {
            sender.sendMessage("§cLastBreath API integration is not available.");
            return;
        }

        sender.sendMessage("§eSending bulk player stats to LastBreath API...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            listener.sendBulkStatsStartup();
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§aBulk player stats sent to LastBreath API."));
        });
    }

    private boolean hasAccess(CommandSender sender) {
        return sender.hasPermission(PERMISSION)
                || (sender instanceof Player player && player.isOp());
    }
}
