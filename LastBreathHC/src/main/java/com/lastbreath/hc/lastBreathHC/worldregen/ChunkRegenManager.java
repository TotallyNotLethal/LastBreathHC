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
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.Arrays;
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
    private final Map<String, RegenTemplateWorld> regenTemplateWorlds = new HashMap<>();

    private final AtomicLong chunksScanned = new AtomicLong();
    private final AtomicLong chunksSkipped = new AtomicLong();
    private final AtomicLong chunksRegenerated = new AtomicLong();

    public ChunkRegenManager(LastBreathHC plugin, PlayerPlacedBlockIndex playerPlacedBlockIndex) {
        this.plugin = plugin;
        this.playerPlacedBlockIndex = playerPlacedBlockIndex;
        this.backupFolder = new File(plugin.getDataFolder(), "world-regen-backups");
    }

    public void shutdown() {
        shutdownTemplateWorlds();
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
        shutdownTemplateWorlds();
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
                    if (tryRegenerateChunk(world, coord.chunkX(), coord.chunkZ())) {
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


    public SingleChunkResult regenerateCurrentChunk(Player player, boolean forceRegenerate) {
        ChunkRegenSettings settings = ChunkRegenSettings.fromConfig(plugin.getConfig());
        if (!settings.enabled()) {
            return new SingleChunkResult("§cworld-regen.enabled is false in config.");
        }

        Chunk chunk = player.getLocation().getChunk();
        if (hasOtherPlayersSeeing(chunk, player.getUniqueId())) {
            return new SingleChunkResult("§cCannot regenerate while other players are seeing this chunk.");
        }

        if (!forceRegenerate) {
            if (settings.detectPlayerBlocks()
                    && playerPlacedBlockIndex.hasPlayerPlacedBlockInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ())) {
                chunksScanned.incrementAndGet();
                chunksSkipped.incrementAndGet();
                return new SingleChunkResult("§eSkipped: player-placed block index marks this chunk as modified. Use /lbhc regen chunk force to override.");
            }

            ChunkRegenScanner.ScanResult scanResult = new ChunkRegenScanner(chunk).scan(Integer.MAX_VALUE);
            chunksScanned.incrementAndGet();
            if (scanResult.state() != ChunkRegenScanner.State.SAFE) {
                chunksSkipped.incrementAndGet();
                return new SingleChunkResult("§eSkipped: " + scanResult.reason() + " Use /lbhc regen chunk force to override.");
            }
        }

        backupChunk(chunk);
        boolean success = tryRegenerateChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
        if (!success) {
            chunksSkipped.incrementAndGet();
            return new SingleChunkResult("§cRegeneration failed for chunk " + chunk.getX() + "," + chunk.getZ() + ". Server version does not expose chunk regeneration API.");
        }

        chunksRegenerated.incrementAndGet();
        if (forceRegenerate) {
            return new SingleChunkResult("§aForce regenerated chunk " + chunk.getX() + "," + chunk.getZ() + " and saved backup.");
        }
        return new SingleChunkResult("§aRegenerated chunk " + chunk.getX() + "," + chunk.getZ() + " and saved backup.");
    }

    public SingleChunkResult reloadCurrentChunk(Player player, boolean forceRegenerate) {
        Chunk chunk = player.getLocation().getChunk();
        if (hasOtherPlayersSeeing(chunk, player.getUniqueId())) {
            return new SingleChunkResult("§cCannot reload while other players are seeing this chunk.");
        }

        File backup = latestBackup(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (backup == null) {
            if (forceRegenerate) {
                boolean success = tryRegenerateChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
                if (success) {
                    chunksRegenerated.incrementAndGet();
                    return new SingleChunkResult("§aForce regenerated chunk " + chunk.getX() + "," + chunk.getZ() + " without backup.");
                }
                chunksSkipped.incrementAndGet();
                return new SingleChunkResult("§cNo backup exists and forced regeneration is not available on this server version.");
            }
            return new SingleChunkResult("§eNo backup found for chunk " + chunk.getX() + "," + chunk.getZ() + ". Use /lbhc regen reloadchunk force to attempt direct regeneration.");
        }

        if (!restoreChunkBackup(backup)) {
            return new SingleChunkResult("§cFailed to reload backup for chunk " + chunk.getX() + "," + chunk.getZ() + ".");
        }
        return new SingleChunkResult("§aReloaded chunk " + chunk.getX() + "," + chunk.getZ() + " from backup.");
    }


    private boolean tryRegenerateChunk(World world, int chunkX, int chunkZ) {
        try {
            return world.regenerateChunk(chunkX, chunkZ);
        } catch (UnsupportedOperationException ex) {
            plugin.getLogger().warning("[WorldRegen] Native chunk regeneration is unsupported by this server version/API. Falling back to template world regeneration.");
            return tryTemplateWorldRegeneration(world, chunkX, chunkZ);
        } catch (Exception ex) {
            plugin.getLogger().warning("[WorldRegen] Failed to regenerate chunk " + chunkX + "," + chunkZ + ": " + ex.getMessage());
            return tryTemplateWorldRegeneration(world, chunkX, chunkZ);
        }
    }

    private boolean tryTemplateWorldRegeneration(World world, int chunkX, int chunkZ) {
        RegenTemplateWorld template = regenTemplateWorlds.computeIfAbsent(world.getName(), key -> createTemplateWorld(world));
        if (template == null || template.world() == null) {
            return false;
        }

        World templateWorld = template.world();
        try {
            Chunk sourceChunk = templateWorld.getChunkAt(chunkX, chunkZ);
            Chunk targetChunk = world.getChunkAt(chunkX, chunkZ);

            removeNonPlayerEntities(targetChunk);
            copyChunkBlocks(sourceChunk, targetChunk);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("[WorldRegen] Template regeneration failed for chunk " + chunkX + "," + chunkZ + ": " + ex.getMessage());
            return false;
        }
    }

    private RegenTemplateWorld createTemplateWorld(World source) {
        String sourceName = source.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        String tempWorldName = "lbhc_regen_" + sourceName + "_" + UUID.randomUUID().toString().substring(0, 8);
        Path templateWorldFolder = Bukkit.getWorldContainer().toPath().resolve(tempWorldName);
        try {
            bootstrapTemplateWorldFolder(source, templateWorldFolder);

            WorldCreator creator = new WorldCreator(tempWorldName)
                    .environment(source.getEnvironment())
                    .type(source.getWorldType());

            if (source.getGenerator() != null) {
                creator.generator(source.getGenerator());
            }

            World templateWorld = creator.createWorld();
            if (templateWorld == null) {
                plugin.getLogger().warning("[WorldRegen] Failed to create template world " + tempWorldName + ".");
                return null;
            }

            templateWorld.setAutoSave(false);
            Path folderPath = templateWorld.getWorldFolder().toPath();
            plugin.getLogger().info("[WorldRegen] Created regeneration template world " + templateWorld.getName() + " for source world " + source.getName() + " using copied world metadata.");
            return new RegenTemplateWorld(templateWorld, folderPath);
        } catch (Exception ex) {
            plugin.getLogger().warning("[WorldRegen] Failed to initialize template world for " + source.getName() + ": " + ex.getMessage());
            deleteDirectory(templateWorldFolder);
            return null;
        }
    }

    private void bootstrapTemplateWorldFolder(World source, Path templateWorldFolder) throws IOException {
        Path sourceWorldFolder = source.getWorldFolder().toPath();
        Files.createDirectories(templateWorldFolder);

        copyFileIfExists(sourceWorldFolder.resolve("level.dat"), templateWorldFolder.resolve("level.dat"));
        copyFileIfExists(sourceWorldFolder.resolve("level.dat_old"), templateWorldFolder.resolve("level.dat_old"));
        copyDirectoryIfExists(sourceWorldFolder.resolve("datapacks"), templateWorldFolder.resolve("datapacks"));
    }

    private void copyFileIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private void copyDirectoryIfExists(Path source, Path target) throws IOException {
        if (Files.notExists(source)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(source)) {
            walk.forEach(path -> {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to copy template world metadata path " + path, ex);
                }
            });
        } catch (IllegalStateException ex) {
            if (ex.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw ex;
        }
    }

    private void copyChunkBlocks(Chunk sourceChunk, Chunk targetChunk) {
        int minY = sourceChunk.getWorld().getMinHeight();
        int maxY = sourceChunk.getWorld().getMaxHeight() - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block sourceBlock = sourceChunk.getBlock(x, y, z);
                    Block targetBlock = targetChunk.getBlock(x, y, z);
                    targetBlock.setBlockData(sourceBlock.getBlockData(), false);
                }
            }
        }
    }

    private void removeNonPlayerEntities(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            entity.remove();
        }
    }

    private void shutdownTemplateWorlds() {
        if (regenTemplateWorlds.isEmpty()) {
            return;
        }

        for (RegenTemplateWorld template : new ArrayList<>(regenTemplateWorlds.values())) {
            World templateWorld = template.world();
            if (templateWorld != null) {
                try {
                    Bukkit.unloadWorld(templateWorld, false);
                } catch (Exception ex) {
                    plugin.getLogger().warning("[WorldRegen] Failed to unload template world " + templateWorld.getName() + ": " + ex.getMessage());
                }
            }
            deleteDirectory(template.folderPath());
        }
        regenTemplateWorlds.clear();
    }

    private void deleteDirectory(Path directory) {
        if (directory == null || Files.notExists(directory)) {
            return;
        }
        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            plugin.getLogger().warning("[WorldRegen] Failed to delete " + path + ": " + ex.getMessage());
                        }
                    });
        } catch (IOException ex) {
            plugin.getLogger().warning("[WorldRegen] Failed to cleanup template world directory " + directory + ": " + ex.getMessage());
        }
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

    public record RegenTemplateWorld(World world, Path folderPath) {}

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
