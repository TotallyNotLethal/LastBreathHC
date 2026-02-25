package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class StructureFootprintRepository {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, StructureFootprint> footprints = new LinkedHashMap<>();
    private boolean dirty;

    public StructureFootprintRepository(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public void load() {
        footprints.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("footprints");
        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            BoundingBox box = new BoundingBox(
                    section.getDouble("boundingBox.minX"),
                    section.getDouble("boundingBox.minY"),
                    section.getDouble("boundingBox.minZ"),
                    section.getDouble("boundingBox.maxX"),
                    section.getDouble("boundingBox.maxY"),
                    section.getDouble("boundingBox.maxZ")
            );

            StructureFootprint footprint = new StructureFootprint(
                    section.getString("structureId", key),
                    section.getString("ownerCaptainId", "unknown"),
                    section.getString("region", "unknown"),
                    box,
                    section.getInt("blockCount"),
                    section.getString("structureType", "unknown"),
                    section.getInt("tier"),
                    section.getString("anchorChunk", "0,0"),
                    section.getLong("lastUpgradeTimestamp"),
                    section.getString("world", "world")
            );
            footprints.put(footprint.structureId(), footprint);
        }
        dirty = false;
    }

    public void upsert(StructureFootprint footprint) {
        footprints.put(footprint.structureId(), footprint);
        dirty = true;
    }

    public Optional<StructureFootprint> findById(String id) {
        return Optional.ofNullable(footprints.get(id));
    }


    public Optional<StructureFootprint> findLatestByOwner(String ownerCaptainId) {
        return footprints.values().stream()
                .filter(footprint -> footprint.ownerCaptainId().equals(ownerCaptainId))
                .max(java.util.Comparator.comparingLong(StructureFootprint::lastUpgradeTimestamp));
    }


    public Stream<StructureFootprint> findByOwner(String ownerCaptainId) {
        return footprints.values().stream()
                .filter(footprint -> footprint.ownerCaptainId().equals(ownerCaptainId));
    }

    public int deleteByOwner(String ownerCaptainId) {
        java.util.List<String> ids = footprints.values().stream()
                .filter(footprint -> footprint.ownerCaptainId().equals(ownerCaptainId))
                .map(StructureFootprint::structureId)
                .toList();
        ids.forEach(footprints::remove);
        if (!ids.isEmpty()) {
            dirty = true;
        }
        return ids.size();
    }

    public int abandonByOwner(String ownerCaptainId) {
        int changed = 0;
        for (StructureFootprint footprint : java.util.List.copyOf(footprints.values())) {
            if (!footprint.ownerCaptainId().equals(ownerCaptainId)) {
                continue;
            }
            StructureFootprint abandoned = new StructureFootprint(
                    footprint.structureId(),
                    "abandoned",
                    footprint.region(),
                    footprint.boundingBox(),
                    footprint.blockCount(),
                    footprint.structureType(),
                    footprint.tier(),
                    footprint.anchorChunk(),
                    footprint.lastUpgradeTimestamp(),
                    footprint.worldName()
            );
            footprints.put(abandoned.structureId(), abandoned);
            changed++;
        }
        if (changed > 0) {
            dirty = true;
        }
        return changed;
    }

    public void abandonStructure(String structureId) {
        StructureFootprint footprint = footprints.get(structureId);
        if (footprint == null) {
            return;
        }
        footprints.put(structureId, new StructureFootprint(
                footprint.structureId(),
                "abandoned",
                footprint.region(),
                footprint.boundingBox(),
                footprint.blockCount(),
                footprint.structureType(),
                footprint.tier(),
                footprint.anchorChunk(),
                footprint.lastUpgradeTimestamp(),
                footprint.worldName()
        ));
        dirty = true;
    }

    public Stream<StructureFootprint> findOverlapping(String worldName, BoundingBox candidate) {
        return footprints.values().stream()
                .filter(existing -> existing.worldName().equals(worldName))
                .filter(existing -> existing.boundingBox().overlaps(candidate));
    }

    public Collection<StructureFootprint> all() {
        return footprints.values();
    }

    public int clearAll() {
        int removed = footprints.size();
        if (removed == 0) {
            return 0;
        }
        footprints.clear();
        dirty = true;
        return removed;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void saveIfDirty() {
        if (dirty) {
            saveAll();
        }
    }

    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("footprints");

        for (StructureFootprint footprint : footprints.values()) {
            ConfigurationSection section = root.createSection(footprint.structureId());
            section.set("structureId", footprint.structureId());
            section.set("ownerCaptainId", footprint.ownerCaptainId());
            section.set("region", footprint.region());
            section.set("world", footprint.worldName());
            section.set("boundingBox.minX", footprint.boundingBox().getMinX());
            section.set("boundingBox.minY", footprint.boundingBox().getMinY());
            section.set("boundingBox.minZ", footprint.boundingBox().getMinZ());
            section.set("boundingBox.maxX", footprint.boundingBox().getMaxX());
            section.set("boundingBox.maxY", footprint.boundingBox().getMaxY());
            section.set("boundingBox.maxZ", footprint.boundingBox().getMaxZ());
            section.set("blockCount", footprint.blockCount());
            section.set("structureType", footprint.structureType());
            section.set("tier", footprint.tier());
            section.set("anchorChunk", footprint.anchorChunk());
            section.set("lastUpgradeTimestamp", footprint.lastUpgradeTimestamp());
        }

        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save structure footprints", e);
        }
    }
}
