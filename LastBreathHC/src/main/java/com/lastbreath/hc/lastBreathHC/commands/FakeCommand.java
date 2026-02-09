package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FakeCommand implements BasicCommand {

    private static final String PERMISSION_NODE = "lastbreathhc.fake";

    private final LastBreathHC plugin;

    public FakeCommand(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list", "mute", "skin");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("remove")
                || args[0].equalsIgnoreCase("mute")
                || args[0].equalsIgnoreCase("skin"))) {
            List<String> names = new ArrayList<>();
            if (args[0].equalsIgnoreCase("mute")) {
                names.add("all");
            }
            for (FakePlayerRecord record : service().listFakePlayers()) {
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

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args);
            case "mute" -> handleMute(sender, args);
            case "skin" -> handleSkin(sender, args);
            default -> {
                sender.sendMessage("§cUnknown subcommand: " + args[0]);
                sendUsage(sender);
            }
        }
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage("§cUsage: /fake add <name> [skinOwner]");
            return;
        }

        String name = args[1].trim();
        if (!isValidName(name)) {
            sender.sendMessage("§cInvalid fake player name. Use 3-16 characters (letters, numbers, underscore).");
            return;
        }

        String skinOwner = args.length == 3 ? args[2].trim() : name;
        if (skinOwner.isEmpty()) {
            sender.sendMessage("§cSkin owner cannot be empty.");
            return;
        }

        FakePlayerRecord record = service().addFakePlayer(name, skinOwner, null, null);
        service().saveNow();
        sender.sendMessage("§aFake player saved: §f" + record.getName() + " §7(skin: " + record.getSkinOwner() + ")");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /fake remove <name>");
            return;
        }

        Optional<FakePlayerRecord> optionalRecord = findByName(args[1]);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + args[1]);
            return;
        }

        FakePlayerRecord record = optionalRecord.get();
        boolean removed = service().removeFakePlayer(record.getUuid());
        if (!removed) {
            sender.sendMessage("§cFailed to remove fake player: " + record.getName());
            return;
        }

        service().saveNow();
        sender.sendMessage("§aRemoved fake player: §f" + record.getName());
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /fake list");
            return;
        }

        List<FakePlayerRecord> records = service().listFakePlayers();
        if (records.isEmpty()) {
            sender.sendMessage("§eNo fake players configured.");
            return;
        }

        sender.sendMessage("§6Fake players §7(" + records.size() + "):");
        for (FakePlayerRecord record : records) {
            sender.sendMessage("§7- §f" + record.getName()
                    + " §8| §7skin: §f" + valueOrUnknown(record.getSkinOwner())
                    + " §8| §7active: " + (record.isActive() ? "§ayes" : "§cno")
                    + " §8| §7muted: " + (record.isMuted() ? "§cyes" : "§ano"));
        }
    }

    private void handleMute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /fake mute <name|all>");
            return;
        }

        String target = args[1];
        if (target.equalsIgnoreCase("all")) {
            int mutedCount = 0;
            for (FakePlayerRecord record : service().listFakePlayers()) {
                if (service().mute(record.getUuid(), true)) {
                    mutedCount++;
                }
            }
            service().saveNow();
            sender.sendMessage("§aMuted fake players: §f" + mutedCount);
            return;
        }

        Optional<FakePlayerRecord> optionalRecord = findByName(target);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + target);
            return;
        }

        FakePlayerRecord record = optionalRecord.get();
        if (record.isMuted()) {
            sender.sendMessage("§e" + record.getName() + " is already muted.");
            return;
        }

        boolean muted = service().mute(record.getUuid(), true);
        if (!muted) {
            sender.sendMessage("§cFailed to mute fake player: " + record.getName());
            return;
        }

        service().saveNow();
        sender.sendMessage("§aMuted fake player: §f" + record.getName());
    }

    private void handleSkin(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§cUsage: /fake skin <name> <skinOwner>");
            return;
        }

        String targetName = args[1].trim();
        String skinOwner = args[2].trim();
        if (skinOwner.isEmpty()) {
            sender.sendMessage("§cSkin owner cannot be empty.");
            return;
        }

        Optional<FakePlayerRecord> optionalRecord = findByName(targetName);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + targetName);
            return;
        }

        FakePlayerRecord record = optionalRecord.get();
        record.setSkinOwner(skinOwner);
        record.setTextures(null);
        record.setSignature(null);
        service().refreshVisual(record.getUuid());
        service().saveNow();
        sender.sendMessage("§aUpdated skin owner for §f" + record.getName() + "§a to §f" + skinOwner + "§a.");
    }

    private Optional<FakePlayerRecord> findByName(String name) {
        for (FakePlayerRecord record : service().listFakePlayers()) {
            if (record.getName().equalsIgnoreCase(name)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6Fake command usage:");
        sender.sendMessage("§e/fake add <name> [skinOwner]");
        sender.sendMessage("§e/fake remove <name>");
        sender.sendMessage("§e/fake list");
        sender.sendMessage("§e/fake mute <name|all>");
        sender.sendMessage("§e/fake skin <name> <skinOwner>");
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

    private boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() < 3 || trimmed.length() > 16) {
            return false;
        }
        return trimmed.matches("[A-Za-z0-9_]+$");
    }

    private String valueOrUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        return value;
    }

    private FakePlayerService service() {
        return plugin.getFakePlayerService();
    }
}
