package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.revive.ReviveStateManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReviveCommand implements BasicCommand {

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "toggle", "status");
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /revive.");
            return;
        }

        if (args.length > 1) {
            player.sendMessage(ChatColor.RED + "Usage: /revive [on|off|toggle|status]");
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean enabled = ReviveStateManager.toggleTeleportOnDeath(player.getUniqueId());
            sendStatusMessage(player, enabled);
            return;
        }

        if (args[0].equalsIgnoreCase("status")) {
            sendStatusMessage(player, ReviveStateManager.isTeleportOnDeathEnabled(player.getUniqueId()));
            return;
        }

        if (args[0].equalsIgnoreCase("on")) {
            ReviveStateManager.setTeleportOnDeathEnabled(player.getUniqueId(), true);
            sendStatusMessage(player, true);
            return;
        }

        if (args[0].equalsIgnoreCase("off")) {
            ReviveStateManager.setTeleportOnDeathEnabled(player.getUniqueId(), false);
            sendStatusMessage(player, false);
            return;
        }

        player.sendMessage(ChatColor.RED + "Usage: /revive [on|off|toggle|status]");
    }

    private void sendStatusMessage(Player player, boolean enabled) {
        if (enabled) {
            player.sendMessage(ChatColor.GREEN + "Revive teleport is ENABLED. Revive tokens will teleport you to your bed/spawn on death.");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "Revive teleport is DISABLED. Revive tokens still work but will not teleport you on death.");
    }
}
