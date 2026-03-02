package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.util.BoundingBox;

public final class StructurePlacementValidator {
    private final StructureFootprintRepository footprintRepository;
    private final ProtectedRegionAdapter protectedRegionAdapter;
    private final PlayerPlacedBlockIndex playerPlacedBlockIndex;

    public StructurePlacementValidator(StructureFootprintRepository footprintRepository,
                                       ProtectedRegionAdapter protectedRegionAdapter,
                                       PlayerPlacedBlockIndex playerPlacedBlockIndex) {
        this.footprintRepository = footprintRepository;
        this.protectedRegionAdapter = protectedRegionAdapter;
        this.playerPlacedBlockIndex = playerPlacedBlockIndex;
    }

    public ValidationResult validate(Location anchor, StructureTemplateMetadata metadata, SpawnContext context, BoundingBox absoluteBoundingBox) {
        World world = anchor.getWorld();
        if (world == null) {
            return ValidationResult.rejected("Anchor world is null");
        }

        int spawnRadius = Math.max(0, metadata.spawnRadiusExclusion());
        if (spawnRadius > 0 && world.getSpawnLocation().distance(anchor) < spawnRadius) {
            return ValidationResult.rejected("Inside spawn exclusion radius");
        }

        if (protectedRegionAdapter.isProtected(anchor, context)) {
            return ValidationResult.rejected("Inside protected region");
        }

        boolean overlaps = footprintRepository.findOverlapping(world.getName(), absoluteBoundingBox)
                .findAny()
                .isPresent();
        if (overlaps) {
            return ValidationResult.rejected("Overlaps existing structure footprint");
        }

        for (StructureBlockPlacement placement : metadata.template().getBlockPlacements()) {
            Block block = world.getBlockAt(anchor.clone().add(placement.relativeOffset()));
            if (playerPlacedBlockIndex != null && playerPlacedBlockIndex.isPlayerPlaced(block)) {
                return ValidationResult.rejected("Overlaps player-placed block");
            }
            if (isLikelyPlayerBuilt(block)) {
                return ValidationResult.rejected("Overlaps likely player-built structure");
            }
        }

        return ValidationResult.accepted();
    }

    private boolean isLikelyPlayerBuilt(Block block) {
        if (block == null) {
            return false;
        }
        Material material = block.getType();
        if (material.isAir() || material == Material.WATER || material == Material.LAVA) {
            return false;
        }
        if (material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.FERN
                || material == Material.LARGE_FERN
                || material == Material.VINE
                || material == Material.SNOW
                || material == Material.SNOW_BLOCK) {
            return false;
        }
        if (block.getState() instanceof TileState) {
            return true;
        }
        if (Tag.PLANKS.isTagged(material)
                || Tag.WOODEN_STAIRS.isTagged(material)
                || Tag.WOODEN_SLABS.isTagged(material)
                || Tag.WOODEN_FENCES.isTagged(material)
                || Tag.DOORS.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material)
                || Tag.WOOL.isTagged(material)
                || Tag.BEDS.isTagged(material)
                || Tag.CANDLES.isTagged(material)
                || Tag.BANNERS.isTagged(material)
                || Tag.SIGNS.isTagged(material)
                || Tag.CARPETS.isTagged(material)
                //|| Tag.GLASS.isTagged(material)
                || Tag.STAIRS.isTagged(material)
                || Tag.SLABS.isTagged(material)
                || Tag.WALLS.isTagged(material)) {
            return true;
        }
        return switch (material) {
            case COBBLESTONE, MOSSY_COBBLESTONE, STONE_BRICKS, MOSSY_STONE_BRICKS,
                 CRACKED_STONE_BRICKS, CHISELED_STONE_BRICKS, BRICKS,
                 NETHER_BRICKS, RED_NETHER_BRICKS, POLISHED_BLACKSTONE_BRICKS,
                 OBSIDIAN, CRYING_OBSIDIAN -> true;
            default -> false;
        };
    }

    public interface ProtectedRegionAdapter {
        boolean isProtected(Location location, SpawnContext context);
    }

    public static final class NoOpProtectedRegionAdapter implements ProtectedRegionAdapter {
        @Override
        public boolean isProtected(Location location, SpawnContext context) {
            return false;
        }
    }

    public record ValidationResult(boolean allowed, String reason) {
        public static ValidationResult accepted() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult rejected(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
