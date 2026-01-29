package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.team.TeamManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EmergencyPingCommand implements BasicCommand {

    private static final String COOLDOWN_PATH = "emergencyPing.cooldownSeconds";
    private final LastBreathHC plugin;
    private final TeamManager teamManager;
    private final Map<UUID, Instant> cooldowns = new HashMap<>();

    public EmergencyPingCommand(LastBreathHC plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(ChatColor.RED + "Only players can use emergency ping.");
            return;
        }

        Set<Player> recipients = teamManager.getOnlineTeamMembers(player);
        if (recipients.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not on a team to send an emergency ping.");
            return;
        }

        int cooldownSeconds = Math.max(0, plugin.getConfig().getInt(COOLDOWN_PATH, 300));
        Instant now = Instant.now();
        Instant lastPing = cooldowns.get(player.getUniqueId());
        if (lastPing != null) {
            Duration elapsed = Duration.between(lastPing, now);
            if (elapsed.compareTo(Duration.ofSeconds(cooldownSeconds)) < 0) {
                long remaining = Math.max(1L, cooldownSeconds - elapsed.getSeconds());
                player.sendMessage(ChatColor.RED + "Please wait " + remaining + "s before pinging again.");
                return;
            }
        }

        Location location = player.getLocation();
        String worldLabel = describeWorld(location.getWorld());

        for (Player recipient : recipients) {
            long distance = approximateDistance(location, recipient.getLocation());
            String message = ChatColor.YELLOW + "⚠ " + player.getName()
                    + " pinged for help (" + worldLabel + ", ~" + distance + " blocks away)";
            recipient.sendMessage(message);
        }

        cooldowns.put(player.getUniqueId(), now);
    }

    private long approximateDistance(Location origin, Location target) {
        return Math.round(origin.toVector().distance(target.toVector()));
    }

    private String describeWorld(World world) {
        if (world == null) {
            return "Unknown";
        }

        return switch (world.getEnvironment()) {
            case NORMAL -> "Overworld";
            case NETHER -> "Nether";
            case THE_END -> "The End";
            default -> world.getName();
        };
    }
}
