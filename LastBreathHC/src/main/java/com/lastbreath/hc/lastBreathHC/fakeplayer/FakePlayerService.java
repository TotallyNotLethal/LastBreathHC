package com.lastbreath.hc.lastBreathHC.fakeplayer;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.platform.FakePlayerPlatformAdapter;
import com.lastbreath.hc.lastBreathHC.fakeplayer.platform.FakePlayerPlatformAdapterFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class FakePlayerService {
    private static final String UUID_NAMESPACE = "com.lastbreath.hc.lastBreathHC.fakeplayer:";

    private final LastBreathHC plugin;
    private final FakePlayerRepository repository;
    private final FakePlayerPlatformAdapter platformAdapter;
    private final SkinService skinService;
    private final FakePlayersSettings settings;
    private final Map<UUID, FakePlayerRecord> records = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Runnable> visualUpdateQueue = new ConcurrentLinkedQueue<>();

    private BukkitTask visualDrainTask;

    public FakePlayerService(LastBreathHC plugin, FakePlayerRepository repository, SkinService skinService, FakePlayersSettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.skinService = skinService;
        this.settings = settings;
        this.platformAdapter = FakePlayerPlatformAdapterFactory.create(plugin);
    }

    public void startup() {
        records.clear();
        Map<String, SkinCacheEntry> skinCache = new ConcurrentHashMap<>();
        repository.loadInto(records, skinCache);
        skinService.loadCache(skinCache);
        startVisualDrainTask();
        int respawned = startupAutoRespawn();
        plugin.getLogger().info("Loaded " + records.size() + " fake player(s), auto-respawned " + respawned + ".");
    }

    public void shutdown() {
        if (visualDrainTask != null) {
            visualDrainTask.cancel();
            visualDrainTask = null;
        }
        flushVisualQueue();
        for (FakePlayerRecord record : records.values()) {
            platformAdapter.despawnFakeTabEntry(record.getUuid());
        }
        repository.save(records.values(), skinService.snapshotCache());
    }

    public FakePlayerRecord addFakePlayer(String name, String skinOwner, String textures, String signature) {
        String normalizedName = name == null ? "unknown" : name.trim();
        if (normalizedName.isEmpty()) {
            normalizedName = "unknown";
        }

        String normalizedOwner = normalizeOwner(skinOwner, normalizedName);
        SkinService.SkinLookupResult skinLookup = skinService.lookup(normalizedOwner, false);
        String resolvedTextures = skinLookup.hasSkin() ? skinLookup.textures() : textures;
        String resolvedSignature = skinLookup.hasSkin() ? skinLookup.signature() : signature;

        UUID uuid = deterministicUuid(normalizedName);
        FakePlayerRecord record = records.get(uuid);
        if (record == null) {
            record = new FakePlayerRecord(normalizedName, uuid, normalizedOwner, resolvedTextures, resolvedSignature);
            records.put(uuid, record);
        } else {
            record.setName(normalizedName);
            record.setSkinOwner(normalizedOwner);
            record.setTextures(resolvedTextures);
            record.setSignature(resolvedSignature);
            record.setActive(true);
            record.setLastSeenAt(Instant.now());
        }
        queueVisualUpdate(() -> platformAdapter.updateDisplayState(record));
        return record;
    }

    public SkinUpdateOutcome refreshSkin(UUID uuid, String skinOwner, boolean forceRefresh) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null) {
            return SkinUpdateOutcome.notFound();
        }

        String normalizedOwner = normalizeOwner(skinOwner, record.getName());
        SkinService.SkinLookupResult lookup = skinService.lookup(normalizedOwner, forceRefresh);

        if (!lookup.hasSkin()) {
            return SkinUpdateOutcome.failedNoSkin();
        }

        record.setSkinOwner(normalizedOwner);
        record.setTextures(lookup.textures());
        record.setSignature(lookup.signature());
        queueVisualUpdate(() -> platformAdapter.updateDisplayState(record));
        return new SkinUpdateOutcome(true, lookup.refreshed(), lookup.usedCachedAfterFailure(), false);
    }

    public boolean removeFakePlayer(UUID uuid) {
        FakePlayerRecord removed = records.remove(uuid);
        if (removed == null) {
            return false;
        }
        queueVisualUpdate(() -> platformAdapter.despawnFakeTabEntry(uuid));
        return true;
    }

    public List<FakePlayerRecord> listFakePlayers() {
        List<FakePlayerRecord> list = new ArrayList<>(records.values());
        list.sort(Comparator.comparing(FakePlayerRecord::getName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(list);
    }

    public int getActiveCount() {
        int active = 0;
        for (FakePlayerRecord record : records.values()) {
            if (record.isActive()) {
                active++;
            }
        }
        return active;
    }

    public Optional<FakePlayerRecord> getByUuid(UUID uuid) {
        return Optional.ofNullable(records.get(uuid));
    }

    public boolean mute(UUID uuid, boolean muted) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null) {
            return false;
        }
        record.setMuted(muted);
        return true;
    }

    public boolean setActive(UUID uuid, boolean active) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null) {
            return false;
        }
        record.setActive(active);
        if (active) {
            record.setLastSeenAt(Instant.now());
        }
        queueVisualUpdate(() -> platformAdapter.updateDisplayState(record));
        return true;
    }

    public boolean registerChat(UUID uuid, String chatMessage) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null || !record.isActive() || record.isMuted()) {
            return false;
        }
        if (chatMessage == null || chatMessage.isBlank()) {
            return false;
        }
        record.setLastChatAt(Instant.now());
        record.setChatCount(record.getChatCount() + 1L);
        return true;
    }


    public boolean registerReaction(UUID uuid) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null || !record.isActive() || record.isMuted()) {
            return false;
        }

        record.setLastReactionAt(Instant.now());
        record.setReactionCount(record.getReactionCount() + 1L);
        return true;
    }

    public Optional<FakePlayerRecord> pickReactionCandidate(Duration cooldown) {
        Duration effectiveCooldown = cooldown == null ? Duration.ZERO : cooldown;
        Instant now = Instant.now();
        FakePlayerRecord picked = records.values().stream()
                .filter(FakePlayerRecord::isActive)
                .filter(record -> !record.isMuted())
                .filter(record -> isCooldownReady(record.getLastReactionAt(), now, effectiveCooldown))
                .min(Comparator.comparing(record -> {
                    Instant lastReactionAt = record.getLastReactionAt();
                    return lastReactionAt == null ? Instant.EPOCH : lastReactionAt;
                }))
                .orElse(null);

        if (picked == null) {
            return Optional.empty();
        }

        picked.setLastReactionAt(now);
        picked.setReactionCount(picked.getReactionCount() + 1L);
        return Optional.of(picked);
    }

    public int startupAutoRespawn() {
        if (!settings.autoRespawnActiveOnStartup()) {
            plugin.getLogger().info("fakePlayers.autoRespawnActiveOnStartup=false, skipping fake-player auto-respawn.");
            return 0;
        }

        Instant now = Instant.now();
        List<FakePlayerRecord> toRespawn = new ArrayList<>();
        for (FakePlayerRecord record : records.values()) {
            if (!record.isActive()) {
                continue;
            }
            record.setLastSeenAt(now);
            toRespawn.add(record);
        }

        int total = toRespawn.size();
        if (total == 0) {
            return 0;
        }

        int batchSize = settings.batching().startupRespawnBatchSize();
        int intervalTicks = settings.batching().startupRespawnIntervalTicks();
        for (int start = 0; start < total; start += batchSize) {
            int from = start;
            int to = Math.min(start + batchSize, total);
            long delay = (long) (start / batchSize) * intervalTicks;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int idx = from; idx < to; idx++) {
                    platformAdapter.spawnFakeTabEntry(toRespawn.get(idx));
                }
            }, delay);
        }

        return total;
    }

    /**
     * Rebuilds the tab-list visual for an existing fake player record.
     *
     * <p>This is still a visual simulation only, not a real client connection.</p>
     */
    public boolean refreshVisual(UUID uuid) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null) {
            return false;
        }
        queueVisualUpdate(() -> platformAdapter.updateDisplayState(record));
        return true;
    }

    public void saveNow() {
        repository.save(records.values(), skinService.snapshotCache());
    }

    private boolean isCooldownReady(Instant lastReactionAt, Instant now, Duration cooldown) {
        if (lastReactionAt == null) {
            return true;
        }
        return !lastReactionAt.plus(cooldown).isAfter(now);
    }

    private UUID deterministicUuid(String name) {
        String seed = UUID_NAMESPACE + name.toLowerCase();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeOwner(String skinOwner, String fallbackName) {
        String owner = skinOwner;
        if (owner == null || owner.isBlank()) {
            owner = fallbackName;
        }
        return owner == null ? "unknown" : owner.trim().toLowerCase();
    }



    private void startVisualDrainTask() {
        if (visualDrainTask != null) {
            visualDrainTask.cancel();
        }
        int intervalTicks = settings.batching().visualUpdateIntervalTicks();
        visualDrainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::drainVisualQueue, intervalTicks, intervalTicks);
    }

    private void queueVisualUpdate(Runnable update) {
        if (update == null) {
            return;
        }
        visualUpdateQueue.offer(update);
    }

    private void drainVisualQueue() {
        int max = settings.batching().visualUpdateBatchSize();
        for (int i = 0; i < max; i++) {
            Runnable update = visualUpdateQueue.poll();
            if (update == null) {
                return;
            }
            update.run();
        }
    }

    private void flushVisualQueue() {
        Runnable update;
        while ((update = visualUpdateQueue.poll()) != null) {
            update.run();
        }
    }
    public record SkinUpdateOutcome(boolean success, boolean refreshed, boolean usedFallbackCache, boolean notFound) {
        private static SkinUpdateOutcome notFound() {
            return new SkinUpdateOutcome(false, false, false, true);
        }

        private static SkinUpdateOutcome failedNoSkin() {
            return new SkinUpdateOutcome(false, false, false, false);
        }
    }
}
