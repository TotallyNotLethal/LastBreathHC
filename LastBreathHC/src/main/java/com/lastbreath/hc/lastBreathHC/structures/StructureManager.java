package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Location;

import java.util.Optional;

public interface StructureManager {
    void registerStructureTemplate(String id, StructureTemplateMetadata metadata);

    Optional<StructureFootprint> spawnStructure(String templateId, Location anchor, SpawnContext context);

    Optional<StructureFootprint> upgradeLatestStructureForOwner(String ownerCaptainId, int newTier, long lastUpgradeTimestamp);
}
