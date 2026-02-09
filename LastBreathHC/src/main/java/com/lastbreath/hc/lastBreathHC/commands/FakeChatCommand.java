package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeChatCommand implements BasicCommand {

    private static final String PERMISSION_NODE = "lastbreathhc.fake.chat";

    private final LastBreathHC plugin;

    public FakeChatCommand(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (FakePlayerRecord record : plugin.getFakePlayerService().listFakePlayers()) {
                names.add(record.getName());
            }
            return names;
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!hasAccess(sender)) {
            sender.sendMessage("§cOnly operators can use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /chat <FakePlayerName> <message...>");
            return;
        }

        String fakeName = args[0];
        Optional<FakePlayerRecord> optionalRecord = findByName(fakeName);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + fakeName);
            return;
        }

        FakePlayerRecord record = optionalRecord.get();
        if (!record.isActive()) {
            sender.sendMessage("§c" + record.getName() + " is inactive and cannot chat.");
            return;
        }
        if (record.isMuted()) {
            sender.sendMessage("§c" + record.getName() + " is muted.");
            return;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
        if (message.isBlank()) {
            sender.sendMessage("§cMessage cannot be empty.");
            return;
        }

        boolean registered = plugin.getFakePlayerService().registerChat(record.getUuid(), message);
        if (!registered) {
            sender.sendMessage("§cFailed to send fake chat for " + record.getName() + ".");
            return;
        }

        plugin.getFakePlayerService().saveNow();
        Bukkit.broadcastMessage("<" + record.getName() + "> " + message);
    }

    private Optional<FakePlayerRecord> findByName(String name) {
        for (FakePlayerRecord record : plugin.getFakePlayerService().listFakePlayers()) {
            if (record.getName().equalsIgnoreCase(name)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    private boolean hasAccess(CommandSender sender) {
        if (sender.isOp()) {
            return true;
        }
        // Secondary permission-node check retained for future LuckPerms rollout.
        boolean hasPermissionNode = sender.hasPermission(PERMISSION_NODE);
        if (hasPermissionNode) {
            // Intentionally not granting access yet; OP gate remains default behavior.
        }
        return false;
    }
}
