package com.lastbreath.hc.lastBreathHC.worldregen;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.structures.PlayerPlacedBlockIndex;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ChunkRegenManager {
    private final LastBreathHC plugin;
    private final PlayerPlacedBlockIndex playerPlacedBlockIndex;
    private final File backupFolder;

    private final Queue<ChunkCoord> pendingChunks = new ConcurrentLinkedQueue<>();
    private final Map<ChunkCoord, ChunkRegenScanner> activeScans = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean producerDone = new AtomicBoolean(false);

    private BukkitTask tickTask;
    private BukkitTask producerTask;
    private volatile RegenRunOptions activeOptions;

    private final AtomicLong chunksScanned = new AtomicLong();
    private final AtomicLong chunksSkipped = new AtomicLong();
    private final AtomicLong chunksRegenerated = new AtomicLong();

    public ChunkRegenManager(LastBreathHC plugin, PlayerPlacedBlockIndex playerPlacedBlockIndex) {
        this.plugin = plugin;
        this.playerPlacedBlockIndex = playerPlacedBlockIndex;
        this.backupFolder = new File(plugin.getDataFolder(), "world-regen-backups");
    }

    public void shutdown() {
        stop();
    }

    public boolean start(Player initiator, RegenRunOptions options) {
        if (running.get()) {
            return false;
        }
        ChunkRegenSettings settings = ChunkRegenSettings.fromConfig(plugin.getConfig());
        if (!settings.enabled()) {
            if (initiator != null) {
                initiator.sendMessage("§cworld-regen.enabled is false in config.");
            }
            return false;
        }

        running.set(true);
        activeOptions = options;
        chunksScanned.set(0);
        chunksSkipped.set(0);
        chunksRegenerated.set(0);
        producerDone.set(false);
        pendingChunks.clear();
        activeScans.clear();

        producerTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> produceChunkTargets(options));
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> processTick(settings), 1L, 1L);
        plugin.getLogger().info("[WorldRegen] Started scan in world " + options.world().getName() + " with radius " + options.radiusBlocks() + " blocks.");
        return true;
    }

    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (producerTask != null) {
            producerTask.cancel();
            producerTask = null;
        }
        pendingChunks.clear();
        activeScans.clear();
        plugin.getLogger().info("[WorldRegen] Stopped.");
    }

    public RegenStatus status() {
        return new RegenStatus(
                running.get(),
                pendingChunks.size(),
                activeScans.size(),
                chunksScanned.get(),
                chunksSkipped.get(),
                chunksRegenerated.get(),
                activeOptions
        );
    }

    public int rollbackAround(Location center, int radiusBlocks) {
        if (center == null || center.getWorld() == null) {
            return 0;
        }
        World world = center.getWorld();
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;
        int radiusChunks = Math.max(0, radiusBlocks / 16);
        int restored = 0;

        for (int cx = centerChunkX - radiusChunks; cx <= centerChunkX + radiusChunks; cx++) {
            for (int cz = centerChunkZ - radiusChunks; cz <= centerChunkZ + radiusChunks; cz++) {
                File backup = latestBackup(world.getName(), cx, cz);
                if (backup == null) {
                    continue;
                }
                if (restoreChunkBackup(backup)) {
                    restored++;
                }
            }
        }
        return restored;
    }

    public int purgeBackups() {
        if (!backupFolder.exists()) {
            return 0;
        }
        int deleted = 0;
        File[] files = backupFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return 0;
        }
        for (File file : files) {
            if (file.delete()) {
                deleted++;
            }
        }
        return deleted;
    }

    private void produceChunkTargets(RegenRunOptions options) {
        World world = options.world();
        int centerChunkX = options.centerX() >> 4;
        int centerChunkZ = options.centerZ() >> 4;
        int radiusChunks = Math.max(1, options.radiusBlocks() / 16);

        for (int cx = centerChunkX - radiusChunks; cx <= centerChunkX + radiusChunks; cx++) {
            for (int cz = centerChunkZ - radiusChunks; cz <= centerChunkZ + radiusChunks; cz++) {
                if (!running.get()) {
                    producerDone.set(true);
                    return;
                }
                pendingChunks.add(new ChunkCoord(world.getName(), cx, cz));
            }
        }
        producerDone.set(true);
        plugin.getLogger().info("[WorldRegen] Enqueued " + pendingChunks.size() + " chunks for scan.");
    }

    private void processTick(ChunkRegenSettings settings) {
        if (!running.get()) {
            return;
        }

        int chunkOps = 0;
        int blockBudget = settings.scanBlocksPerTick();

        while (chunkOps < settings.chunksPerTick() && blockBudget > 0) {
            ChunkCoord coord = nextChunkCoord();
            if (coord == null) {
                if (pendingChunks.isEmpty() && activeScans.isEmpty() && producerFinished()) {
                    plugin.getLogger().info("[WorldRegen] Completed. scanned=" + chunksScanned.get() + " skipped=" + chunksSkipped.get() + " regenerated=" + chunksRegenerated.get());
                    stop();
                }
                break;
            }

            World world = Bukkit.getWorld(coord.worldName());
            if (world == null) {
                chunksSkipped.incrementAndGet();
                chunkOps++;
                continue;
            }

            boolean wasLoaded = world.isChunkLoaded(coord.chunkX(), coord.chunkZ());
            Chunk chunk = world.getChunkAt(coord.chunkX(), coord.chunkZ());
            if (shouldSkipChunk(settings, activeOptions, wasLoaded, chunk)) {
                chunksSkipped.incrementAndGet();
                chunksScanned.incrementAndGet();
                chunkOps++;
                maybeLogProgress();
                continue;
            }

            if (settings.detectPlayerBlocks() && playerPlacedBlockIndex.hasPlayerPlacedBlockInChunk(world.getName(), coord.chunkX(), coord.chunkZ())) {
                chunksSkipped.incrementAndGet();
                chunksScanned.incrementAndGet();
                chunkOps++;
                maybeLogProgress();
                continue;
            }

            ChunkRegenScanner scanner = activeScans.computeIfAbsent(coord, ignored -> new ChunkRegenScanner(chunk));
            ChunkRegenScanner.ScanResult result = scanner.scan(blockBudget);
            blockBudget -= Math.max(0, result.blocksProcessed());

            if (result.state() == ChunkRegenScanner.State.IN_PROGRESS) {
                chunkOps++;
                continue;
            }

            activeScans.remove(coord);
            chunksScanned.incrementAndGet();

            if (result.state() == ChunkRegenScanner.State.PLAYER_MODIFIED) {
                chunksSkipped.incrementAndGet();
            } else {
                if (canRegenerate(chunk)) {
                    backupChunk(chunk);
                    if (world.regenerateChunk(coord.chunkX(), coord.chunkZ())) {
                        chunksRegenerated.incrementAndGet();
                    } else {
                        chunksSkipped.incrementAndGet();
                    }
                } else {
                    chunksSkipped.incrementAndGet();
                }
            }

            maybeLogProgress();
            chunkOps++;
        }
    }

    private ChunkCoord nextChunkCoord() {
        ChunkCoord fromQueue = pendingChunks.poll();
        if (fromQueue != null) {
            return fromQueue;
        }
        for (Map.Entry<ChunkCoord, ChunkRegenScanner> entry : activeScans.entrySet()) {
            return entry.getKey();
        }
        return null;
    }

    private boolean shouldSkipChunk(ChunkRegenSettings settings, RegenRunOptions options, boolean wasLoaded, Chunk chunk) {
        if ((options != null && options.unloadedOnly()) && wasLoaded) {
            return true;
        }
        if (settings.skipLoaded() && wasLoaded) {
            return true;
        }
        if (!chunk.getPlayersSeeingChunk().isEmpty()) {
            return true;
        }
        return false;
    }


    public SingleChunkResult regenerateCurrentChunk(Player player) {
        ChunkRegenSettings settings = ChunkRegenSettings.fromConfig(plugin.getConfig());
        if (!settings.enabled()) {
            return new SingleChunkResult("§cworld-regen.enabled is false in config.");
        }

        Chunk chunk = player.getLocation().getChunk();
        if (hasOtherPlayersSeeing(chunk, player.getUniqueId())) {
            return new SingleChunkResult("§cCannot regenerate while other players are seeing this chunk.");
        }

        if (settings.detectPlayerBlocks()
                && playerPlacedBlockIndex.hasPlayerPlacedBlockInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ())) {
            chunksScanned.incrementAndGet();
            chunksSkipped.incrementAndGet();
            return new SingleChunkResult("§eSkipped: player-placed block index marks this chunk as modified.");
        }

        ChunkRegenScanner.ScanResult scanResult = new ChunkRegenScanner(chunk).scan(Integer.MAX_VALUE);
        chunksScanned.incrementAndGet();
        if (scanResult.state() != ChunkRegenScanner.State.SAFE) {
            chunksSkipped.incrementAndGet();
            return new SingleChunkResult("§eSkipped: " + scanResult.reason());
        }

        backupChunk(chunk);
        boolean success = chunk.getWorld().regenerateChunk(chunk.getX(), chunk.getZ());
        if (!success) {
            chunksSkipped.incrementAndGet();
            return new SingleChunkResult("§cRegeneration failed for chunk " + chunk.getX() + "," + chunk.getZ() + ".");
        }

        chunksRegenerated.incrementAndGet();
        return new SingleChunkResult("§aRegenerated chunk " + chunk.getX() + "," + chunk.getZ() + " and saved backup.");
    }

    public SingleChunkResult reloadCurrentChunk(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        if (hasOtherPlayersSeeing(chunk, player.getUniqueId())) {
            return new SingleChunkResult("§cCannot reload while other players are seeing this chunk.");
        }

        File backup = latestBackup(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (backup == null) {
            return new SingleChunkResult("§eNo backup found for chunk " + chunk.getX() + "," + chunk.getZ() + ".");
        }

        if (!restoreChunkBackup(backup)) {
            return new SingleChunkResult("§cFailed to reload backup for chunk " + chunk.getX() + "," + chunk.getZ() + ".");
        }
        return new SingleChunkResult("§aReloaded chunk " + chunk.getX() + "," + chunk.getZ() + " from backup.");
    }

    private boolean canRegenerate(Chunk chunk) {
        return !hasOtherPlayersSeeing(chunk, null);
    }

    private boolean hasOtherPlayersSeeing(Chunk chunk, UUID allowedViewer) {
        return chunk.getPlayersSeeingChunk().stream()
                .map(Player::getUniqueId)
                .anyMatch(id -> allowedViewer == null || !allowedViewer.equals(id));
    }

    private void maybeLogProgress() {
        long scanned = chunksScanned.get();
        if (scanned > 0 && scanned % 500 == 0) {
            plugin.getLogger().info("[WorldRegen] Progress scanned=" + scanned + " skipped=" + chunksSkipped.get() + " regenerated=" + chunksRegenerated.get());
        }
    }

    private boolean producerFinished() {
        return producerDone.get();
    }

    private void backupChunk(Chunk chunk) {
        YamlConfiguration yaml = new YamlConfiguration();
        World world = chunk.getWorld();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        long now = System.currentTimeMillis();

        yaml.set("world", world.getName());
        yaml.set("chunkX", cx);
        yaml.set("chunkZ", cz);
        yaml.set("timestamp", now);

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        yaml.set("minY", minY);
        yaml.set("maxY", maxY);

        List<Map<String, Object>> blocks = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getType().isAir()) {
                        continue;
                    }
                    Map<String, Object> stateMap = new HashMap<>();
                    stateMap.put("x", x);
                    stateMap.put("y", y);
                    stateMap.put("z", z);
                    stateMap.put("blockData", block.getBlockData().getAsString());
                    BlockState state = block.getState(false);
                    if (state != null) {
                        stateMap.put("state", state.serialize());
                    }
                    if (block.getState(false) instanceof Container container) {
                        stateMap.put("inventory", container.getInventory().getContents());
                    }
                    blocks.add(stateMap);
                }
            }
        }
        yaml.set("blocks", blocks);

        List<Map<String, Object>> entities = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            Map<String, Object> entityMap = new HashMap<>();
            entityMap.put("type", entity.getType().name());
            entityMap.put("x", entity.getLocation().getX());
            entityMap.put("y", entity.getLocation().getY());
            entityMap.put("z", entity.getLocation().getZ());
            entityMap.put("yaw", entity.getLocation().getYaw());
            entityMap.put("pitch", entity.getLocation().getPitch());
            if (entity instanceof ArmorStand armorStand) {
                entityMap.put("customName", armorStand.getCustomName());
                entityMap.put("visible", armorStand.isVisible());
                entityMap.put("arms", armorStand.hasArms());
                entityMap.put("basePlate", armorStand.hasBasePlate());
                entityMap.put("small", armorStand.isSmall());
                entityMap.put("marker", armorStand.isMarker());
                entityMap.put("helmet", armorStand.getEquipment() != null ? armorStand.getEquipment().getHelmet() : null);
                entityMap.put("chestplate", armorStand.getEquipment() != null ? armorStand.getEquipment().getChestplate() : null);
                entityMap.put("leggings", armorStand.getEquipment() != null ? armorStand.getEquipment().getLeggings() : null);
                entityMap.put("boots", armorStand.getEquipment() != null ? armorStand.getEquipment().getBoots() : null);
                entityMap.put("mainHand", armorStand.getEquipment() != null ? armorStand.getEquipment().getItemInMainHand() : null);
                entityMap.put("offHand", armorStand.getEquipment() != null ? armorStand.getEquipment().getItemInOffHand() : null);
            }
            entities.add(entityMap);
        }
        yaml.set("entities", entities);

        if (!backupFolder.exists() && !backupFolder.mkdirs()) {
            plugin.getLogger().warning("[WorldRegen] Failed to create backup folder.");
            return;
        }

        File file = new File(backupFolder, world.getName() + "_" + cx + "_" + cz + "_" + now + ".yml");
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("[WorldRegen] Failed to backup chunk " + cx + "," + cz + ": " + ex.getMessage());
        }
    }

    private File latestBackup(String worldName, int chunkX, int chunkZ) {
        if (!backupFolder.exists()) {
            return null;
        }
        String prefix = worldName + "_" + chunkX + "_" + chunkZ + "_";
        File[] matches = backupFolder.listFiles((dir, name) -> name.startsWith(prefix) && name.endsWith(".yml"));
        if (matches == null || matches.length == 0) {
            return null;
        }
        return Arrays.stream(matches)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }

    private boolean restoreChunkBackup(File backupFile) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(backupFile);
        String worldName = yaml.getString("world", "");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }

        int chunkX = yaml.getInt("chunkX");
        int chunkZ = yaml.getInt("chunkZ");
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);

        int minY = yaml.getInt("minY", world.getMinHeight());
        int maxY = yaml.getInt("maxY", world.getMaxHeight() - 1);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    chunk.getBlock(x, y, z).setType(Material.AIR, false);
                }
            }
        }

        List<Map<?, ?>> blocks = yaml.getMapList("blocks");
        for (Map<?, ?> raw : blocks) {
            int x = ((Number) raw.getOrDefault("x", 0)).intValue();
            int y = ((Number) raw.getOrDefault("y", minY)).intValue();
            int z = ((Number) raw.getOrDefault("z", 0)).intValue();
            String blockData = Objects.toString(raw.get("blockData"), "minecraft:air");

            Block block = chunk.getBlock(x, y, z);
            try {
                block.setBlockData(Bukkit.createBlockData(blockData), false);
            } catch (IllegalArgumentException ignored) {
                block.setType(Material.AIR, false);
            }

            Object inventoryObj = raw.get("inventory");
            if (inventoryObj instanceof ItemStack[] inventory && block.getState(false) instanceof Container container) {
                container.getInventory().setContents(inventory);
                container.update(true, false);
            }
            BlockState blockState = block.getState(false);
            if (blockState != null) {
                blockState.update(true, false);
            }
        }

        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }

        List<Map<?, ?>> entities = yaml.getMapList("entities");
        for (Map<?, ?> raw : entities) {
            EntityType type = EntityType.valueOf(Objects.toString(raw.get("type"), "ARMOR_STAND").toUpperCase(Locale.ROOT));
            Location location = new Location(
                    world,
                    ((Number) raw.getOrDefault("x", 0.0D)).doubleValue(),
                    ((Number) raw.getOrDefault("y", 64.0D)).doubleValue(),
                    ((Number) raw.getOrDefault("z", 0.0D)).doubleValue(),
                    ((Number) raw.getOrDefault("yaw", 0.0F)).floatValue(),
                    ((Number) raw.getOrDefault("pitch", 0.0F)).floatValue()
            );

            Entity restored = world.spawnEntity(location, type);
            if (restored instanceof ArmorStand armorStand) {
                armorStand.setCustomName((String) raw.getOrDefault("customName", null));
                armorStand.setVisible(Boolean.TRUE.equals(raw.get("visible")));
                armorStand.setArms(Boolean.TRUE.equals(raw.get("arms")));
                armorStand.setBasePlate(Boolean.TRUE.equals(raw.get("basePlate")));
                armorStand.setSmall(Boolean.TRUE.equals(raw.get("small")));
                armorStand.setMarker(Boolean.TRUE.equals(raw.get("marker")));
                if (armorStand.getEquipment() != null) {
                    armorStand.getEquipment().setHelmet((ItemStack) raw.get("helmet"));
                    armorStand.getEquipment().setChestplate((ItemStack) raw.get("chestplate"));
                    armorStand.getEquipment().setLeggings((ItemStack) raw.get("leggings"));
                    armorStand.getEquipment().setBoots((ItemStack) raw.get("boots"));
                    armorStand.getEquipment().setItemInMainHand((ItemStack) raw.get("mainHand"));
                    armorStand.getEquipment().setItemInOffHand((ItemStack) raw.get("offHand"));
                }
            }
        }

        world.refreshChunk(chunkX, chunkZ);
        return true;
    }

    public record SingleChunkResult(String message) {}

    public record ChunkCoord(String worldName, int chunkX, int chunkZ) {}

    public record RegenRunOptions(World world, int centerX, int centerZ, int radiusBlocks, boolean unloadedOnly) {}

    public record RegenStatus(
            boolean running,
            int queuedChunks,
            int activeScans,
            long chunksScanned,
            long chunksSkipped,
            long chunksRegenerated,
            RegenRunOptions options
    ) {}
}
