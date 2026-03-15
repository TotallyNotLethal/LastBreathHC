package com.lastbreath.hc.lastBreathHC.bootstrap;

import com.lastbreath.hc.lastBreathHC.ChunkRegenManager;
import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisBuildingService;
import com.lastbreath.hc.lastBreathHC.structures.PlayerPlacedBlockIndex;
import com.lastbreath.hc.lastBreathHC.structures.StructureFootprintRepository;
import com.lastbreath.hc.lastBreathHC.structures.StructureManager;
import com.lastbreath.hc.lastBreathHC.structures.StructureManagerImpl;
import com.lastbreath.hc.lastBreathHC.structures.StructurePlacementValidator;

import java.io.File;

public final class StructureModule implements PluginModule {
    private final LastBreathHC plugin;
    private StructureFootprintRepository structureFootprintRepository;
    private PlayerPlacedBlockIndex playerPlacedBlockIndex;
    private ChunkRegenManager chunkRegenManager;
    private StructureManager structureManager;
    private NemesisBuildingService nemesisBuildingService;

    public StructureModule(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        structureFootprintRepository = new StructureFootprintRepository(plugin, new File(plugin.getDataFolder(), "nemesis-structures.yml"));
        structureFootprintRepository.load();
        playerPlacedBlockIndex = new PlayerPlacedBlockIndex(plugin, new File(plugin.getDataFolder(), "player-placed-blocks.yml"));
        playerPlacedBlockIndex.load();
        chunkRegenManager = new ChunkRegenManager(plugin, playerPlacedBlockIndex);

        StructurePlacementValidator structurePlacementValidator = new StructurePlacementValidator(
                structureFootprintRepository,
                new StructurePlacementValidator.NoOpProtectedRegionAdapter(),
                playerPlacedBlockIndex
        );
        structureManager = new StructureManagerImpl(structurePlacementValidator, structureFootprintRepository);
        nemesisBuildingService = new NemesisBuildingService(plugin, structureManager);
        nemesisBuildingService.initialize();
    }

    @Override
    public void shutdown() {
        if (nemesisBuildingService != null) {
            nemesisBuildingService.shutdown();
        }
        if (structureFootprintRepository != null) {
            structureFootprintRepository.saveIfDirty();
        }
        if (playerPlacedBlockIndex != null) {
            playerPlacedBlockIndex.saveIfDirty();
        }
        if (chunkRegenManager != null) {
            chunkRegenManager.shutdown();
        }
    }

    public StructureFootprintRepository structureFootprintRepository() {
        return structureFootprintRepository;
    }

    public PlayerPlacedBlockIndex playerPlacedBlockIndex() {
        return playerPlacedBlockIndex;
    }

    public ChunkRegenManager chunkRegenManager() {
        return chunkRegenManager;
    }

    public StructureManager structureManager() {
        return structureManager;
    }

    public NemesisBuildingService nemesisBuildingService() {
        return nemesisBuildingService;
    }
}
