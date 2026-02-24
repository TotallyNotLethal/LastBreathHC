package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

public final class StructurePlacementValidator {
    private final StructureFootprintRepository footprintRepository;
    private final ProtectedRegionAdapter protectedRegionAdapter;

    public StructurePlacementValidator(StructureFootprintRepository footprintRepository, ProtectedRegionAdapter protectedRegionAdapter) {
        this.footprintRepository = footprintRepository;
        this.protectedRegionAdapter = protectedRegionAdapter;
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

        return ValidationResult.allowed();
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
        public static ValidationResult allowed() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult rejected(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
