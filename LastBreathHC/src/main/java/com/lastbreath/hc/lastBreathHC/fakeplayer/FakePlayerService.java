package com.lastbreath.hc.lastBreathHC.fakeplayer;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.platform.FakePlayerPlatformAdapter;
import com.lastbreath.hc.lastBreathHC.fakeplayer.platform.FakePlayerPlatformAdapterFactory;
import com.lastbreath.hc.lastBreathHC.titles.Title;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
        for (FakePlayerRecord record : records.values()) {
            initializeTabPresentationDefaults(record);
        }
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
            initializeTabPresentationDefaults(record);
            records.put(uuid, record);
        } else {
            record.setName(normalizedName);
            record.setSkinOwner(normalizedOwner);
            record.setTextures(resolvedTextures);
            record.setSignature(resolvedSignature);
            record.setActive(true);
            record.setLastSeenAt(Instant.now());
            initializeTabPresentationDefaults(record);
        }
        FakePlayerRecord finalRecord = record;
        queueVisualUpdate(() -> platformAdapter.updateDisplayState(finalRecord));
        return record;
    }

    public SkinUpdateOutcome refreshSkin(UUID uuid, String skinOwner, boolean forceRefresh) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null) {
            return SkinUpdateOutcome.notFoundOutcome();
        }

        String normalizedOwner = normalizeOwner(skinOwner, record.getName());
        SkinService.SkinLookupResult lookup = skinService.lookup(normalizedOwner, forceRefresh);

        if (!lookup.hasSkin()) {
            return SkinUpdateOutcome.failedNoSkinOutcome();
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

    public Optional<FakePlayerRecord> findActiveByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return records.values().stream()
                .filter(FakePlayerRecord::isActive)
                .filter(record -> record.getName() != null)
                .filter(record -> record.getName().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    public List<String> listActiveNames() {
        return records.values().stream()
                .filter(FakePlayerRecord::isActive)
                .map(FakePlayerRecord::getName)
                .filter(name -> name != null && !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
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

    public boolean setTabTitle(UUID uuid, String tabTitleKey) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null || tabTitleKey == null || tabTitleKey.isBlank()) {
            return false;
        }
        Title title = Title.fromInput(tabTitleKey);
        if (title == null) {
            return false;
        }
        record.setTabTitleKey(title.name());
        queueVisualUpdate(() -> platformAdapter.updateDisplayState(record));
        return true;
    }

    public boolean setTabPing(UUID uuid, int pingMillis) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null) {
            return false;
        }
        record.setTabPingMillis(clampTabPingMillis(pingMillis));
        queueVisualUpdate(() -> platformAdapter.updateDisplayState(record));
        return true;
    }

    public boolean announceAdvancement(UUID uuid, NamespacedKey advancementKey) {
        if (advancementKey == null) {
            return false;
        }

        FakePlayerRecord record = records.get(uuid);
        if (record == null || !record.isActive()) {
            return false;
        }

        Advancement advancement = Bukkit.getAdvancement(advancementKey);
        if (advancement == null) {
            return false;
        }

        Optional<Player> fakeBukkitPlayer = resolveBukkitPlayer(record);
        if (fakeBukkitPlayer.isPresent()) {
            try {
                AdvancementProgress progress = fakeBukkitPlayer.get().getAdvancementProgress(advancement);
                for (String criterion : progress.getRemainingCriteria()) {
                    progress.awardCriteria(criterion);
                }
            } catch (Exception ignored) {
            }
        }

        String advancementTitle = humanizeAdvancementKey(advancement.getKey());
        //Bukkit.broadcastMessage(ChatColor.GREEN + record.getName() + " has made the advancement [" + advancementTitle + "]");

        record.setLastSeenAt(Instant.now());
        return true;
    }

    public void saveNow() {
        repository.save(records.values(), skinService.snapshotCache());
    }

    public boolean killFakePlayer(UUID uuid, String killerName) {
        FakePlayerRecord record = records.get(uuid);
        if (record == null || !record.isActive()) {
            return false;
        }

        String reason = "Killed";
        if (killerName != null && !killerName.isBlank()) {
            reason = "Killed by " + killerName;
        }

        record.setActive(false);
        queueVisualUpdate(() -> platformAdapter.updateDisplayState(record));

        Bukkit.broadcastMessage(
                "§4☠ " + record.getName() + " has perished permanently. §7(" + reason + ")"
        );
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
                .addBan(record.getName(), "You have died.", null, null);

        announceFakeLeave(record);
        saveNow();
        return true;
    }

    public void announceFakeJoin(FakePlayerRecord record) {
        announceFakePresenceChange(record, true);
    }

    public void announceFakeLeave(FakePlayerRecord record) {
        announceFakePresenceChange(record, false);
    }

    public Optional<Player> resolveBukkitPlayer(FakePlayerRecord record) {
        return platformAdapter.getBukkitPlayer(record);
    }

    public void sendFakeChat(FakePlayerRecord record, String message) {
        if (record == null || message == null) {
            return;
        }
        Optional<Player> player = resolveBukkitPlayer(record);
        if (player.isPresent()) {
            try {
                Player chatPlayer = player.get();
                AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(
                        false,
                        chatPlayer,
                        message,
                        new HashSet<>(Bukkit.getOnlinePlayers())
                );
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                String formatted = String.format(event.getFormat(), chatPlayer.getDisplayName(), event.getMessage());
                for (Player recipient : event.getRecipients()) {
                    recipient.sendMessage(formatted);
                }
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.broadcastMessage(record.getName() + ": " + message);
    }

    private String humanizeAdvancementKey(NamespacedKey key) {
        if (key == null) {
            return "Unknown";
        }
        String value = key.getKey();
        int slash = value.lastIndexOf('/');
        if (slash >= 0 && slash < value.length() - 1) {
            value = value.substring(slash + 1);
        }
        String[] words = value.replace('_', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.length() == 0 ? key.toString() : builder.toString();
    }

    private boolean isCooldownReady(Instant lastReactionAt, Instant now, Duration cooldown) {
        if (lastReactionAt == null) {
            return true;
        }
        return !lastReactionAt.plus(cooldown).isAfter(now);
    }

    public static UUID deterministicUuid(String name) {
        String seed = UUID_NAMESPACE + name.toLowerCase();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private void ensureNormalizedIdentity(FakePlayerRecord record) {
        if (record == null) {
            return;
        }
        String name = record.getName();
        String normalizedName = name == null ? "unknown" : name.trim();
        if (normalizedName.isEmpty()) {
            normalizedName = "unknown";
        }
        record.setName(normalizedName);
        record.setUuid(deterministicUuid(normalizedName));
    }

    private String normalizeOwner(String skinOwner, String fallbackName) {
        String owner = skinOwner;
        if (owner == null || owner.isBlank()) {
            owner = fallbackName;
        }
        return owner == null ? "unknown" : owner.trim().toLowerCase();
    }

    private int clampTabPingMillis(int pingMillis) {
        return Math.max(0, Math.min(9999, pingMillis));
    }

    private void initializeTabPresentationDefaults(FakePlayerRecord record) {
        if (record == null || record.getUuid() == null) {
            return;
        }
        if (record.getTabTitleKey() == null || record.getTabTitleKey().isBlank()) {
            Title[] titles = Title.values();
            int index = Math.floorMod(record.getUuid().hashCode(), titles.length);
            record.setTabTitleKey(titles[index].name());
        }
        if (record.getTabPingMillis() <= 0) {
            int hash = Math.floorMod(record.getUuid().hashCode(), 101);
            record.setTabPingMillis(40 + hash);
        }
    }

    private void announceFakePresenceChange(FakePlayerRecord record, boolean joined) {
        if (record == null) {
            return;
        }
        String baseMessage = ChatColor.YELLOW + record.getName() + (joined ? " joined the game" : " left the game");
        Optional<Player> player = platformAdapter.getBukkitPlayer(record);
        if (joined) {
            ensureFakePlayerData(record);
        }
        if (player.isPresent()) {
            if (joined) {
                PlayerJoinEvent event = new PlayerJoinEvent(player.get(), baseMessage);
                Bukkit.getPluginManager().callEvent(event);
                broadcastIfPresent(event.getJoinMessage());
            } else {
                PlayerQuitEvent event = new PlayerQuitEvent(player.get(), baseMessage);
                Bukkit.getPluginManager().callEvent(event);
                broadcastIfPresent(event.getQuitMessage());
            }
        } else {
            Bukkit.broadcastMessage(baseMessage);
        }
        notifyDiscordSrvPresence(player.orElse(null), baseMessage, joined);
    }

    private void broadcastIfPresent(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Bukkit.broadcastMessage(message);
    }

    private boolean notifyDiscordSrvPresence(Player player, String message, boolean joined) {
        try {
            Class<?> discordSrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Object api = resolveDiscordSrvApi(discordSrvClass);
            if (api == null) {
                return false;
            }
            String eventClassName = joined
                    ? "github.scarsz.discordsrv.api.events.GamePlayerJoinEvent"
                    : "github.scarsz.discordsrv.api.events.GamePlayerQuitEvent";
            Class<?> eventClass = null;
            try {
                eventClass = Class.forName(eventClassName);
            } catch (ClassNotFoundException ignored) {
                if (!joined) {
                    eventClass = Class.forName("github.scarsz.discordsrv.api.events.GamePlayerLeaveEvent");
                }
            }
            if (eventClass == null) {
                return false;
            }
            Object event = constructDiscordEvent(eventClass, player, message);
            if (event == null) {
                return false;
            }
            Method callEvent = resolveDiscordSrvCall(api);
            if (callEvent == null) {
                return false;
            }
            callEvent.invoke(api, event);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Object resolveDiscordSrvApi(Class<?> discordSrvClass) {
        try {
            Field apiField = discordSrvClass.getField("api");
            return apiField.get(null);
        } catch (Exception ignored) {
        }
        try {
            Field apiField = discordSrvClass.getDeclaredField("api");
            apiField.setAccessible(true);
            return apiField.get(null);
        } catch (Exception ignored) {
        }
        try {
            Method getApi = discordSrvClass.getMethod("getApi");
            return getApi.invoke(null);
        } catch (Exception ignored) {
        }
        try {
            Method getPlugin = discordSrvClass.getMethod("getPlugin");
            Object plugin = getPlugin.invoke(null);
            if (plugin == null) {
                return null;
            }
            Method getApi = plugin.getClass().getMethod("getApi");
            return getApi.invoke(plugin);
        } catch (Exception ignored) {
        }
        return null;
    }

    private Method resolveDiscordSrvCall(Object api) {
        for (String methodName : new String[]{"callEvent", "dispatchEvent", "invokeEvent"}) {
            try {
                return api.getClass().getMethod(methodName, Object.class);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Object constructDiscordEvent(Class<?> eventClass, Player player, String message) {
        Constructor<?>[] constructors = eventClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] params = constructor.getParameterTypes();
            try {
                if (params.length == 2 && Player.class.isAssignableFrom(params[0]) && params[1] == String.class) {
                    return constructor.newInstance(player, message);
                }
                if (params.length == 2 && params[0] == String.class && Player.class.isAssignableFrom(params[1])) {
                    return constructor.newInstance(message, player);
                }
                if (params.length == 1 && Player.class.isAssignableFrom(params[0])) {
                    return constructor.newInstance(player);
                }
                if (params.length == 1 && params[0] == String.class) {
                    return constructor.newInstance(message);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void ensureFakePlayerData(FakePlayerRecord record) {
        if (record == null) {
            return;
        }
        ensureNormalizedIdentity(record);
        Path worldFolder = resolvePrimaryWorldFolder();
        if (worldFolder == null) {
            return;
        }
        ensurePlayerDat(worldFolder, record);
        ensureStatsFile(worldFolder, record);
        ensureUserCache(worldFolder, record);
    }

    private Path resolvePrimaryWorldFolder() {
        if (Bukkit.getWorlds().isEmpty()) {
            return null;
        }
        return Bukkit.getWorlds().get(0).getWorldFolder().toPath();
    }

    private void ensurePlayerDat(Path worldFolder, FakePlayerRecord record) {
        try {
            Path playerDataDir = worldFolder.resolve("playerdata");
            Files.createDirectories(playerDataDir);
            Path datFile = playerDataDir.resolve(record.getUuid().toString() + ".dat");
            if (Files.exists(datFile)) {
                return;
            }
            if (!writeEmptyPlayerDat(datFile)) {
                Files.write(datFile, new byte[0]);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean writeEmptyPlayerDat(Path datFile) {
        try {
            Class<?> compoundTagClass = Class.forName("net.minecraft.nbt.CompoundTag");
            Object compoundTag = compoundTagClass.getConstructor().newInstance();
            Class<?> nbtIoClass = Class.forName("net.minecraft.nbt.NbtIo");
            Method writeCompressed = nbtIoClass.getMethod("writeCompressed", compoundTagClass, Path.class);
            writeCompressed.invoke(null, compoundTag, datFile);
            return true;
        } catch (Exception ignored) {
        }
        try {
            Class<?> compoundTagClass = Class.forName("net.minecraft.nbt.CompoundTag");
            Object compoundTag = compoundTagClass.getConstructor().newInstance();
            Class<?> nbtIoClass = Class.forName("net.minecraft.nbt.NbtIo");
            Method writeCompressed = nbtIoClass.getMethod("writeCompressed", compoundTagClass, java.io.File.class);
            writeCompressed.invoke(null, compoundTag, datFile.toFile());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private void ensureStatsFile(Path worldFolder, FakePlayerRecord record) {
        try {
            Path statsDir = worldFolder.resolve("stats");
            Files.createDirectories(statsDir);
            Path statsFile = statsDir.resolve(record.getUuid().toString() + ".json");
            if (Files.exists(statsFile)) {
                return;
            }
            Files.writeString(statsFile, "{}", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private void ensureUserCache(Path worldFolder, FakePlayerRecord record) {
        try {
            Path parent = worldFolder.getParent();
            if (parent == null) {
                return;
            }
            Path userCache = parent.resolve("usercache.json");
            JsonArray entries = readUserCache(userCache);
            String uuid = record.getUuid().toString();
            String name = record.getName();
            boolean updated = false;
            for (JsonElement element : entries) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                if (uuid.equalsIgnoreCase(getAsString(obj, "uuid"))
                        || name.equalsIgnoreCase(getAsString(obj, "name"))) {
                    obj.addProperty("uuid", uuid);
                    obj.addProperty("name", name);
                    obj.addProperty("expiresOn", userCacheExpiry());
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                JsonObject entry = new JsonObject();
                entry.addProperty("uuid", uuid);
                entry.addProperty("name", name);
                entry.addProperty("expiresOn", userCacheExpiry());
                entries.add(entry);
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(userCache, gson.toJson(entries), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private JsonArray readUserCache(Path userCache) {
        if (userCache == null || !Files.exists(userCache)) {
            return new JsonArray();
        }
        try {
            String content = Files.readAllLines(userCache, StandardCharsets.UTF_8).stream()
                    .collect(Collectors.joining("\n"));
            JsonElement parsed = new Gson().fromJson(content, JsonElement.class);
            if (parsed != null && parsed.isJsonArray()) {
                return parsed.getAsJsonArray();
            }
        } catch (Exception ignored) {
        }
        return new JsonArray();
    }

    private String getAsString(JsonObject obj, String key) {
        if (obj == null || key == null) {
            return "";
        }
        JsonElement element = obj.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private String userCacheExpiry() {
        ZonedDateTime expiry = ZonedDateTime.now(ZoneId.of("UTC")).plusDays(30);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z").format(expiry);
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
        private static SkinUpdateOutcome notFoundOutcome() {
            return new SkinUpdateOutcome(false, false, false, true);
        }

        private static SkinUpdateOutcome failedNoSkinOutcome() {
            return new SkinUpdateOutcome(false, false, false, false);
        }
    }
}
