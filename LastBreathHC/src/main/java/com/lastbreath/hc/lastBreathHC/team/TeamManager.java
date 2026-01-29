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

    public enum JoinOutcome {
        JOINED,
        REQUESTED,
        REQUEST_ALREADY_PENDING,
        ALREADY_MEMBER,
        FAILED
    }

    public enum LeaveOutcome {
        NOT_IN_TEAM,
        LEFT,
        OWNER_TRANSFERRED,
        DISBANDED
    }

    private final LastBreathHC plugin;
    private final File ownerFile;
    private final File dataFile;
    private final Map<String, UUID> teamOwners = new HashMap<>();
    private final Map<String, Boolean> teamLocks = new HashMap<>();
    private final Map<String, Map<UUID, String>> teamJoinRequests = new HashMap<>();

    public TeamManager(LastBreathHC plugin) {
        this.plugin = plugin;
        this.ownerFile = new File(plugin.getDataFolder(), "team-owners.yml");
        this.dataFile = new File(plugin.getDataFolder(), "team-data.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        loadTeams();
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
        setJoinLocked(team, false);
        return Optional.of(team);
    }

    public JoinOutcome joinTeam(Player player, Team team) {
        if (team == null) {
            return JoinOutcome.FAILED;
        }
        if (team.hasEntry(player.getName())) {
            return JoinOutcome.ALREADY_MEMBER;
        }
        if (isJoinLocked(team) && !isOwner(player, team)) {
            String teamKey = normalizeTeamKey(team.getName());
            Map<UUID, String> requests = teamJoinRequests.computeIfAbsent(teamKey, key -> new HashMap<>());
            if (requests.containsKey(player.getUniqueId())) {
                return JoinOutcome.REQUEST_ALREADY_PENDING;
            }
            requests.put(player.getUniqueId(), player.getName());
            saveAll();
            return JoinOutcome.REQUESTED;
        }
        getTeam(player).ifPresent(current -> current.removeEntry(player.getName()));
        boolean joined = team.addEntry(player.getName());
        if (joined && getOwner(team).isEmpty()) {
            setOwner(team, player.getUniqueId());
        }
        if (joined) {
            Map<UUID, String> requests = teamJoinRequests.get(normalizeTeamKey(team.getName()));
            if (requests != null) {
                requests.remove(player.getUniqueId());
            }
            saveAll();
            return JoinOutcome.JOINED;
        }
        return JoinOutcome.FAILED;
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

    public boolean isJoinLocked(Team team) {
        if (team == null) {
            return false;
        }
        return teamLocks.getOrDefault(normalizeTeamKey(team.getName()), false);
    }

    public void setJoinLocked(Team team, boolean locked) {
        if (team == null) {
            return;
        }
        teamLocks.put(normalizeTeamKey(team.getName()), locked);
        saveAll();
    }

    public Map<UUID, String> getJoinRequests(Team team) {
        if (team == null) {
            return Map.of();
        }
        return Map.copyOf(teamJoinRequests.getOrDefault(normalizeTeamKey(team.getName()), Map.of()));
    }

    public boolean approveJoinRequest(Team team, OfflinePlayer target) {
        if (team == null || target == null) {
            return false;
        }
        String teamKey = normalizeTeamKey(team.getName());
        Map<UUID, String> requests = teamJoinRequests.get(teamKey);
        if (requests == null || !requests.containsKey(target.getUniqueId())) {
            return false;
        }
        String name = target.getName();
        if (name == null) {
            name = requests.get(target.getUniqueId());
        }
        if (name == null) {
            return false;
        }
        requests.remove(target.getUniqueId());
        team.addEntry(name);
        saveAll();
        return true;
    }

    public boolean denyJoinRequest(Team team, OfflinePlayer target) {
        if (team == null || target == null) {
            return false;
        }
        String teamKey = normalizeTeamKey(team.getName());
        Map<UUID, String> requests = teamJoinRequests.get(teamKey);
        if (requests == null) {
            return false;
        }
        if (requests.remove(target.getUniqueId()) != null) {
            saveAll();
            return true;
        }
        return false;
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
        saveAll();
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
            saveAll();
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
        saveAll();
        return true;
    }

    public void disbandTeam(Team team) {
        if (team == null) {
            return;
        }
        String teamKey = normalizeTeamKey(team.getName());
        teamOwners.remove(teamKey);
        teamLocks.remove(teamKey);
        teamJoinRequests.remove(teamKey);
        saveAll();
        team.unregister();
    }

    public Set<Player> getOnlineTeamMembers(Player player) {
        Optional<Team> team = getTeam(player);
        if (team.isEmpty()) {
            return Set.of();
        }

        return getOnlineTeamMembers(team.get());
    }

    public Set<Player> getOnlineTeamMembers(Team team) {
        if (team == null) {
            return Set.of();
        }

        Set<Player> members = new LinkedHashSet<>();
        for (String entry : team.getEntries()) {
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

    private void loadTeams() {
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return;
        }

        if (!dataFile.exists()) {
            loadOwners();
            for (Team team : scoreboard.getTeams()) {
                String teamKey = normalizeTeamKey(team.getName());
                teamLocks.put(teamKey, false);
                teamJoinRequests.put(teamKey, new HashMap<>());
            }
            saveAll();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        org.bukkit.configuration.ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection == null) {
            return;
        }

        for (String key : teamsSection.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection section = teamsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String teamName = section.getString("name", key);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            } else {
                for (String entry : Set.copyOf(team.getEntries())) {
                    team.removeEntry(entry);
                }
            }

            for (String entry : section.getStringList("members")) {
                if (entry != null && !entry.isBlank()) {
                    team.addEntry(entry);
                }
            }

            String ownerRaw = section.getString("owner");
            if (ownerRaw != null) {
                try {
                    teamOwners.put(normalizeTeamKey(teamName), UUID.fromString(ownerRaw));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid team owner UUID for team " + teamName);
                }
            }

            teamLocks.put(normalizeTeamKey(teamName), section.getBoolean("locked", false));
            Map<UUID, String> requests = new HashMap<>();
            org.bukkit.configuration.ConfigurationSection requestSection = section.getConfigurationSection("requests");
            if (requestSection != null) {
                for (String requestKey : requestSection.getKeys(false)) {
                    String name = requestSection.getString(requestKey);
                    try {
                        UUID uuid = UUID.fromString(requestKey);
                        requests.put(uuid, name);
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Invalid join request UUID for team " + teamName);
                    }
                }
            }
            teamJoinRequests.put(normalizeTeamKey(teamName), requests);
        }
    }

    public void saveAll() {
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return;
        }
        FileConfiguration config = new YamlConfiguration();
        for (Team team : scoreboard.getTeams()) {
            String teamKey = normalizeTeamKey(team.getName());
            String path = "teams." + teamKey;
            config.set(path + ".name", team.getName());
            UUID ownerId = teamOwners.get(teamKey);
            if (ownerId != null) {
                config.set(path + ".owner", ownerId.toString());
            }
            config.set(path + ".locked", teamLocks.getOrDefault(teamKey, false));
            config.set(path + ".members", team.getEntries().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList());
            Map<UUID, String> requests = teamJoinRequests.getOrDefault(teamKey, Map.of());
            if (!requests.isEmpty()) {
                for (Map.Entry<UUID, String> entry : requests.entrySet()) {
                    config.set(path + ".requests." + entry.getKey(), entry.getValue());
                }
            }
        }
        dataFile.getParentFile().mkdirs();
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save team data.");
        }
    }
}
