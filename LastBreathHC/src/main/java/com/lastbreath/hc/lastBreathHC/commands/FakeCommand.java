package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
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
            return List.of("add", "remove", "kill", "list", "counts", "mute", "skin", "title", "ping", "advancement");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("remove")
                || args[0].equalsIgnoreCase("kill")
                || args[0].equalsIgnoreCase("mute")
                || args[0].equalsIgnoreCase("skin")
                || args[0].equalsIgnoreCase("title")
                || args[0].equalsIgnoreCase("ping")
                || args[0].equalsIgnoreCase("advancement"))) {
            List<String> names = new ArrayList<>();
            if (args[0].equalsIgnoreCase("mute")) {
                names.add("all");
            }
            for (FakePlayerRecord record : service().listFakePlayers()) {
                names.add(record.getName());
            }
            return names;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("advancement")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            List<String> keys = new ArrayList<>();
            java.util.Iterator<Advancement> iterator = Bukkit.advancementIterator();
            while (iterator.hasNext()) {
                Advancement advancement = iterator.next();
                String key = advancement.getKey().toString();
                if (key.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    keys.add(key);
                }
            }
            return keys;
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

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "kill" -> handleKill(sender, args);
            case "list" -> handleList(sender, args);
            case "counts" -> handleCounts(sender, args);
            case "mute" -> handleMute(sender, args);
            case "skin" -> handleSkin(sender, args);
            case "title" -> handleTitle(sender, args);
            case "ping" -> handlePing(sender, args);
            case "advancement" -> handleAdvancement(sender, args);
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

        Optional<FakePlayerRecord> existing = findByName(name);
        boolean wasActive = existing.isPresent() && existing.get().isActive();
        FakePlayerRecord record = service().addFakePlayer(name, skinOwner, null, null);
        service().saveNow();
        if (!wasActive) {
            service().announceFakeJoin(record);
        }
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
        boolean wasActive = record.isActive();
        boolean removed = service().removeFakePlayer(record.getUuid());
        if (!removed) {
            sender.sendMessage("§cFailed to remove fake player: " + record.getName());
            return;
        }

        service().saveNow();
        if (wasActive) {
            service().announceFakeLeave(record);
        }
        sender.sendMessage("§aRemoved fake player: §f" + record.getName());
    }

    private void handleKill(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /fake kill <name>");
            return;
        }

        Optional<FakePlayerRecord> optionalRecord = findByName(args[1]);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + args[1]);
            return;
        }

        FakePlayerRecord record = optionalRecord.get();
        if (!record.isActive()) {
            sender.sendMessage("§e" + record.getName() + " is already inactive.");
            return;
        }

        String killerName = sender.getName();
        boolean killed = service().killFakePlayer(record.getUuid(), killerName);
        if (!killed) {
            sender.sendMessage("§cFailed to kill fake player: " + record.getName());
            return;
        }

        sender.sendMessage("§aKilled fake player: §f" + record.getName());
    }

    private void handleAdvancement(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§cUsage: /fake advancement <name> <namespace:key>");
            return;
        }

        Optional<FakePlayerRecord> optionalRecord = findByName(args[1]);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + args[1]);
            return;
        }

        FakePlayerRecord record = optionalRecord.get();
        if (!record.isActive()) {
            sender.sendMessage("§c" + record.getName() + " is inactive and cannot gain advancements.");
            return;
        }

        NamespacedKey advancementKey = NamespacedKey.fromString(args[2]);
        if (advancementKey == null) {
            sender.sendMessage("§cInvalid advancement key. Use namespace:key format.");
            return;
        }

        boolean announced = service().announceAdvancement(record.getUuid(), advancementKey);
        if (!announced) {
            sender.sendMessage("§cUnknown advancement: " + advancementKey);
            return;
        }

        sender.sendMessage("§aGranted advancement to fake player: §f" + record.getName() + " §7(" + advancementKey + ")");
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /fake list");
            return;
        }

        List<FakePlayerRecord> records = service().listFakePlayers();
        if (records.isEmpty()) {
            sender.sendMessage("§eNo fake players configured.");
            sendCounts(sender, records);
            return;
        }

        sender.sendMessage("§6Fake players §7(" + records.size() + "):");
        for (FakePlayerRecord record : records) {
            sender.sendMessage("§7- §f" + record.getName()
                    + " §8| §7skin: §f" + valueOrUnknown(record.getSkinOwner())
                    + " §8| §7active: " + (record.isActive() ? "§ayes" : "§cno")
                    + " §8| §7muted: " + (record.isMuted() ? "§cyes" : "§ano"));
        }
        sendCounts(sender, records);
    }

    private void handleCounts(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /fake counts");
            return;
        }

        sendCounts(sender, service().listFakePlayers());
    }

    private void sendCounts(CommandSender sender, List<FakePlayerRecord> records) {
        int activeFakeCount = 0;
        for (FakePlayerRecord record : records) {
            if (record.isActive()) {
                activeFakeCount++;
            }
        }
        int realOnlineCount = Bukkit.getOnlinePlayers().size();

        sender.sendMessage("§6Online counts §8| §7real: §f" + realOnlineCount
                + " §8| §7fake(active): §f" + activeFakeCount
                + " §8| §7combined: §f" + (realOnlineCount + activeFakeCount));
        sender.sendMessage("§8Note: §f/list§8 includes active fake players on this server.");
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
        FakePlayerService.SkinUpdateOutcome outcome = service().refreshSkin(record.getUuid(), skinOwner, true);
        if (outcome.notFound()) {
            sender.sendMessage("§cNo fake player found with name: " + targetName);
            return;
        }
        if (!outcome.success()) {
            sender.sendMessage("§cUnable to resolve skin textures for owner: " + skinOwner);
            return;
        }

        service().saveNow();
        if (outcome.usedFallbackCache()) {
            sender.sendMessage("§eSkin API unavailable, kept cached skin for §f" + record.getName() + "§e (owner: §f" + skinOwner + "§e).");
            return;
        }
        sender.sendMessage("§aUpdated skin owner for §f" + record.getName() + "§a to §f" + skinOwner + "§a.");
    }


    private void handleTitle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /fake title <name> <title>");
            return;
        }

        String targetName = args[1].trim();
        String titleInput = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)).trim();
        if (titleInput.isEmpty()) {
            sender.sendMessage("§cTitle cannot be empty.");
            return;
        }

        Optional<FakePlayerRecord> optionalRecord = findByName(targetName);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + targetName);
            return;
        }

        FakePlayerRecord record = optionalRecord.get();
        boolean updated = service().setTabTitle(record.getUuid(), titleInput);
        if (!updated) {
            sender.sendMessage("§cUnknown title. Use a valid configured title name.");
            return;
        }

        service().refreshVisual(record.getUuid());
        service().saveNow();
        sender.sendMessage("§aUpdated tab title for §f" + record.getName() + "§a to §f" + service().getByUuid(record.getUuid()).map(FakePlayerRecord::getTabTitleKey).orElse("unknown") + "§a.");
    }

    private void handlePing(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§cUsage: /fake ping <name> <ms>");
            return;
        }

        String targetName = args[1].trim();
        int pingMillis;
        try {
            pingMillis = Integer.parseInt(args[2].trim());
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cPing must be a whole number in milliseconds.");
            return;
        }

        if (pingMillis < 0) {
            sender.sendMessage("§cPing must be zero or greater.");
            return;
        }

        Optional<FakePlayerRecord> optionalRecord = findByName(targetName);
        if (optionalRecord.isEmpty()) {
            sender.sendMessage("§cNo fake player found with name: " + targetName);
            return;
        }

        FakePlayerRecord record = optionalRecord.get();
        boolean updated = service().setTabPing(record.getUuid(), pingMillis);
        if (!updated) {
            sender.sendMessage("§cFailed to update ping for fake player: " + record.getName());
            return;
        }

        service().refreshVisual(record.getUuid());
        service().saveNow();
        int effectivePing = service().getByUuid(record.getUuid()).map(FakePlayerRecord::getTabPingMillis).orElse(pingMillis);
        sender.sendMessage("§aUpdated tab ping for §f" + record.getName() + "§a to §f" + effectivePing + "ms§a.");
    }

    private Optional<FakePlayerRecord> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String trimmedName = name.trim();
        Optional<FakePlayerRecord> normalizedHit = service().getByUuid(FakePlayerService.deterministicUuid(trimmedName));
        if (normalizedHit.isPresent()) {
            return normalizedHit;
        }

        for (FakePlayerRecord record : service().listFakePlayers()) {
            if (record.getName().equalsIgnoreCase(trimmedName)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6Fake command usage:");
        sender.sendMessage("§e/fake add <name> [skinOwner]");
        sender.sendMessage("§e/fake remove <name>");
        sender.sendMessage("§e/fake kill <name>");
        sender.sendMessage("§e/fake list");
        sender.sendMessage("§e/fake counts");
        sender.sendMessage("§e/fake mute <name|all>");
        sender.sendMessage("§e/fake skin <name> <skinOwner>");
        sender.sendMessage("§e/fake title <name> <title>");
        sender.sendMessage("§e/fake ping <name> <ms>");
        sender.sendMessage("§e/fake advancement <name> <namespace:key>");
    }

    private boolean hasAccess(CommandSender sender) {
        return sender.isOp() || sender.hasPermission(PERMISSION_NODE);
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
