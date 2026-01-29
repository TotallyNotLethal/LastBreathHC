package com.lastbreath.hc.lastBreathHC.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class TeamManager {

    public Optional<Team> getTeam(Player player) {
        Team team = getMainScoreboardTeam(player);
        if (team != null) {
            return Optional.of(team);
        }

        return Optional.ofNullable(player.getScoreboard().getEntryTeam(player.getName()));
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
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            return null;
        }

        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();
        return scoreboard.getEntryTeam(player.getName());
    }
}
