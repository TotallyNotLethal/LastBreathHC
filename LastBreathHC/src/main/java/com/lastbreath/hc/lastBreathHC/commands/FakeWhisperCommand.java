package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FakeWhisperCommand implements BasicCommand {

    private static final String PERMISSION_NODE = "lastbreathhc.fake.whisper";

    private final LastBreathHC plugin;

    public FakeWhisperCommand(LastBreathHC plugin) {
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

        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return names;
        }

        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!hasAccess(sender)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /fwhisper <FakePlayerName> <PlayerName> <message...>");
            return;
        }

        String fakeName = args[0];
        Optional<FakePlayerRecord> optionalRecord = findByName(fakeName);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + fakeName);
            return;
        }

        FakePlayerRecord fake = optionalRecord.get();
        if (!fake.isActive()) {
            sender.sendMessage("§c" + fake.getName() + " is inactive and cannot whisper.");
            return;
        }
        if (fake.isMuted()) {
            sender.sendMessage("§c" + fake.getName() + " is muted.");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }

        String payload = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        if (payload.isBlank()) {
            sender.sendMessage("§cMessage cannot be empty.");
            return;
        }

        if (!plugin.getFakePlayerService().registerReaction(fake.getUuid())) {
            sender.sendMessage("§cFailed to track fake whisper reaction for " + fake.getName() + ".");
            return;
        }
        if (!plugin.getFakePlayerService().registerChat(fake.getUuid(), payload)) {
            sender.sendMessage("§cFailed to send fake whisper for " + fake.getName() + ".");
            return;
        }

        plugin.getFakePlayerService().saveNow();

        target.sendMessage(ChatColor.GRAY + fake.getName() + " whispers to you: " + payload);
        sender.sendMessage(ChatColor.GRAY + fake.getName() + " whispered to " + target.getName() + ": " + payload);
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
        return sender.isOp() || sender.hasPermission(PERMISSION_NODE);
    }
}
