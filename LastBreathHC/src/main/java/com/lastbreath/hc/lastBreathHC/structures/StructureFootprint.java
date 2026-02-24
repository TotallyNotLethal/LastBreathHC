package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.util.BoundingBox;

public record StructureFootprint(
        String structureId,
        String ownerCaptainId,
        String region,
        BoundingBox boundingBox,
        int blockCount,
        String structureType,
        int tier,
        String anchorChunk,
        long lastUpgradeTimestamp,
        String worldName
) {
}
