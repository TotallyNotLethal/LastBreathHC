package com.lastbreath.hc.lastBreathHC.team;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TeamManager {

    public enum LeaveOutcome {
        NOT_IN_TEAM,
        LEFT,
        OWNER_TRANSFERRED,
        DISBANDED
    }

    private final LastBreathHC plugin;
    private final File ownerFile;
    private final Map<String, UUID> teamOwners = new HashMap<>();

    public TeamManager(LastBreathHC plugin) {
        this.plugin = plugin;
        this.ownerFile = new File(plugin.getDataFolder(), "team-owners.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        loadOwners();
    }

    public Optional<Team> getTeam(Player player) {
        Team team = getMainScoreboardTeam(player);
        if (team != null) {
            return Optional.of(team);
        }

        return Optional.ofNullable(player.getScoreboard().getEntryTeam(player.getName()));
    }

    public Optional<Team> getTeamByName(String name) {
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return Optional.empty();
        }
        Team direct = scoreboard.getTeam(name);
        if (direct != null) {
            return Optional.of(direct);
        }
        return scoreboard.getTeams().stream()
                .filter(team -> team.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<Team> createTeam(String name, Player owner) {
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return Optional.empty();
        }
        Team existing = scoreboard.getTeam(name);
        if (existing != null) {
            return Optional.of(existing);
        }
        Team team = scoreboard.registerNewTeam(name);
        setOwner(team, owner.getUniqueId());
        return Optional.of(team);
    }

    public boolean joinTeam(Player player, Team team) {
        if (team == null) {
            return false;
        }
        getTeam(player).ifPresent(current -> current.removeEntry(player.getName()));
        boolean joined = team.addEntry(player.getName());
        if (joined && getOwner(team).isEmpty()) {
            setOwner(team, player.getUniqueId());
        }
        return joined;
    }

    public Collection<String> getTeamNames() {
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return Set.of();
        }
        return scoreboard.getTeams().stream()
                .map(Team::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public Collection<Team> getTeams() {
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return Set.of();
        }
        return scoreboard.getTeams();
    }

    public Optional<UUID> getOwner(Team team) {
        if (team == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(teamOwners.get(normalizeTeamKey(team.getName())));
    }

    public boolean isOwner(Player player, Team team) {
        return getOwner(team)
                .map(ownerId -> ownerId.equals(player.getUniqueId()))
                .orElse(false);
    }

    public void setOwner(Team team, UUID ownerId) {
        if (team == null || ownerId == null) {
            return;
        }
        teamOwners.put(normalizeTeamKey(team.getName()), ownerId);
        saveOwners();
    }

    public LeaveOutcome leaveTeam(Player player) {
        Optional<Team> teamOptional = getTeam(player);
        if (teamOptional.isEmpty()) {
            return LeaveOutcome.NOT_IN_TEAM;
        }

        Team team = teamOptional.get();
        boolean wasOwner = isOwner(player, team);
        team.removeEntry(player.getName());

        if (!wasOwner) {
            return LeaveOutcome.LEFT;
        }

        if (team.getEntries().isEmpty()) {
            disbandTeam(team);
            return LeaveOutcome.DISBANDED;
        }

        OfflinePlayer nextOwner = team.getEntries().stream()
                .map(Bukkit::getOfflinePlayer)
                .findFirst()
                .orElse(null);
        if (nextOwner != null) {
            setOwner(team, nextOwner.getUniqueId());
            return LeaveOutcome.OWNER_TRANSFERRED;
        }

        disbandTeam(team);
        return LeaveOutcome.DISBANDED;
    }

    public boolean kickMember(Player owner, Team team, OfflinePlayer target) {
        if (team == null || owner == null || target == null) {
            return false;
        }
        if (!isOwner(owner, team)) {
            return false;
        }
        if (target.getUniqueId().equals(owner.getUniqueId())) {
            return false;
        }
        if (!team.hasEntry(target.getName())) {
            return false;
        }
        team.removeEntry(target.getName());
        return true;
    }

    public void disbandTeam(Team team) {
        if (team == null) {
            return;
        }
        teamOwners.remove(normalizeTeamKey(team.getName()));
        saveOwners();
        team.unregister();
    }

    public Set<Player> getOnlineTeamMembers(Player player) {
        Optional<Team> team = getTeam(player);
        if (team.isEmpty()) {
            return Set.of();
        }

        Set<Player> members = new LinkedHashSet<>();
        for (String entry : team.get().getEntries()) {
            Player online = Bukkit.getPlayerExact(entry);
            if (online != null) {
                members.add(online);
            }
        }

        return members;
    }

    private Team getMainScoreboardTeam(Player player) {
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return null;
        }
        return scoreboard.getEntryTeam(player.getName());
    }

    private Scoreboard getMainScoreboard() {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            return null;
        }
        return scoreboardManager.getMainScoreboard();
    }

    private String normalizeTeamKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private void loadOwners() {
        if (!ownerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(ownerFile);
        org.bukkit.configuration.ConfigurationSection ownersSection = config.getConfigurationSection("owners");
        if (ownersSection == null) {
            return;
        }
        for (String key : ownersSection.getKeys(false)) {
            String raw = ownersSection.getString(key);
            if (raw == null) {
                continue;
            }
            try {
                teamOwners.put(normalizeTeamKey(key), UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid team owner UUID for team " + key);
            }
        }
    }

    public void saveOwners() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, UUID> entry : teamOwners.entrySet()) {
            config.set("owners." + entry.getKey(), entry.getValue().toString());
        }
        try {
            config.save(ownerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save team owner data.");
        }
    }
}
