package com.lastbreath.hc.lastBreathHC.mobs;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AggressiveLogoutMobManager implements Listener {

    private static final String FILE_NAME = "logout-aggro-mobs.yml";

    private final LastBreathHC plugin;
    private final File dataFile;
    private final Map<UUID, TrackedMob> trackedMobs = new HashMap<>();
    private final Map<UUID, Set<UUID>> mobsByPlayer = new HashMap<>();

    public AggressiveLogoutMobManager(LastBreathHC plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public void shutdown() {
        save();
        trackedMobs.clear();
        mobsByPlayer.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        tagAggressiveMobs(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        reapplyAggro(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        reapplyAggro(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (trackedMobs.isEmpty()) {
            return;
        }
        for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            TrackedMob tracked = trackedMobs.get(mob.getUniqueId());
            if (tracked == null) {
                continue;
            }
            Player player = Bukkit.getPlayer(tracked.playerId());
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (!mob.getWorld().equals(player.getWorld())) {
                continue;
            }
            mob.setRemoveWhenFarAway(false);
            mob.setTarget(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        UUID mobId = event.getEntity().getUniqueId();
        if (trackedMobs.containsKey(mobId)) {
            untrackMob(mobId);
            save();
        }
    }

    private void tagAggressiveMobs(Player player) {
        boolean changed = false;
        for (Mob mob : player.getWorld().getEntitiesByClass(Mob.class)) {
            if (mob.getTarget() == null) {
                continue;
            }
            if (!(mob.getTarget() instanceof Player targetPlayer)) {
                continue;
            }
            if (!targetPlayer.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (trackMob(player, mob)) {
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    private void reapplyAggro(Player player) {
        Set<UUID> mobIds = mobsByPlayer.get(player.getUniqueId());
        if (mobIds == null || mobIds.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (UUID mobId : new HashSet<>(mobIds)) {
            Entity entity = Bukkit.getEntity(mobId);
            if (!(entity instanceof Mob mob) || mob.isDead()) {
                untrackMob(mobId);
                changed = true;
                continue;
            }
            if (!mob.getWorld().equals(player.getWorld())) {
                continue;
            }
            mob.setRemoveWhenFarAway(false);
            mob.setTarget(player);
        }

        if (changed) {
            save();
        }
    }

    private boolean trackMob(Player player, Mob mob) {
        UUID mobId = mob.getUniqueId();
        if (trackedMobs.containsKey(mobId)) {
            return false;
        }
        Location location = mob.getLocation();
        TrackedMob tracked = new TrackedMob(
                mobId,
                player.getUniqueId(),
                location.getWorld() != null ? location.getWorld().getName() : null,
                location.getX(),
                location.getY(),
                location.getZ()
        );
        trackedMobs.put(mobId, tracked);
        mobsByPlayer.computeIfAbsent(player.getUniqueId(), key -> new HashSet<>()).add(mobId);
        mob.setRemoveWhenFarAway(false);
        return true;
    }

    private void untrackMob(UUID mobId) {
        TrackedMob tracked = trackedMobs.remove(mobId);
        if (tracked == null) {
            return;
        }
        Set<UUID> mobIds = mobsByPlayer.get(tracked.playerId());
        if (mobIds != null) {
            mobIds.remove(mobId);
            if (mobIds.isEmpty()) {
                mobsByPlayer.remove(tracked.playerId());
            }
        }
    }

    private void load() {
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = config.getConfigurationSection("mobs");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UUID mobId = parseUuid(key);
            UUID playerId = parseUuid(section.getString("player"));
            if (mobId == null || playerId == null) {
                continue;
            }
            String worldName = section.getString("world");
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            TrackedMob tracked = new TrackedMob(mobId, playerId, worldName, x, y, z);
            trackedMobs.put(mobId, tracked);
            mobsByPlayer.computeIfAbsent(playerId, keyId -> new HashSet<>()).add(mobId);
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (TrackedMob tracked : trackedMobs.values()) {
            String path = "mobs." + tracked.mobId();
            Entity entity = Bukkit.getEntity(tracked.mobId());
            TrackedMob snapshot = tracked;
            if (entity != null) {
                Location location = entity.getLocation();
                snapshot = new TrackedMob(
                        tracked.mobId(),
                        tracked.playerId(),
                        location.getWorld() != null ? location.getWorld().getName() : tracked.worldName(),
                        location.getX(),
                        location.getY(),
                        location.getZ()
                );
            }
            config.set(path + ".player", snapshot.playerId().toString());
            config.set(path + ".world", snapshot.worldName());
            config.set(path + ".x", snapshot.x());
            config.set(path + ".y", snapshot.y());
            config.set(path + ".z", snapshot.z());
        }
        dataFile.getParentFile().mkdirs();
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save logout aggro mob data.");
        }
    }

    private UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record TrackedMob(UUID mobId, UUID playerId, String worldName, double x, double y, double z) {
    }
}
