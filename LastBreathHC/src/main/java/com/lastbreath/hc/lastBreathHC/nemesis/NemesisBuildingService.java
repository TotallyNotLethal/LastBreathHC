package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.structures.LitematicStructureTemplateLoader;
import com.lastbreath.hc.lastBreathHC.structures.SpawnContext;
import com.lastbreath.hc.lastBreathHC.structures.StructureManager;
import com.lastbreath.hc.lastBreathHC.structures.StructureTemplate;
import com.lastbreath.hc.lastBreathHC.structures.StructureTemplateMetadata;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NemesisBuildingService {
    private final LastBreathHC plugin;
    private final StructureManager structureManager;
    private final LitematicStructureTemplateLoader litematicLoader;
    private final Map<Rank, RankBuildingDefinition> definitionsByRank = new EnumMap<>(Rank.class);
    private final Map<UUID, BukkitTask> pendingByCaptain = new ConcurrentHashMap<>();

    public NemesisBuildingService(LastBreathHC plugin, StructureManager structureManager) {
        this.plugin = plugin;
        this.structureManager = structureManager;
        this.litematicLoader = new LitematicStructureTemplateLoader();
    }

    public void initialize() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Failed to create plugin data folder for nemesis building config.");
            return;
        }
        File configFile = new File(plugin.getDataFolder(), "nemesis-buildings.yml");
        if (!configFile.exists()) {
            plugin.saveResource("nemesis-buildings.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String schematicsDirectoryName = config.getString("nemesisBuildings.schematicsDirectory", "schematics");
        File schematicsDirectory = new File(plugin.getDataFolder(), schematicsDirectoryName);
        if (!schematicsDirectory.exists() && !schematicsDirectory.mkdirs()) {
            plugin.getLogger().warning("Unable to create schematics directory at " + schematicsDirectory.getAbsolutePath());
        }

        String defaultStructureType = config.getString("nemesisBuildings.defaults.structureType", "nemesis_fortification");
        int defaultSpawnRadiusExclusion = Math.max(0, config.getInt("nemesisBuildings.defaults.spawnRadiusExclusion", 24));

        ConfigurationSection ranksSection = config.getConfigurationSection("nemesisBuildings.ranks");
        if (ranksSection == null) {
            plugin.getLogger().warning("No nemesisBuildings.ranks section found in nemesis-buildings.yml.");
            return;
        }

        definitionsByRank.clear();
        for (String key : ranksSection.getKeys(false)) {
            ConfigurationSection section = ranksSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            Rank rank = Rank.from(section.getString("rank"), Rank.from(key, null));
            if (rank == null) {
                plugin.getLogger().warning("Skipping nemesis building entry '" + key + "' due to invalid rank.");
                continue;
            }

            String templateId = section.getString("templateId", "nemesis_" + key.toLowerCase(Locale.ROOT));
            String schematicName = section.getString("schematic", templateId + ".litematic");
            long buildSeconds = Math.max(5L, section.getLong("buildSeconds", 300L));
            int tier = Math.max(1, section.getInt("tier", rank.ordinal() + 1));
            String structureType = section.getString("structureType", defaultStructureType);
            int spawnRadiusExclusion = Math.max(0, section.getInt("spawnRadiusExclusion", defaultSpawnRadiusExclusion));
            String displayName = section.getString("displayName", rank.name());

            File litematic = new File(schematicsDirectory, schematicName);
            try {
                StructureTemplate template = litematicLoader.load(templateId, litematic);
                structureManager.registerStructureTemplate(templateId,
                        new StructureTemplateMetadata(template, structureType, tier, spawnRadiusExclusion));
                definitionsByRank.put(rank, new RankBuildingDefinition(templateId, displayName, schematicName, buildSeconds));
                plugin.getLogger().info("Registered nemesis building template " + templateId + " for " + rank.name());
            } catch (IOException ex) {
                plugin.getLogger().warning("Unable to register nemesis building template for " + rank.name()
                        + " because schematic is missing or invalid: " + litematic.getAbsolutePath());
            }
        }
    }

    public void scheduleRankConstruction(CaptainRecord record, Rank rank, String region, int cohortIndex) {
        if (record == null || record.identity() == null || record.origin() == null) {
            return;
        }

        RankBuildingDefinition definition = definitionsByRank.get(rank);
        if (definition == null) {
            plugin.getLogger().warning("No building definition registered for rank " + rank + ".");
            return;
        }

        World world = Bukkit.getWorld(record.origin().world());
        if (world == null) {
            return;
        }

        double direction = cohortIndex % 2 == 0 ? 1.0 : -1.0;
        Location anchor = new Location(
                world,
                record.origin().spawnX() + (cohortIndex * 4.0),
                record.origin().spawnY(),
                record.origin().spawnZ() + (direction * 3.0)
        );

        UUID captainId = record.identity().captainId();
        BukkitTask existing = pendingByCaptain.remove(captainId);
        if (existing != null) {
            existing.cancel();
        }

        long delayTicks = definition.buildSeconds() * 20L;
        plugin.getLogger().info("Queued " + definition.displayName() + " for " + captainId + " in "
                + definition.buildSeconds() + "s using " + definition.schematicName());

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingByCaptain.remove(captainId);
            structureManager.spawnStructure(
                    definition.templateId(),
                    anchor,
                    new SpawnContext(captainId.toString(), region, System.currentTimeMillis())
            );
        }, delayTicks);

        pendingByCaptain.put(captainId, task);
    }

    public void shutdown() {
        for (BukkitTask task : pendingByCaptain.values()) {
            task.cancel();
        }
        pendingByCaptain.clear();
    }

    private record RankBuildingDefinition(
            String templateId,
            String displayName,
            String schematicName,
            long buildSeconds
    ) {
    }
}
