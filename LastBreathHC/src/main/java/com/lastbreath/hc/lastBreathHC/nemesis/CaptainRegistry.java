package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptainRegistry {

    private final Map<UUID, CaptainRecord> captainsByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> captainsByNemesisPlayer = new ConcurrentHashMap<>();
    private final Map<OriginChunkKey, Set<UUID>> captainsByOriginChunk = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> activeCaptainsByWorld = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyCaptainIds = ConcurrentHashMap.newKeySet();

    public synchronized void load(Collection<CaptainRecord> records) {
        captainsByUuid.clear();
        captainsByNemesisPlayer.clear();
        captainsByOriginChunk.clear();
        activeCaptainsByWorld.clear();
        dirtyCaptainIds.clear();

        if (records == null) {
            return;
        }

        for (CaptainRecord record : records) {
            upsert(record, false);
        }
    }

    public synchronized void upsert(CaptainRecord record) {
        upsert(record, true);
    }

    private void upsert(CaptainRecord record, boolean markDirty) {
        if (record == null || record.identity() == null || record.identity().captainId() == null) {
            return;
        }

        CaptainRecord existing = captainsByUuid.put(record.identity().captainId(), record);
        if (existing != null) {
            deindex(existing);
        }
        index(record);
        if (markDirty) {
            dirtyCaptainIds.add(record.identity().captainId());
        }
    }

    public synchronized CaptainRecord remove(UUID captainUuid) {
        if (captainUuid == null) {
            return null;
        }
        CaptainRecord removed = captainsByUuid.remove(captainUuid);
        if (removed != null) {
            deindex(removed);
            dirtyCaptainIds.add(captainUuid);
        }
        return removed;
    }

    public synchronized Set<UUID> snapshotAndClearDirtyCaptainIds() {
        if (dirtyCaptainIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> dirtySnapshot = Set.copyOf(dirtyCaptainIds);
        dirtyCaptainIds.clear();
        return dirtySnapshot;
    }

    public CaptainRecord getByCaptainUuid(UUID captainUuid) {
        return captainUuid == null ? null : captainsByUuid.get(captainUuid);
    }

    public List<CaptainRecord> getByNemesisOf(UUID playerUuid) {
        if (playerUuid == null) {
            return List.of();
        }
        return resolveRecords(captainsByNemesisPlayer.getOrDefault(playerUuid, Set.of()));
    }

    public List<CaptainRecord> getByOriginChunk(String world, int chunkX, int chunkZ) {
        if (world == null || world.isBlank()) {
            return List.of();
        }
        OriginChunkKey key = new OriginChunkKey(world, chunkX, chunkZ);
        return resolveRecords(captainsByOriginChunk.getOrDefault(key, Set.of()));
    }

    public List<CaptainRecord> getActiveByWorld(String world) {
        if (world == null || world.isBlank()) {
            return List.of();
        }
        return resolveRecords(activeCaptainsByWorld.getOrDefault(normalizeWorld(world), Set.of()));
    }

    public Map<String, Integer> getActiveCaptainCountsByWorld() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : activeCaptainsByWorld.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return Collections.unmodifiableMap(counts);
    }

    public List<CaptainRecord> getActiveByChunk(String world, int chunkX, int chunkZ) {
        List<CaptainRecord> chunkRecords = getByOriginChunk(world, chunkX, chunkZ);
        if (chunkRecords.isEmpty()) {
            return chunkRecords;
        }
        List<CaptainRecord> active = new ArrayList<>(chunkRecords.size());
        for (CaptainRecord record : chunkRecords) {
            if (isActive(record)) {
                active.add(record);
            }
        }
        return active;
    }

    public List<CaptainRecord> getActiveByRadius(String world, double x, double z, double radius, EntityType typeFilter, UUID nemesisFilter) {
        if (world == null || world.isBlank() || radius < 0.0) {
            return List.of();
        }

        int minChunkX = floorToChunk(x - radius);
        int maxChunkX = floorToChunk(x + radius);
        int minChunkZ = floorToChunk(z - radius);
        int maxChunkZ = floorToChunk(z + radius);
        double radiusSquared = radius * radius;

        List<CaptainRecord> matches = new ArrayList<>();
        Set<UUID> seen = ConcurrentHashMap.newKeySet();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (CaptainRecord record : getActiveByChunk(world, chunkX, chunkZ)) {
                    if (!seen.add(record.identity().captainId()) || !matchesFilter(record, typeFilter, nemesisFilter)) {
                        continue;
                    }
                    CaptainRecord.Origin origin = record.origin();
                    if (origin == null) {
                        continue;
                    }
                    double dx = origin.spawnX() - x;
                    double dz = origin.spawnZ() - z;
                    if ((dx * dx) + (dz * dz) <= radiusSquared) {
                        matches.add(record);
                    }
                }
            }
        }
        return matches;
    }

    public Collection<CaptainRecord> getAll() {
        return Collections.unmodifiableCollection(captainsByUuid.values());
    }

    private void index(CaptainRecord record) {
        UUID captainUuid = record.identity().captainId();

        UUID nemesisOf = record.identity().nemesisOf();
        if (nemesisOf != null) {
            captainsByNemesisPlayer
                    .computeIfAbsent(nemesisOf, ignored -> ConcurrentHashMap.newKeySet())
                    .add(captainUuid);
        }

        CaptainRecord.Origin origin = record.origin();
        if (origin != null && origin.world() != null && !origin.world().isBlank()) {
            OriginChunkKey key = new OriginChunkKey(origin.world(), origin.chunkX(), origin.chunkZ());
            captainsByOriginChunk
                    .computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet())
                    .add(captainUuid);

            if (isActive(record)) {
                activeCaptainsByWorld
                        .computeIfAbsent(normalizeWorld(origin.world()), ignored -> ConcurrentHashMap.newKeySet())
                        .add(captainUuid);
            }
        }
    }

    private void deindex(CaptainRecord record) {
        UUID captainUuid = record.identity().captainId();

        UUID nemesisOf = record.identity().nemesisOf();
        if (nemesisOf != null) {
            removeFromIndex(captainsByNemesisPlayer, nemesisOf, captainUuid);
        }

        CaptainRecord.Origin origin = record.origin();
        if (origin != null && origin.world() != null && !origin.world().isBlank()) {
            OriginChunkKey key = new OriginChunkKey(origin.world(), origin.chunkX(), origin.chunkZ());
            removeFromIndex(captainsByOriginChunk, key, captainUuid);
            removeFromIndex(activeCaptainsByWorld, normalizeWorld(origin.world()), captainUuid);
        }
    }

    private boolean matchesFilter(CaptainRecord record, EntityType typeFilter, UUID nemesisFilter) {
        if (typeFilter != null) {
            String typeName = record.naming() == null ? null : record.naming().aliasSeed();
            if (typeName == null || !typeName.equalsIgnoreCase(typeFilter.name())) {
                return false;
            }
        }

        if (nemesisFilter != null) {
            UUID nemesisOf = record.identity() == null ? null : record.identity().nemesisOf();
            if (!nemesisFilter.equals(nemesisOf)) {
                return false;
            }
        }

        return isActive(record);
    }

    private boolean isActive(CaptainRecord record) {
        return record != null
                && record.state() != null
                && record.state().state() == CaptainState.ACTIVE;
    }

    private static String normalizeWorld(String world) {
        return Objects.requireNonNullElse(world, "").trim().toLowerCase();
    }

    private static int floorToChunk(double value) {
        return (int) Math.floor(value) >> 4;
    }

    private <K> void removeFromIndex(Map<K, Set<UUID>> index, K key, UUID captainUuid) {
        Set<UUID> uuids = index.get(key);
        if (uuids == null) {
            return;
        }
        uuids.remove(captainUuid);
        if (uuids.isEmpty()) {
            index.remove(key);
        }
    }

    private List<CaptainRecord> resolveRecords(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<CaptainRecord> matches = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            CaptainRecord record = captainsByUuid.get(id);
            if (record != null) {
                matches.add(record);
            }
        }
        return matches;
    }

    public record OriginChunkKey(String world, int chunkX, int chunkZ) {
        public OriginChunkKey {
            world = Objects.requireNonNullElse(world, "").trim().toLowerCase();
        }

        @Override
        public String toString() {
            return world + ":" + chunkX + ":" + chunkZ;
        }

        public static OriginChunkKey fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                return new OriginChunkKey("", 0, 0);
            }
            String[] split = raw.split(":");
            if (split.length != 3) {
                return new OriginChunkKey(split[0], 0, 0);
            }
            return new OriginChunkKey(split[0], parseInt(split[1]), parseInt(split[2]));
        }

        private static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
