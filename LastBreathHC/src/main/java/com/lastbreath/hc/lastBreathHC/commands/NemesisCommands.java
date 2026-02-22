package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainRecord;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainRegistry;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainSpawner;
import com.lastbreath.hc.lastBreathHC.nemesis.KillerResolver;
import com.lastbreath.hc.lastBreathHC.nemesis.MinionController;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class NemesisCommands implements BasicCommand {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainSpawner spawner;
    private final NemesisUI ui;
    private final KillerResolver killerResolver;
    private final MinionController minionController;

    public NemesisCommands(LastBreathHC plugin, CaptainRegistry registry, CaptainSpawner spawner, NemesisUI ui, KillerResolver killerResolver, MinionController minionController) {
        this.plugin = plugin;
        this.registry = registry;
        this.spawner = spawner;
        this.ui = ui;
        this.killerResolver = killerResolver;
        this.minionController = minionController;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("list", "info", "hunt", "spawn", "retire", "debug");
        }
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return List.of("resolvekiller", "dump", "active", "cleanupOrphans");
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
            case "debug" -> handleDebug(sender, args);
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

    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lastbreathhc.nemesis.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }

        if (args.length == 1) {
            long totalKills = registry.getAll().stream().mapToLong(r -> r.telemetry().counters().getOrDefault("playerKills", 0L)).sum();
            long totalEscapes = registry.getAll().stream().mapToLong(r -> r.telemetry().counters().getOrDefault("escapes", 0L)).sum();
            long totalMinionDeaths = registry.getAll().stream().mapToLong(r -> r.telemetry().counters().getOrDefault("minionDeaths", 0L)).sum();
            sender.sendMessage("§6Nemesis Debug: §e" + spawner.debugSummary() + " §7kills=" + totalKills + " escapes=" + totalEscapes + " minionDeaths=" + totalMinionDeaths);
            sender.sendMessage("§7Subcommands: resolvekiller, dump, active, cleanupOrphans");
            return;
        }

        String debugSub = args[1].toLowerCase(Locale.ROOT);
        switch (debugSub) {
            case "resolvekiller" -> handleDebugResolveKiller(sender, args);
            case "dump" -> handleDebugDump(sender, args);
            case "active" -> handleDebugActive(sender);
            case "cleanuporphans" -> handleDebugCleanupOrphans(sender);
            default -> sender.sendMessage("§cUsage: /nemesis debug <resolvekiller|dump|active|cleanupOrphans>");
        }
    }

    private void handleDebugResolveKiller(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /nemesis debug resolvekiller <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§cPlayer not online.");
            return;
        }

        KillerResolver.ResolutionDebugSnapshot debug = killerResolver.inspect(target, KillerResolver.AttributionTimeouts.defaultTimeouts());
        sender.sendMessage("§6Killer resolution for §e" + target.getName());
        if (debug.resolvedKiller() == null || debug.resolvedKiller().entity() == null) {
            sender.sendMessage("§7Resolved: §cnone");
        } else {
            sender.sendMessage("§7Resolved: §f" + debug.resolvedKiller().entity().getType().name() + " §8(" + debug.resolvedKiller().entityUuid() + ") §7via §f" + debug.resolvedKiller().sourceType());
        }
        if (debug.attributionChain().isEmpty()) {
            sender.sendMessage("§7Attribution chain: §8empty");
            return;
        }
        sender.sendMessage("§7Attribution chain:");
        for (KillerResolver.AttributionSnapshot entry : debug.attributionChain()) {
            sender.sendMessage("§8- §f" + entry.displayName() + " §7(" + entry.sourceType() + ", " + entry.damageCause() + ", valid=" + entry.valid() + ")");
        }
    }

    private void handleDebugDump(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /nemesis debug dump <id>");
            return;
        }
        CaptainRecord record = findRecord(args[2]);
        if (record == null) {
            sender.sendMessage("§cCaptain not found.");
            return;
        }
        sender.sendMessage("§6Captain dump §8(" + record.identity().captainId() + ")");
        sender.sendMessage("§7NemesisOf: §f" + record.identity().nemesisOf());
        sender.sendMessage("§7State: §f" + (record.state() == null ? "null" : record.state().state()));
        sender.sendMessage("§7World/Chunk: §f" + (record.origin() == null ? "unknown" : record.origin().world() + " " + record.origin().chunkX() + "," + record.origin().chunkZ()));
        sender.sendMessage("§7Runtime entity: §f" + (record.state() == null ? "null" : record.state().runtimeEntityUuid()));
        sender.sendMessage("§7Scores: §f" + (record.nemesisScores() == null ? "none" : "threat=" + record.nemesisScores().threat() + " rivalry=" + record.nemesisScores().rivalry() + " hate=" + record.nemesisScores().hate()));
        sender.sendMessage("§7Victims: §f" + (record.victims() == null ? "none" : record.victims().killCount() + " recent=" + record.victims().recentVictimIds().size()));
        sender.sendMessage("§7Counters: §f" + (record.telemetry() == null ? "{}" : record.telemetry().counters()));
    }

    private void handleDebugActive(CommandSender sender) {
        Map<String, Integer> perWorld = registry.getActiveCaptainCountsByWorld();
        if (perWorld.isEmpty()) {
            sender.sendMessage("§eNo active captains indexed.");
            return;
        }
        sender.sendMessage("§6Active captains per world:");
        perWorld.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> sender.sendMessage("§7- §f" + entry.getKey() + ": §e" + entry.getValue()));
    }

    private void handleDebugCleanupOrphans(CommandSender sender) {
        int removed = minionController.cleanupOrphansNow();
        sender.sendMessage("§aOrphan cleanup completed. Removed entities: §e" + removed);
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
