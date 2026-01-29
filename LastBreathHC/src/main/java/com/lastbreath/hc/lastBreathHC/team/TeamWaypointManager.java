package com.lastbreath.hc.lastBreathHC.team;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class TeamWaypointManager {

    private static final String ROOT_KEY = "teams";
    private final File dataFile;
    private final Map<String, TeamWaypoint> waypoints = new HashMap<>();

    public TeamWaypointManager(File dataFile) {
        this.dataFile = dataFile;
    }

    public void load() {
        waypoints.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = config.getConfigurationSection(ROOT_KEY);
        if (root == null) {
            return;
        }

        for (String teamKey : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(teamKey);
            if (section == null) {
                continue;
            }

            String label = section.getString("label", "Waypoint");
            String worldName = section.getString("world");
            if (worldName == null) {
                continue;
            }
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            waypoints.put(teamKey, new TeamWaypoint(label, worldName, x, y, z));
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, TeamWaypoint> entry : waypoints.entrySet()) {
            String path = ROOT_KEY + "." + entry.getKey();
            TeamWaypoint waypoint = entry.getValue();
            config.set(path + ".label", waypoint.getLabel());
            config.set(path + ".world", waypoint.getWorldName());
            config.set(path + ".x", waypoint.getX());
            config.set(path + ".y", waypoint.getY());
            config.set(path + ".z", waypoint.getZ());
        }

        dataFile.getParentFile().mkdirs();
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Optional<TeamWaypoint> getWaypoint(Team team) {
        if (team == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(waypoints.get(normalizeTeamKey(team.getName())));
    }

    public Optional<TeamWaypoint> getWaypoint(Player player, TeamManager teamManager) {
        return teamManager.getTeam(player)
                .flatMap(this::getWaypoint);
    }

    public void setWaypoint(Team team, TeamWaypoint waypoint) {
        if (team == null) {
            return;
        }
        waypoints.put(normalizeTeamKey(team.getName()), waypoint);
        save();
    }

    public void clearWaypoint(Team team) {
        if (team == null) {
            return;
        }
        waypoints.remove(normalizeTeamKey(team.getName()));
        save();
    }

    public Location toLocation(TeamWaypoint waypoint) {
        World world = Bukkit.getWorld(waypoint.getWorldName());
        if (world == null) {
            return null;
        }
        return new Location(world, waypoint.getX(), waypoint.getY(), waypoint.getZ());
    }

    public String describeDistanceAndDirection(Player viewer, TeamWaypoint waypoint) {
        Location target = toLocation(waypoint);
        if (target == null) {
            return ChatColor.RED + "(unknown world)";
        }

        Location origin = viewer.getLocation();
        if (origin.getWorld() != null && !origin.getWorld().equals(target.getWorld())) {
            return ChatColor.RED + "(different world)";
        }
        double distance = origin.distance(target);
        String direction = describeDirection(origin, target);
        return ChatColor.YELLOW + "~" + Math.round(distance) + " blocks " + direction;
    }

    private String describeDirection(Location origin, Location target) {
        Vector diff = target.toVector().subtract(origin.toVector());
        if (diff.lengthSquared() == 0) {
            return "(here)";
        }

        double angle = Math.toDegrees(Math.atan2(diff.getZ(), diff.getX()));
        if (angle < 0) {
            angle += 360.0;
        }

        String[] directions = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
        int index = (int) Math.round(angle / 45.0) % directions.length;
        return "(" + directions[index] + ")";
    }

    private String normalizeTeamKey(String name) {
        return Objects.requireNonNullElse(name, "").toLowerCase(Locale.ROOT);
    }
}
