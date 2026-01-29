package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.gui.TeamManagementGUI;
import com.lastbreath.hc.lastBreathHC.team.TeamManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TeamCommand implements BasicCommand {

    private final TeamManager teamManager;
    private final TeamManagementGUI teamManagementGUI;

    public TeamCommand(TeamManager teamManager, TeamManagementGUI teamManagementGUI) {
        this.teamManager = teamManager;
        this.teamManagementGUI = teamManagementGUI;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("create", "join", "leave", "kick", "lock", "unlock", "requests", "accept", "deny", "gui");
        }
        if (args.length == 2 && "join".equalsIgnoreCase(args[0])) {
            return teamManager.getTeamNames().stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && "kick".equalsIgnoreCase(args[0]) && source.getSender() instanceof Player player) {
            return teamManager.getTeam(player)
                    .filter(team -> teamManager.isOwner(player, team))
                    .map(team -> team.getEntries().stream()
                            .filter(entry -> entry.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .toList())
                    .orElse(List.of());
        }
        if (args.length == 2
                && ("accept".equalsIgnoreCase(args[0]) || "deny".equalsIgnoreCase(args[0]))
                && source.getSender() instanceof Player player) {
            return teamManager.getTeam(player)
                    .filter(team -> teamManager.isOwner(player, team))
                    .map(team -> teamManager.getJoinRequests(team).values().stream()
                            .filter(name -> name != null && name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .toList())
                    .orElse(List.of());
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(ChatColor.RED + "Only players can manage teams.");
            return;
        }

        if (args.length == 0) {
            teamManagementGUI.open(player);
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String teamName = args.length > 1 ? args[1] : "";

        if (args.length > 2 && ("create".equals(action) || "join".equals(action))) {
            player.sendMessage(ChatColor.RED + "Team names cannot contain spaces.");
            return;
        }

        if (!teamName.isEmpty() && teamName.length() > 16 && "create".equals(action)) {
            player.sendMessage(ChatColor.RED + "Team names must be 16 characters or fewer.");
            return;
        }

        switch (action) {
            case "create" -> handleCreate(player, teamName);
            case "join" -> handleJoin(player, teamName);
            case "leave" -> handleLeave(player);
            case "kick" -> handleKick(player, args);
            case "lock" -> handleLock(player, true);
            case "unlock" -> handleLock(player, false);
            case "requests" -> handleRequests(player);
            case "accept" -> handleRequestDecision(player, args, true);
            case "deny" -> handleRequestDecision(player, args, false);
            case "gui" -> teamManagementGUI.open(player);
            default -> player.sendMessage(ChatColor.RED + "Usage: /team <create|join|leave|kick|lock|unlock|requests|accept|deny|gui> <name>");
        }
    }

    private void handleCreate(Player player, String teamName) {
        if (teamName.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Usage: /team create <name>");
            return;
        }
        Optional<Team> existing = teamManager.getTeamByName(teamName);
        if (existing.isPresent()) {
            player.sendMessage(ChatColor.RED + "That team already exists.");
            return;
        }

        Optional<Team> team = teamManager.createTeam(teamName, player);
        if (team.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Unable to create a team right now.");
            return;
        }

        teamManager.joinTeam(player, team.get());
        player.sendMessage(ChatColor.GREEN + "Created and joined team " + teamName + ".");
    }

    private void handleJoin(Player player, String teamName) {
        if (teamName.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Usage: /team join <name>");
            return;
        }
        Optional<Team> team = teamManager.getTeamByName(teamName);
        if (team.isEmpty()) {
            player.sendMessage(ChatColor.RED + "That team does not exist.");
            return;
        }

        TeamManager.JoinOutcome outcome = teamManager.joinTeam(player, team.get());
        switch (outcome) {
            case JOINED -> player.sendMessage(ChatColor.GREEN + "Joined team " + teamName + ".");
            case REQUESTED -> {
                player.sendMessage(ChatColor.YELLOW + "Join request sent to the team owner.");
                notifyOwnerOfRequest(team.get(), player);
            }
            case REQUEST_ALREADY_PENDING -> player.sendMessage(ChatColor.YELLOW + "You already have a pending join request.");
            case ALREADY_MEMBER -> player.sendMessage(ChatColor.YELLOW + "You are already on that team.");
            case FAILED -> player.sendMessage(ChatColor.RED + "Unable to join that team.");
        }
    }

    private void handleLeave(Player player) {
        TeamManager.LeaveOutcome outcome = teamManager.leaveTeam(player);
        switch (outcome) {
            case NOT_IN_TEAM -> player.sendMessage(ChatColor.RED + "You are not on a team.");
            case DISBANDED -> player.sendMessage(ChatColor.RED + "Team disbanded.");
            case OWNER_TRANSFERRED -> player.sendMessage(ChatColor.YELLOW + "You left and ownership was transferred.");
            case LEFT -> player.sendMessage(ChatColor.YELLOW + "You left the team.");
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team kick <player>");
            return;
        }
        Optional<Team> team = teamManager.getTeam(player);
        if (team.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not on a team.");
            return;
        }
        if (!teamManager.isOwner(player, team.get())) {
            player.sendMessage(ChatColor.RED + "Only the team owner can kick members.");
            return;
        }
        String targetName = args[1];
        if (!team.get().hasEntry(targetName)) {
            player.sendMessage(ChatColor.RED + "That player is not on your team.");
            return;
        }
        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(ChatColor.RED + "You cannot kick yourself.");
            return;
        }
        if (teamManager.kickMember(player, team.get(), player.getServer().getOfflinePlayer(targetName))) {
            player.sendMessage(ChatColor.YELLOW + "Kicked " + targetName + " from the team.");
            Player online = player.getServer().getPlayerExact(targetName);
            if (online != null) {
                online.sendMessage(ChatColor.RED + "You were kicked from " + team.get().getName() + ".");
            }
        }
    }

    private void handleLock(Player player, boolean locked) {
        Optional<Team> team = teamManager.getTeam(player);
        if (team.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not on a team.");
            return;
        }
        if (!teamManager.isOwner(player, team.get())) {
            player.sendMessage(ChatColor.RED + "Only the team owner can change join settings.");
            return;
        }
        teamManager.setJoinLocked(team.get(), locked);
        player.sendMessage(locked
                ? ChatColor.YELLOW + "Team joining is now locked (requests only)."
                : ChatColor.GREEN + "Team joining is now open.");
    }

    private void handleRequests(Player player) {
        Optional<Team> team = teamManager.getTeam(player);
        if (team.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not on a team.");
            return;
        }
        if (!teamManager.isOwner(player, team.get())) {
            player.sendMessage(ChatColor.RED + "Only the team owner can view join requests.");
            return;
        }
        List<String> requests = teamManager.getJoinRequests(team.get()).values().stream()
                .filter(name -> name != null && !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (requests.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No pending join requests.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Pending join requests: " + ChatColor.YELLOW + String.join(", ", requests));
    }

    private void handleRequestDecision(Player player, String[] args, boolean accept) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team " + (accept ? "accept" : "deny") + " <player>");
            return;
        }
        Optional<Team> team = teamManager.getTeam(player);
        if (team.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not on a team.");
            return;
        }
        if (!teamManager.isOwner(player, team.get())) {
            player.sendMessage(ChatColor.RED + "Only the team owner can manage join requests.");
            return;
        }
        String targetName = args[1];
        if (accept) {
            if (teamManager.approveJoinRequest(team.get(), player.getServer().getOfflinePlayer(targetName))) {
                player.sendMessage(ChatColor.GREEN + "Accepted join request from " + targetName + ".");
                Player online = player.getServer().getPlayerExact(targetName);
                if (online != null) {
                    online.sendMessage(ChatColor.GREEN + "Your join request was accepted.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "No pending join request for " + targetName + ".");
            }
            return;
        }
        if (teamManager.denyJoinRequest(team.get(), player.getServer().getOfflinePlayer(targetName))) {
            player.sendMessage(ChatColor.YELLOW + "Denied join request from " + targetName + ".");
            Player online = player.getServer().getPlayerExact(targetName);
            if (online != null) {
                online.sendMessage(ChatColor.RED + "Your join request was denied.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "No pending join request for " + targetName + ".");
        }
    }

    private void notifyOwnerOfRequest(Team team, Player requester) {
        teamManager.getOwner(team)
                .map(requester.getServer()::getPlayer)
                .ifPresent(owner -> owner.sendMessage(ChatColor.YELLOW + requester.getName()
                        + " requested to join your team. Use /team accept " + requester.getName() + " or /team deny " + requester.getName() + "."));
    }
}
