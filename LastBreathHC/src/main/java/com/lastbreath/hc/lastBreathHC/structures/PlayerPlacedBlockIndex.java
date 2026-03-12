package com.lastbreath.hc.lastBreathHC.structures;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPlacedBlockIndex implements Listener {
    private final LastBreathHC plugin;
    private final File dataFile;
    private final Map<String, Set<Long>> coordinatesByWorld = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    public PlayerPlacedBlockIndex(LastBreathHC plugin, File dataFile) {
        this.plugin = plugin;
        this.dataFile = dataFile;
    }

    public void load() {
        coordinatesByWorld.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection worldsSection = yaml.getConfigurationSection("worlds");
        if (worldsSection == null) {
            return;
        }

        for (String worldKey : worldsSection.getKeys(false)) {
            Set<Long> set = ConcurrentHashMap.newKeySet();
            for (String packed : worldsSection.getStringList(worldKey)) {
                try {
                    set.add(Long.parseLong(packed));
                } catch (NumberFormatException ignored) {
                }
            }
            if (!set.isEmpty()) {
                coordinatesByWorld.put(worldKey, set);
            }
        }
        dirty = false;
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        saveNow();
    }

    public synchronized void saveNow() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Set<Long>> entry : coordinatesByWorld.entrySet()) {
            yaml.set("worlds." + entry.getKey(), entry.getValue().stream().map(String::valueOf).toList());
        }
        try {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            yaml.save(dataFile);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save player placed block index: " + ex.getMessage());
        }
    }

    public boolean isPlayerPlaced(Block block) {
        if (block == null || block.getWorld() == null) {
            return false;
        }
        return coordinatesByWorld
                .getOrDefault(block.getWorld().getName(), Set.of())
                .contains(pack(block.getX(), block.getY(), block.getZ()));
    }

    public boolean hasPlayerPlacedBlockInChunk(String worldName, int chunkX, int chunkZ) {
        Set<Long> values = coordinatesByWorld.get(worldName);
        if (values == null || values.isEmpty()) {
            return false;
        }
        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        for (long packed : values) {
            int x = unpackX(packed);
            int z = unpackZ(packed);
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        World world = block.getWorld();
        coordinatesByWorld
                .computeIfAbsent(world.getName(), ignored -> ConcurrentHashMap.newKeySet())
                .add(pack(block.getX(), block.getY(), block.getZ()));
        dirty = true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Set<Long> values = coordinatesByWorld.get(block.getWorld().getName());
        if (values == null) {
            return;
        }
        if (values.remove(pack(block.getX(), block.getY(), block.getZ()))) {
            dirty = true;
        }
    }

    private long pack(int x, int y, int z) {
        long lx = ((long) x & 0x3FFFFFFL) << 38;
        long lz = ((long) z & 0x3FFFFFFL) << 12;
        long ly = ((long) y & 0xFFFL);
        return lx | lz | ly;
    }

    private int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    private int unpackZ(long packed) {
        return (int) ((packed << 26) >> 38);
    }
}
