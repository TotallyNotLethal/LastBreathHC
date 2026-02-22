package com.lastbreath.hc.lastBreathHC.nemesis;

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

    public synchronized void load(Collection<CaptainRecord> records) {
        captainsByUuid.clear();
        captainsByNemesisPlayer.clear();
        captainsByOriginChunk.clear();

        if (records == null) {
            return;
        }

        for (CaptainRecord record : records) {
            upsert(record);
        }
    }

    public synchronized void upsert(CaptainRecord record) {
        if (record == null || record.identity() == null || record.identity().captainId() == null) {
            return;
        }

        CaptainRecord existing = captainsByUuid.put(record.identity().captainId(), record);
        if (existing != null) {
            deindex(existing);
        }
        index(record);
    }

    public synchronized CaptainRecord remove(UUID captainUuid) {
        if (captainUuid == null) {
            return null;
        }
        CaptainRecord removed = captainsByUuid.remove(captainUuid);
        if (removed != null) {
            deindex(removed);
        }
        return removed;
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
        }
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
