package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.structures.StructureFootprint;
import com.lastbreath.hc.lastBreathHC.structures.StructureFootprintRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CaptainHabitatService {
    private final LastBreathHC plugin;
    private final CaptainRegistry captainRegistry;
    private final StructureFootprintRepository footprintRepository;

    public CaptainHabitatService(LastBreathHC plugin, CaptainRegistry captainRegistry, StructureFootprintRepository footprintRepository) {
        this.plugin = plugin;
        this.captainRegistry = captainRegistry;
        this.footprintRepository = footprintRepository;
    }

    public void linkCaptainToStructure(CaptainRecord record, StructureFootprint footprint) {
        if (record == null || footprint == null) {
            return;
        }
        String[] chunk = footprint.anchorChunk().split(",");
        int cx = chunk.length > 0 ? parseInt(chunk[0]) : 0;
        int cz = chunk.length > 1 ? parseInt(chunk[1]) : 0;
        CaptainRecord.Habitat habitat = new CaptainRecord.Habitat(
                footprint.structureId(),
                footprint.region(),
                footprint.worldName(),
                cx,
                cz,
                record.origin().spawnX(),
                record.origin().spawnY(),
                record.origin().spawnZ()
        );
        CaptainRecord updated = new CaptainRecord(
                record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(),
                record.naming(), record.traits(), record.minionPack(), record.state(), record.telemetry(),
                record.political(), record.social(), record.relationships(), record.memory(), record.persona(), Optional.of(habitat)
        );
        captainRegistry.upsert(updated);
    }

    public DeathCleanupResult handlePermanentCaptainDeath(UUID captainId) {
        CaptainRecord record = captainRegistry.getByCaptainUuid(captainId);
        if (record == null) {
            return new DeathCleanupResult(0, 0, 0, "none");
        }
        String behavior = plugin.getConfig().getString("nemesis.structures.onCaptainDeath", "abandon").toLowerCase();
        List<StructureFootprint> owned = footprintRepository.findByOwner(captainId.toString()).toList();
        int decayed = 0;
        int abandoned = 0;
        if ("decay".equals(behavior)) {
            decayed = footprintRepository.deleteByOwner(captainId.toString());
        } else if ("abandon".equals(behavior)) {
            abandoned = footprintRepository.abandonByOwner(captainId.toString());
        }

        CaptainRecord updated = new CaptainRecord(
                record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(),
                record.naming(), record.traits(), record.minionPack(), record.state(), record.telemetry(),
                record.political(), record.social(), record.relationships(), record.memory(), record.persona(), Optional.empty()
        );
        captainRegistry.upsert(updated);
        footprintRepository.saveIfDirty();
        return new DeathCleanupResult(owned.size(), decayed, abandoned, behavior);
    }

    public int cleanupMissingCaptainLinks() {
        List<StructureFootprint> abandoned = new ArrayList<>();
        for (StructureFootprint footprint : footprintRepository.all()) {
            UUID owner = parseUuid(footprint.ownerCaptainId());
            if (owner != null && captainRegistry.getByCaptainUuid(owner) == null) {
                abandoned.add(footprint);
            }
        }
        for (StructureFootprint footprint : abandoned) {
            footprintRepository.abandonStructure(footprint.structureId());
        }
        footprintRepository.saveIfDirty();
        return abandoned.size();
    }

    private UUID parseUuid(String raw) {
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    public record DeathCleanupResult(int ownedCount, int decayedCount, int abandonedCount, String behavior) {
    }
}
