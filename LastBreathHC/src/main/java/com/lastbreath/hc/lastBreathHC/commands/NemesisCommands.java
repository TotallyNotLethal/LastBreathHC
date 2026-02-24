package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.nemesis.CaptainRecord;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainRegistry;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainSpawner;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainTraitRegistry;
import com.lastbreath.hc.lastBreathHC.nemesis.KillerResolver;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainHabitatService;
import com.lastbreath.hc.lastBreathHC.nemesis.MinionController;
import com.lastbreath.hc.lastBreathHC.nemesis.Rank;
import com.lastbreath.hc.lastBreathHC.nemesis.ArmyGraphService;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisCaptainListGUI;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisMobRules;
import com.lastbreath.hc.lastBreathHC.nemesis.TerritoryPressureService;
import com.lastbreath.hc.lastBreathHC.structures.StructureFootprintRepository;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class NemesisCommands implements BasicCommand {
    private final CaptainRegistry registry;
    private final CaptainSpawner spawner;
    private final NemesisCaptainListGUI captainListGUI;
    private final KillerResolver killerResolver;
    private final MinionController minionController;
    private final CaptainTraitRegistry traitRegistry;
    private final ArmyGraphService armyGraphService;
    private final TerritoryPressureService territoryPressureService;
    private final StructureFootprintRepository structureFootprintRepository;
    private final CaptainHabitatService captainHabitatService;

    public NemesisCommands(CaptainRegistry registry, CaptainSpawner spawner, NemesisCaptainListGUI captainListGUI, KillerResolver killerResolver, MinionController minionController, CaptainTraitRegistry traitRegistry, ArmyGraphService armyGraphService, TerritoryPressureService territoryPressureService, StructureFootprintRepository structureFootprintRepository, CaptainHabitatService captainHabitatService) {
        this.registry = registry;
        this.spawner = spawner;
        this.captainListGUI = captainListGUI;
        this.killerResolver = killerResolver;
        this.minionController = minionController;
        this.traitRegistry = traitRegistry;
        this.armyGraphService = armyGraphService;
        this.territoryPressureService = territoryPressureService;
        this.structureFootprintRepository = structureFootprintRepository;
        this.captainHabitatService = captainHabitatService;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("list", "nearby", "info", "hunt", "army", "territory", "spawn", "retire", "clear", "debug");
        }
        if (args.length == 2) {
            if ("debug".equalsIgnoreCase(args[0])) {
                return List.of("resolvekiller", "dump", "active", "cleanupOrphans", "resetpressure", "clearrelationships", "forcepromotion", "habitat", "cleanuphabitat");
            }
            if ("clear".equalsIgnoreCase(args[0])) {
                return List.of("confirm");
            }
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /nemesis <list|nearby|info|hunt|army|territory|spawn|retire|clear|debug>");
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> handleList(sender);
            case "nearby" -> handleNearby(sender);
            case "info" -> handleInfo(sender, args);
            case "hunt" -> handleHunt(sender);
            case "army" -> handleArmy(sender);
            case "territory" -> handleTerritory(sender);
            case "spawn" -> handleSpawn(sender, args);
            case "retire" -> handleRetire(sender, args);
            case "clear" -> handleClear(sender, args);
            case "debug" -> handleDebug(sender, args);
            default -> sender.sendMessage("§cUsage: /nemesis <list|nearby|info|hunt|army|territory|spawn|retire|clear|debug>");
        }
    }

    private void handleTerritory(CommandSender sender) {
        if (!sender.hasPermission("lastbreathhc.nemesis")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        if (territoryPressureService == null || !territoryPressureService.enabled()) {
            sender.sendMessage("§eTerritory pressure subsystem disabled.");
            return;
        }
        Map<String, Double> snapshot = territoryPressureService.snapshot();
        if (snapshot.isEmpty()) {
            sender.sendMessage("§7No territory pressure tracked yet.");
            return;
        }
        sender.sendMessage("§6Territory pressure summary:");
        snapshot.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                sender.sendMessage("§7- §f" + entry.getKey() + ": §e" + String.format(Locale.US, "%.1f", entry.getValue())));
    }

    private void handleArmy(CommandSender sender) {
        if (!sender.hasPermission("lastbreathhc.nemesis")) {
            sender.sendMessage("§cNo permission.");
            return;
        }

        Map<String, List<CaptainRecord>> byRegion = registry.getAll().stream().collect(Collectors.groupingBy(record -> {
            if (record.political().isPresent() && record.political().get().region() != null && !record.political().get().region().isBlank()) {
                return record.political().get().region();
            }
            return "unassigned";
        }));

        if (byRegion.isEmpty()) {
            sender.sendMessage("§eNo captains are registered in the army graph.");
            return;
        }

        sender.sendMessage("§6Nemesis Army");
        byRegion.entrySet().stream().sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)).forEach(entry -> {
            sender.sendMessage("§7Region §f" + entry.getKey());
            for (Rank rank : Rank.values()) {
                List<CaptainRecord> ranked = entry.getValue().stream()
                        .filter(record -> Rank.from(record.political().map(CaptainRecord.Political::rank).orElse(Rank.CAPTAIN.name()), Rank.CAPTAIN) == rank)
                        .sorted(Comparator.comparingDouble((CaptainRecord record) -> record.political().map(CaptainRecord.Political::promotionScore).orElse(0.0)).reversed())
                        .limit(rank.slots())
                        .toList();

                List<String> slots = new ArrayList<>();
                for (CaptainRecord record : ranked) {
                    String name = record.naming() == null ? record.identity().captainId().toString().substring(0, 8) : record.naming().displayName();
                    double score = record.political().map(CaptainRecord.Political::promotionScore).orElse(0.0);
                    ArmyGraphService.ArmyLinks links = armyGraphService.linksOf(record.identity().captainId());
                    slots.add("§c" + name + " §8[" + String.format(Locale.US, "%.1f", score) + "|rivals=" + links.rivals().size() + "]");
                }
                while (slots.size() < rank.slots()) {
                    slots.add("§8<empty>");
                }
                sender.sendMessage("§8- §6" + rank.name() + " §7(" + ranked.size() + "/" + rank.slots() + ") §f" + String.join(" §7| ", slots));
            }
        });
    }

    private void handleNearby(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only.");
            return;
        }
        if (!sender.hasPermission("lastbreathhc.nemesis")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        captainListGUI.open(player);
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
        sender.sendMessage("§7Strengths: §f" + localize(record.traits().traits()));
        sender.sendMessage("§7Weaknesses: §f" + localize(record.traits().weaknesses()));
        sender.sendMessage("§7Immunities: §f" + localize(record.traits().immunities()));
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


    private void handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lastbreathhc.nemesis.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§cThis will clear all nemesis captains and minions.");
            sender.sendMessage("§eUsage: /nemesis clear confirm");
            return;
        }

        int captainsRemoved = registry.clearAll();
        armyGraphService.pruneMissingCaptains(java.util.Set.of());
        int minionsRemoved = minionController.despawnAllMinions();
        sender.sendMessage("§aNemesis data cleared. Captains removed: §e" + captainsRemoved + "§a, minions removed: §e" + minionsRemoved);
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
            sender.sendMessage("§7Subcommands: resolvekiller, dump, active, cleanupOrphans, resetPressure, clearRelationships, forcePromotion, habitat, cleanupHabitat");
            return;
        }

        String debugSub = args[1].toLowerCase(Locale.ROOT);
        switch (debugSub) {
            case "resolvekiller" -> handleDebugResolveKiller(sender, args);
            case "dump" -> handleDebugDump(sender, args);
            case "active" -> handleDebugActive(sender);
            case "cleanuporphans" -> handleDebugCleanupOrphans(sender);
            case "resetpressure" -> handleDebugResetPressure(sender);
            case "clearrelationships" -> handleDebugClearRelationships(sender, args);
            case "forcepromotion" -> handleDebugForcePromotion(sender, args);
            case "habitat" -> handleDebugHabitat(sender, args);
            case "cleanuphabitat" -> handleDebugCleanupHabitat(sender, args);
            default -> sender.sendMessage("§cUsage: /nemesis debug <resolvekiller|dump|active|cleanupOrphans|resetPressure|clearRelationships|forcePromotion|habitat|cleanupHabitat>");
        }
    }

    private void handleDebugResetPressure(CommandSender sender) {
        if (territoryPressureService == null || !territoryPressureService.enabled()) {
            sender.sendMessage("§eTerritory pressure subsystem disabled.");
            return;
        }
        territoryPressureService.resetAll();
        sender.sendMessage("§aTerritory pressure reset.");
    }

    private void handleDebugClearRelationships(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /nemesis debug clearRelationships <id>");
            return;
        }
        CaptainRecord record = findRecord(args[2]);
        if (record == null) {
            sender.sendMessage("§cCaptain not found.");
            return;
        }
        armyGraphService.clearRelationship(record.identity().captainId());
        sender.sendMessage("§aRelationships cleared for §e" + record.naming().displayName());
    }

    private void handleDebugForcePromotion(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /nemesis debug forcePromotion <id> <score>");
            return;
        }
        CaptainRecord record = findRecord(args[2]);
        if (record == null) {
            sender.sendMessage("§cCaptain not found.");
            return;
        }
        double score;
        try {
            score = Double.parseDouble(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid score.");
            return;
        }
        CaptainRecord.Political political = record.political().orElse(new CaptainRecord.Political(Rank.CAPTAIN.name(), "overworld", "", 0.0, 0.0));
        CaptainRecord updated = new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), record.naming(), record.traits(), record.minionPack(), record.state(), record.telemetry(), Optional.of(new CaptainRecord.Political(political.rank(), political.region(), political.seatId(), score, political.influence())), record.social(), record.relationships(), record.memory(), record.persona(), record.habitat());
        registry.upsert(updated);
        sender.sendMessage("§aPromotion score forced to §e" + score);
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
            if (!NemesisMobRules.isHostileOrAggressive(debug.resolvedKiller().entity())) {
                sender.sendMessage("§7Eligibility: §crejected (non-hostile/non-aggressive)");
            }
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
        sender.sendMessage("§7Scores: §f" + (record.nemesisScores() == null ? "none" : "threat=" + record.nemesisScores().threat() + " rivalry=" + record.nemesisScores().rivalry() + " brutality=" + record.nemesisScores().brutality()));
        sender.sendMessage("§7Victims: §f" + (record.victims() == null ? "none" : record.victims().totalVictimCount() + " recent=" + record.victims().playerVictims().size()));
        sender.sendMessage("§7Counters: §f" + (record.telemetry() == null ? "{}" : record.telemetry().counters()));
        sender.sendMessage("§7Political: §f" + formatSection(record.political()));
        sender.sendMessage("§7Social: §f" + formatSection(record.social()));
        sender.sendMessage("§7Relationships: §f" + formatSection(record.relationships()));
        sender.sendMessage("§7Memory: §f" + formatSection(record.memory()));
        sender.sendMessage("§7Persona: §f" + formatSection(record.persona()));
    }

    private String formatSection(Optional<?> section) {
        if (section == null) {
            return "none (null optional)";
        }
        if (section.isEmpty()) {
            return "default/none";
        }
        return section.get().toString();
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



    private void handleDebugHabitat(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /nemesis debug habitat <id>");
            return;
        }
        CaptainRecord record = findRecord(args[2]);
        if (record == null) {
            sender.sendMessage("§cCaptain not found.");
            return;
        }
        sender.sendMessage("§6Habitat linkage for §e" + record.naming().displayName());
        sender.sendMessage("§7Habitat: §f" + formatSection(record.habitat()));
        long owned = structureFootprintRepository.findByOwner(record.identity().captainId().toString()).count();
        sender.sendMessage("§7Owned footprints: §e" + owned);
    }

    private void handleDebugCleanupHabitat(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            CaptainRecord record = findRecord(args[2]);
            if (record == null) {
                sender.sendMessage("§cCaptain not found.");
                return;
            }
            CaptainHabitatService.DeathCleanupResult result = captainHabitatService.handlePermanentCaptainDeath(record.identity().captainId());
            sender.sendMessage("§aCleanup complete for captain. owned=" + result.ownedCount() + " decayed=" + result.decayedCount() + " abandoned=" + result.abandonedCount() + " behavior=" + result.behavior());
            return;
        }
        int count = captainHabitatService.cleanupMissingCaptainLinks();
        sender.sendMessage("§aHabitat cleanup completed. Reassigned orphan structures: §e" + count);
    }

    private String localize(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "None";
        }
        return ids.stream().map(traitRegistry::displayName).collect(java.util.stream.Collectors.joining(", "));
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
