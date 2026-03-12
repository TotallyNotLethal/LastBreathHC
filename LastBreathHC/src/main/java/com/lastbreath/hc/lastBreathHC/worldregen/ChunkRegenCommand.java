package com.lastbreath.hc.lastBreathHC.worldregen;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChunkRegenCommand implements BasicCommand {
    private static final String ADMIN_PERMISSION = "lastbreathhc.worldregen.admin";

    private final ChunkRegenManager manager;
    private final ChunkRegenSettings defaults;

    public ChunkRegenCommand(ChunkRegenManager manager, ChunkRegenSettings defaults) {
        this.manager = manager;
        this.defaults = defaults;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player) || !hasAccess(player)) {
            return List.of();
        }

        if (args.length == 0) {
            return List.of("regen");
        }
        if (args.length == 1) {
            return filter(args[0], List.of("regen"));
        }
        if (!"regen".equalsIgnoreCase(args[0])) {
            return List.of();
        }
        if (args.length == 2) {
            return filter(args[1], List.of("start", "stop", "status", "chunk", "reloadchunk", "rollback", "purge"));
        }
        if (args.length >= 3 && "start".equalsIgnoreCase(args[1])) {
            return filter(args[args.length - 1], List.of("radius", "aroundop", "unloadedonly"));
        }
        if (args.length >= 3 && "rollback".equalsIgnoreCase(args[1])) {
            return filter(args[args.length - 1], List.of("radius"));
        }
        if (args.length >= 3 && "reloadchunk".equalsIgnoreCase(args[1])) {
            return filter(args[args.length - 1], List.of("force"));
        }
        if (args.length >= 3 && "chunk".equalsIgnoreCase(args[1])) {
            return filter(args[args.length - 1], List.of("force"));
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }
        if (!hasAccess(player)) {
            player.sendMessage("§cNo permission.");
            return;
        }

        if (args.length < 2 || !"regen".equalsIgnoreCase(args[0])) {
            sendUsage(player);
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "start" -> handleStart(player, args);
            case "stop" -> {
                manager.stop();
                player.sendMessage("§eChunk regeneration stopped.");
            }
            case "status" -> {
                ChunkRegenManager.RegenStatus status = manager.status();
                player.sendMessage("§7Running: §f" + status.running());
                player.sendMessage("§7Queued: §f" + status.queuedChunks() + " §7Active scans: §f" + status.activeScans());
                player.sendMessage("§7Scanned: §f" + status.chunksScanned() + " §7Skipped: §f" + status.chunksSkipped() + " §7Regenerated: §f" + status.chunksRegenerated());
            }
            case "chunk" -> {
                boolean force = containsToken(args, "force");
                ChunkRegenManager.SingleChunkResult result = manager.regenerateCurrentChunk(player, force);
                player.sendMessage(result.message());
            }
            case "reloadchunk" -> {
                boolean force = containsToken(args, "force");
                ChunkRegenManager.SingleChunkResult result = manager.reloadCurrentChunk(player, force);
                player.sendMessage(result.message());
            }
            case "rollback" -> handleRollback(player, args);
            case "purge" -> {
                int deleted = manager.purgeBackups();
                player.sendMessage("§aPurged " + deleted + " chunk backup file(s).");
            }
            default -> sendUsage(player);
        }
    }

    private void handleStart(Player player, String[] args) {
        int radius = defaults.scanRadius();
        boolean aroundOp = false;
        boolean unloadedOnly = false;

        for (int i = 2; i < args.length; i++) {
            String token = args[i].toLowerCase(Locale.ROOT);
            if ("radius".equals(token) && i + 1 < args.length) {
                try {
                    radius = Math.max(16, Integer.parseInt(args[++i]));
                } catch (NumberFormatException ignored) {
                    player.sendMessage("§cInvalid radius.");
                    return;
                }
                continue;
            }
            if ("aroundop".equals(token)) {
                aroundOp = true;
                continue;
            }
            if ("unloadedonly".equals(token)) {
                unloadedOnly = true;
            }
        }

        Location center = player.getLocation();
        World world = player.getWorld();

        if (aroundOp) {
            Player opTarget = Bukkit.getOnlinePlayers().stream().filter(Player::isOp).findFirst().orElse(player);
            center = opTarget.getLocation();
            world = opTarget.getWorld();
        }

        ChunkRegenManager.RegenRunOptions options = new ChunkRegenManager.RegenRunOptions(
                world,
                center.getBlockX(),
                center.getBlockZ(),
                radius,
                unloadedOnly
        );

        if (manager.start(player, options)) {
            player.sendMessage("§aChunk regeneration started.");
        } else {
            player.sendMessage("§cUnable to start. It may already be running or disabled in config.");
        }
    }

    private void handleRollback(Player player, String[] args) {
        int radius = 16;
        for (int i = 2; i < args.length; i++) {
            if ("radius".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                try {
                    radius = Math.max(0, Integer.parseInt(args[++i]));
                } catch (NumberFormatException ignored) {
                    player.sendMessage("§cInvalid rollback radius.");
                    return;
                }
            }
        }
        int restored = manager.rollbackAround(player.getLocation(), radius);
        player.sendMessage("§aRollback restored " + restored + " chunk(s). Radius=" + radius + " blocks.");
    }

    private boolean hasAccess(Player player) {
        return player.isOp() || player.hasPermission(ADMIN_PERMISSION);
    }

    private List<String> filter(String input, List<String> options) {
        if (input == null || input.isBlank()) {
            return options;
        }
        List<String> matches = new ArrayList<>();
        String lowered = input.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.startsWith(lowered)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private boolean containsToken(String[] args, String token) {
        for (String arg : args) {
            if (token.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private void sendUsage(Player player) {
        player.sendMessage("§cUsage:");
        player.sendMessage("§7/lbhc regen start [radius <blocks>] [aroundop] [unloadedonly]");
        player.sendMessage("§7/lbhc regen stop");
        player.sendMessage("§7/lbhc regen status");
        player.sendMessage("§7/lbhc regen chunk [force]");
        player.sendMessage("§7/lbhc regen reloadchunk [force]");
        player.sendMessage("§7/lbhc regen rollback [radius <blocks>]");
        player.sendMessage("§7/lbhc regen purge");
    }
}
