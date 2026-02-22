package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainRecord;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainRegistry;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainSpawner;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class NemesisCommands implements BasicCommand {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainSpawner spawner;
    private final NemesisUI ui;

    public NemesisCommands(LastBreathHC plugin, CaptainRegistry registry, CaptainSpawner spawner, NemesisUI ui) {
        this.plugin = plugin;
        this.registry = registry;
        this.spawner = spawner;
        this.ui = ui;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("list", "info", "hunt", "spawn", "retire", "debug");
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /nemesis <list|info|hunt|spawn|retire|debug>");
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "hunt" -> handleHunt(sender);
            case "spawn" -> handleSpawn(sender, args);
            case "retire" -> handleRetire(sender, args);
            case "debug" -> handleDebug(sender);
            default -> sender.sendMessage("§cUsage: /nemesis <list|info|hunt|spawn|retire|debug>");
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("lastbreathhc.nemesis")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        sender.sendMessage("§6Nemesis Captains: §e" + registry.getAll().size());
        registry.getAll().stream().limit(10).forEach(record -> {
            String name = record.naming() == null ? record.identity().captainId().toString() : record.naming().displayName();
            sender.sendMessage("§7- §c" + name + " §8(" + record.identity().captainId() + ")");
        });
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lastbreathhc.nemesis")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nemesis info <id|name>");
            return;
        }
        CaptainRecord record = findRecord(args[1]);
        if (record == null) {
            sender.sendMessage("§cCaptain not found.");
            return;
        }
        sender.sendMessage("§6Nemesis Info");
        sender.sendMessage("§7ID: §f" + record.identity().captainId());
        sender.sendMessage("§7Name: §f" + (record.naming() == null ? "Unknown" : record.naming().displayName()));
        sender.sendMessage("§7Level: §f" + record.progression().level() + " §7XP: §f" + record.progression().experience());
        sender.sendMessage("§7Traits: §f" + String.join(", ", record.traits().traits()));
        sender.sendMessage("§7Weaknesses: §f" + String.join(", ", record.traits().weaknesses()));
    }

    private void handleHunt(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only.");
            return;
        }
        if (!sender.hasPermission("lastbreathhc.nemesis")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        boolean triggered = spawner.triggerHunt(player, 1.0, "command_hunt");
        sender.sendMessage(triggered ? "§aYour nemesis hunt has begun." : "§eNo nemesis could be spawned right now.");
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lastbreathhc.nemesis.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nemesis spawn <id>");
            return;
        }
        CaptainRecord record = findRecord(args[1]);
        if (record == null) {
            sender.sendMessage("§cCaptain not found.");
            return;
        }
        boolean ok = spawner.forceSpawn(record.identity().captainId());
        sender.sendMessage(ok ? "§aNemesis spawned." : "§cUnable to spawn that captain now.");
    }

    private void handleRetire(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lastbreathhc.nemesis.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nemesis retire <id>");
            return;
        }
        CaptainRecord record = findRecord(args[1]);
        if (record == null) {
            sender.sendMessage("§cCaptain not found.");
            return;
        }
        boolean ok = spawner.retireCaptain(record.identity().captainId());
        sender.sendMessage(ok ? "§aCaptain retired." : "§eCaptain already retired or unavailable.");
    }

    private void handleDebug(CommandSender sender) {
        if (!sender.hasPermission("lastbreathhc.nemesis.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        long totalKills = registry.getAll().stream().mapToLong(r -> r.telemetry().counters().getOrDefault("playerKills", 0L)).sum();
        long totalEscapes = registry.getAll().stream().mapToLong(r -> r.telemetry().counters().getOrDefault("escapes", 0L)).sum();
        long totalMinionDeaths = registry.getAll().stream().mapToLong(r -> r.telemetry().counters().getOrDefault("minionDeaths", 0L)).sum();
        sender.sendMessage("§6Nemesis Debug: §e" + spawner.debugSummary() + " §7kills=" + totalKills + " escapes=" + totalEscapes + " minionDeaths=" + totalMinionDeaths);
    }

    private CaptainRecord findRecord(String token) {
        try {
            UUID id = UUID.fromString(token);
            return registry.getByCaptainUuid(id);
        } catch (IllegalArgumentException ignored) {
        }
        String lowered = token.toLowerCase(Locale.ROOT);
        return registry.getAll().stream()
                .filter(r -> r.naming() != null && r.naming().displayName() != null)
                .filter(r -> r.naming().displayName().toLowerCase(Locale.ROOT).contains(lowered))
                .findFirst().orElse(null);
    }
}
