package com.lastbreath.hc.lastBreathHC.fakeplayer;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;

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

public class FakePlayerService {
    private static final String UUID_NAMESPACE = "com.lastbreath.hc.lastBreathHC.fakeplayer:";

    private final LastBreathHC plugin;
    private final FakePlayerRepository repository;
    private final Map<UUID, FakePlayerRecord> records = new ConcurrentHashMap<>();

    public FakePlayerService(LastBreathHC plugin, FakePlayerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void startup() {
        records.clear();
        records.putAll(repository.load());
        int respawned = startupAutoRespawn();
        plugin.getLogger().info("Loaded " + records.size() + " fake player(s), auto-respawned " + respawned + ".");
    }

    public void shutdown() {
        repository.save(records.values());
    }

    public FakePlayerRecord addFakePlayer(String name, String skinOwner, String textures, String signature) {
        String normalizedName = name == null ? "unknown" : name.trim();
        if (normalizedName.isEmpty()) {
            normalizedName = "unknown";
        }

        UUID uuid = deterministicUuid(normalizedName);
        FakePlayerRecord record = records.get(uuid);
        if (record == null) {
            record = new FakePlayerRecord(normalizedName, uuid, skinOwner, textures, signature);
            records.put(uuid, record);
        } else {
            record.setName(normalizedName);
            record.setSkinOwner(skinOwner);
            record.setTextures(textures);
            record.setSignature(signature);
            record.setActive(true);
            record.setLastSeenAt(Instant.now());
        }
        return record;
    }

    public boolean removeFakePlayer(UUID uuid) {
        return records.remove(uuid) != null;
    }

    public List<FakePlayerRecord> listFakePlayers() {
        List<FakePlayerRecord> list = new ArrayList<>(records.values());
        list.sort(Comparator.comparing(FakePlayerRecord::getName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(list);
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
        Instant now = Instant.now();
        int count = 0;
        for (FakePlayerRecord record : records.values()) {
            if (!record.isActive()) {
                continue;
            }
            record.setLastSeenAt(now);
            count++;
        }
        return count;
    }

    public void saveNow() {
        repository.save(records.values());
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
}
