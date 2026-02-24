package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StructureManagerImpl implements StructureManager {
    private final Map<String, StructureTemplateMetadata> templates = new HashMap<>();
    private final StructurePlacementValidator placementValidator;
    private final StructureFootprintRepository footprintRepository;

    public StructureManagerImpl(StructurePlacementValidator placementValidator, StructureFootprintRepository footprintRepository) {
        this.placementValidator = placementValidator;
        this.footprintRepository = footprintRepository;
    }

    @Override
    public void registerStructureTemplate(String id, StructureTemplateMetadata metadata) {
        templates.put(id, metadata);
    }

    @Override
    public Optional<StructureFootprint> spawnStructure(String templateId, Location anchor, SpawnContext context) {
        StructureTemplateMetadata metadata = templates.get(templateId);
        if (metadata == null || anchor.getWorld() == null) {
            return Optional.empty();
        }

        BoundingBox absoluteBoundingBox = metadata.template().getRelativeBoundingBox().shift(anchor.toVector());
        StructurePlacementValidator.ValidationResult validationResult =
                placementValidator.validate(anchor, metadata, context, absoluteBoundingBox);
        if (!validationResult.allowed()) {
            return Optional.empty();
        }

        World world = anchor.getWorld();
        for (StructureBlockPlacement placement : metadata.template().getBlockPlacements()) {
            Location blockLocation = anchor.clone().add(placement.relativeOffset());
            Block block = world.getBlockAt(blockLocation);
            block.setBlockData(placement.blockData(), false);
        }

        String structureId = templateId + "-" + UUID.randomUUID();
        String anchorChunk = anchor.getChunk().getX() + "," + anchor.getChunk().getZ();
        StructureFootprint footprint = new StructureFootprint(
                structureId,
                context.ownerCaptainId(),
                context.region(),
                absoluteBoundingBox,
                metadata.template().getBlockPlacements().size(),
                metadata.structureType(),
                metadata.tier(),
                anchorChunk,
                context.lastUpgradeTimestamp(),
                world.getName()
        );

        footprintRepository.upsert(footprint);
        footprintRepository.saveIfDirty();
        return Optional.of(footprint);
    }

    @Override
    public Optional<StructureFootprint> upgradeLatestStructureForOwner(String ownerCaptainId, int newTier, long lastUpgradeTimestamp) {
        Optional<StructureFootprint> latest = footprintRepository.findLatestByOwner(ownerCaptainId);
        if (latest.isEmpty()) {
            return Optional.empty();
        }
        StructureFootprint base = latest.get();
        StructureFootprint upgraded = new StructureFootprint(
                base.structureId(),
                base.ownerCaptainId(),
                base.region(),
                base.boundingBox(),
                base.blockCount(),
                base.structureType(),
                Math.max(base.tier(), newTier),
                base.anchorChunk(),
                Math.max(base.lastUpgradeTimestamp(), lastUpgradeTimestamp),
                base.worldName()
        );
        footprintRepository.upsert(upgraded);
        footprintRepository.saveIfDirty();
        return Optional.of(upgraded);
    }

}