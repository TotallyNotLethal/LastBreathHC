package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.team.TeamManager;
import com.lastbreath.hc.lastBreathHC.team.TeamWaypoint;
import com.lastbreath.hc.lastBreathHC.team.TeamWaypointManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WaypointCommand implements BasicCommand {

    private final LastBreathHC plugin;
    private final TeamManager teamManager;
    private final TeamWaypointManager waypointManager;

    public WaypointCommand(LastBreathHC plugin, TeamManager teamManager, TeamWaypointManager waypointManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.waypointManager = waypointManager;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("set", "clear");
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(ChatColor.RED + "Only players can use waypoints.");
            return;
        }

        if (!plugin.getConfig().getBoolean("waypoint.enabled", true)) {
            player.sendMessage(ChatColor.RED + "Waypoints are disabled.");
            return;
        }

        Optional<Team> team = teamManager.getTeam(player);
        if (team.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not on a team.");
            return;
        }

        if (args.length == 0) {
            showWaypoint(player, team.get());
            return;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "set" -> handleSet(player, team.get(), args);
            case "clear" -> handleClear(player, team.get());
            default -> showWaypoint(player, team.get());
        }
    }

    private void handleSet(Player player, Team team, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /waypoint set <name>");
            return;
        }

        String label = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Location location = player.getLocation();
        TeamWaypoint waypoint = new TeamWaypoint(label, location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
        waypointManager.setWaypoint(team, waypoint);
        waypointManager.save();

        String message = format("waypoint.messages.set", ChatColor.GREEN + "%player% set the team waypoint to %label%.");
        notifyTeam(teamManager.getOnlineTeamMembers(player),
                message.replace("%label%", label).replace("%player%", player.getName()),
                player);
    }

    private void handleClear(Player player, Team team) {
        Optional<TeamWaypoint> existing = waypointManager.getWaypoint(team);
        if (existing.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Your team does not have a waypoint.");
            return;
        }

        waypointManager.clearWaypoint(team);
        waypointManager.save();

        String message = format("waypoint.messages.cleared", ChatColor.YELLOW + "%player% cleared the team waypoint.");
        notifyTeam(teamManager.getOnlineTeamMembers(player),
                message.replace("%player%", player.getName()),
                player);
    }

    private void showWaypoint(Player player, Team team) {
        Optional<TeamWaypoint> waypoint = waypointManager.getWaypoint(team);
        if (waypoint.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Your team does not have a waypoint.");
            return;
        }

        TeamWaypoint waypointValue = waypoint.get();
        String distance = waypointManager.describeDistanceAndDirection(player, waypointValue);
        String base = format("waypoint.messages.view",
                ChatColor.AQUA + "Team waypoint: %label% (%world%) - %distance%");

        String worldName = waypointValue.getWorldName();
        String message = base
                .replace("%label%", waypointValue.getLabel())
                .replace("%world%", worldName)
                .replace("%distance%", distance);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void notifyTeam(Set<Player> recipients, String message, Player source) {
        if (recipients.isEmpty()) {
            source.sendMessage(ChatColor.RED + "You are not on a team.");
            return;
        }
        for (Player recipient : recipients) {
            recipient.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private String format(String path, String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }
}
