package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArmyGraphSerializer {
    private final LastBreathHC plugin;
    private final File file;

    public ArmyGraphSerializer(LastBreathHC plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public List<ArmyGraphService.EdgeRecord> load() {
        List<ArmyGraphService.EdgeRecord> edges = new ArrayList<>();
        if (!file.exists()) {
            return edges;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("edges");
        if (section == null) {
            return edges;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection row = section.getConfigurationSection(key);
            if (row == null) {
                continue;
            }
            UUID from = parseUuid(row.getString("from"));
            UUID to = parseUuid(row.getString("to"));
            String type = row.getString("type", "");
            if (from == null || to == null || type.isBlank()) {
                continue;
            }
            edges.add(new ArmyGraphService.EdgeRecord(type, from, to));
        }
        return edges;
    }

    public void saveDirty(List<ArmyGraphService.EdgeRecord> edges, boolean dirty) {
        if (!dirty) {
            return;
        }
        save(edges);
    }

    public void save(List<ArmyGraphService.EdgeRecord> edges) {
        ensureParentDirectory();

        YamlConfiguration config = new YamlConfiguration();
        int index = 0;
        for (ArmyGraphService.EdgeRecord edge : edges) {
            if (edge == null || edge.from() == null || edge.to() == null || edge.type() == null || edge.type().isBlank()) {
                continue;
            }
            String base = "edges." + index++;
            config.set(base + ".type", edge.type());
            config.set(base + ".from", edge.from().toString());
            config.set(base + ".to", edge.to().toString());
        }

        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(tempFile);
            moveAtomic(tempFile, file);
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save nemesis army graph: " + e.getMessage());
        } finally {
            if (tempFile.exists() && !tempFile.equals(file)) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void ensureParentDirectory() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
    }

    private void moveAtomic(File source, File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
