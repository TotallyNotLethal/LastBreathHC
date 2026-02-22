package com.lastbreath.hc.lastBreathHC.heads;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeadTrackingLogger {

    private static final String FILE_NAME = "player-head-tracking.yml";
    private final LastBreathHC plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, LocationSnapshot> placedHeads = new HashMap<>();

    public HeadTrackingLogger(LastBreathHC plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
        loadPlacedHeads();
    }

    public boolean hasPlacedHead(UUID ownerUuid) {
        return placedHeads.containsKey(ownerUuid);
    }

    public void recordHeadDropped(UUID ownerUuid, String ownerName, UUID recordId, UUID itemEntityUuid, Location dropLocation) {
        String path = nextEventPath();
        config.set(path + ".type", "HEAD_DROPPED");
        config.set(path + ".ownerUuid", ownerUuid.toString());
        config.set(path + ".ownerName", ownerName);
        config.set(path + ".recordId", recordId.toString());
        config.set(path + ".itemEntityUuid", itemEntityUuid.toString());
        setLocation(path + ".location", dropLocation);
        save();
    }

    public void recordHeadPlaced(UUID ownerUuid, String ownerName, UUID recordId, Player placedBy, Location placedLocation) {
        String path = nextEventPath();
        config.set(path + ".type", "HEAD_PLACED");
        config.set(path + ".ownerUuid", ownerUuid.toString());
        config.set(path + ".ownerName", ownerName);
        config.set(path + ".recordId", recordId.toString());
        if (placedBy != null) {
            config.set(path + ".actorUuid", placedBy.getUniqueId().toString());
            config.set(path + ".actorName", placedBy.getName());
        }
        setLocation(path + ".location", placedLocation);

        placedHeads.put(ownerUuid, LocationSnapshot.from(placedLocation));
        String placedPath = "placed." + ownerUuid;
        config.set(placedPath + ".recordId", recordId.toString());
        config.set(placedPath + ".ownerName", ownerName);
        if (placedBy != null) {
            config.set(placedPath + ".actorUuid", placedBy.getUniqueId().toString());
            config.set(placedPath + ".actorName", placedBy.getName());
        }
        setLocation(placedPath + ".location", placedLocation);
        config.set(placedPath + ".updatedAt", Instant.now().toString());
        save();
    }

    public void recordHeadUnplaced(UUID ownerUuid, String reason, Player actor, Location location) {
        String path = nextEventPath();
        config.set(path + ".type", "HEAD_UNPLACED");
        config.set(path + ".ownerUuid", ownerUuid.toString());
        config.set(path + ".reason", reason);
        if (actor != null) {
            config.set(path + ".actorUuid", actor.getUniqueId().toString());
            config.set(path + ".actorName", actor.getName());
        }
        setLocation(path + ".location", location);

        placedHeads.remove(ownerUuid);
        config.set("placed." + ownerUuid, null);
        save();
    }

    private void loadPlacedHeads() {
        placedHeads.clear();
        if (!config.isConfigurationSection("placed")) {
            return;
        }

        for (String ownerRaw : config.getConfigurationSection("placed").getKeys(false)) {
            try {
                UUID ownerId = UUID.fromString(ownerRaw);
                LocationSnapshot snapshot = readLocation("placed." + ownerRaw + ".location");
                if (snapshot != null) {
                    placedHeads.put(ownerId, snapshot);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private String nextEventPath() {
        long now = System.currentTimeMillis();
        return "events." + now + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void setLocation(String path, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getBlockX());
        config.set(path + ".y", location.getBlockY());
        config.set(path + ".z", location.getBlockZ());
    }

    private LocationSnapshot readLocation(String path) {
        String worldName = config.getString(path + ".world");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        return new LocationSnapshot(
                worldName,
                config.getInt(path + ".x"),
                config.getInt(path + ".y"),
                config.getInt(path + ".z")
        );
    }

    private void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create data directory for head tracking logs.");
            return;
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player head tracking log.");
        }
    }

    private record LocationSnapshot(String worldName, int x, int y, int z) {
        static LocationSnapshot from(Location location) {
            World world = location.getWorld();
            return new LocationSnapshot(world == null ? "unknown" : world.getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ());
        }
    }
}
