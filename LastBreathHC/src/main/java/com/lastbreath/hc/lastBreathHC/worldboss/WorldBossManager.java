package com.lastbreath.hc.lastBreathHC.worldboss;

import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.GameRule;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.block.BlockFace;

import java.io.File;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class WorldBossManager implements Listener {

    private static final String CONFIG_ROOT = "worldBoss";
    private static final int DEFAULT_MIN_SECONDS = 1800;
    private static final int DEFAULT_MAX_SECONDS = 3600;
    private static final int MAX_LOCATION_ATTEMPTS = 25;
    private static final double DEFAULT_FALLBACK_RADIUS = 40.0;
    private static final int DEFAULT_ARENA_RADIUS = 24;
    private static final int DEFAULT_ARENA_WALL_HEIGHT = 6;
    private static final int PORTAL_WIDTH = 4;
    private static final int PORTAL_HEIGHT = 5;
    private static final double BLOOD_MOON_SPAWN_RADIUS = 100.0;

    private final Plugin plugin;
    private final BloodMoonManager bloodMoonManager;
    private final Random random = new Random();
    private final NamespacedKey bossTypeKey;
    private final NamespacedKey returnLocationKey;
    private final Map<TriggerType, Map<UUID, Long>> lastTriggerTimes = new EnumMap<>(TriggerType.class);
    private final Map<UUID, WorldBossController> activeBosses = new HashMap<>();
    private final WorldBossAntiCheese antiCheese;
    private BukkitTask randomSpawnTask;
    private BukkitTask bloodMoonCheckTask;
    private BukkitTask bossTickTask;
    private boolean lastBloodMoonActive;
    private int beaconTickCounter;
    private final Map<World, Set<Location>> portalBlocks = new HashMap<>();
    private final Map<UUID, Long> portalCooldowns = new HashMap<>();
    private final Set<Location> escapeBlocks = new HashSet<>();
    private boolean enabled = true;
    private boolean arenaMarkedForDeletion;
    private String markedArenaWorldName;
    private boolean bossDefeated;

    public WorldBossManager(Plugin plugin, BloodMoonManager bloodMoonManager) {
        this.plugin = plugin;
        this.bloodMoonManager = bloodMoonManager;
        this.bossTypeKey = new NamespacedKey(plugin, "world_boss_type");
        this.returnLocationKey = new NamespacedKey(plugin, "world_boss_return_location");
        this.antiCheese = new WorldBossAntiCheese(plugin);
        for (TriggerType triggerType : TriggerType.values()) {
            lastTriggerTimes.put(triggerType, new HashMap<>());
        }
    }

    public void start() {
        scheduleNextRandomSpawn();
        scheduleBloodMoonChecks();
        startBossTick();
        prepareInstancePortals();
    }

    public void shutdown() {
        if (randomSpawnTask != null) {
            randomSpawnTask.cancel();
            randomSpawnTask = null;
        }
        if (bloodMoonCheckTask != null) {
            bloodMoonCheckTask.cancel();
            bloodMoonCheckTask = null;
        }
        if (bossTickTask != null) {
            bossTickTask.cancel();
            bossTickTask = null;
        }
        for (WorldBossController controller : activeBosses.values()) {
            controller.cleanup();
            antiCheese.clear(controller.getBoss());
        }
        activeBosses.clear();
        removeAllPortals();
        portalCooldowns.clear();
    }

    public void enableBosses() {
        enabled = true;
        scheduleNextRandomSpawn();
        scheduleBloodMoonChecks();
    }

    public void disableBosses() {
        enabled = false;
        if (randomSpawnTask != null) {
            randomSpawnTask.cancel();
            randomSpawnTask = null;
        }
        if (bloodMoonCheckTask != null) {
            bloodMoonCheckTask.cancel();
            bloodMoonCheckTask = null;
        }
        World arenaWorld = resolveArenaWorld();
        if (arenaWorld != null) {
            for (Player player : arenaWorld.getPlayers()) {
                teleportPlayerToRespawn(player);
            }
        }
        for (WorldBossController controller : activeBosses.values()) {
            LivingEntity boss = controller.getBoss();
            if (boss != null && boss.isValid()) {
                boss.remove();
            }
            controller.cleanup();
            antiCheese.clear(controller.getBoss());
        }
        activeBosses.clear();
        removeAllPortals();
    }

    public boolean spawnTestBoss(World world, Location origin, WorldBossType overrideType) {
        if (!enabled) {
            return false;
        }
        if (world == null) {
            return false;
        }
        Location base = origin != null ? origin : world.getSpawnLocation();
        Biome baseBiome = world.getBiome(base.getBlockX(), base.getBlockY(), base.getBlockZ());
        WorldBossType bossType = overrideType != null
                ? overrideType
                : resolveBossType(baseBiome).orElseGet(() -> getFallbackBossType().orElse(null));
        if (bossType == null) {
            return false;
        }
        return spawnBossInArena(world, base, bossType);
    }

    public void createPortalAt(Location origin) {
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        createPortal(origin.getWorld(), origin);
    }

    public PortalPurgeResult purgeLegacyPortals() {
        Material frameMaterial = resolveMaterial(
                plugin.getConfig().getString(CONFIG_ROOT + ".arena.portal.frameMaterial", "GLOWSTONE"),
                Material.GLOWSTONE
        );
        Material innerMaterial = resolvePortalInnerMaterial(
                plugin.getConfig().getString(CONFIG_ROOT + ".arena.portal.innerMaterial", "PARTICLE")
        );
        Material escapeMaterial = resolveMaterial(
                plugin.getConfig().getString(CONFIG_ROOT + ".arena.escape.blockMaterial", "EMERALD_BLOCK"),
                Material.EMERALD_BLOCK
        );

        boolean escapeEnabled = plugin.getConfig().getBoolean(CONFIG_ROOT + ".arena.escape.enabled", true);

        Set<World> worldsToScan = new java.util.LinkedHashSet<>(getEligibleWorlds());
        World arenaWorld = resolveArenaWorld();
        if (arenaWorld != null) {
            worldsToScan.add(arenaWorld);
        }

        int frameRemoved = 0;
        int innerRemoved = 0;
        int escapeRemoved = 0;

        for (World world : worldsToScan) {
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight() - 1;
            int maxBaseY = maxY - (PORTAL_HEIGHT - 1);
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                int startX = chunk.getX() << 4;
                int startZ = chunk.getZ() << 4;
                for (int x = startX; x < startX + 16; x++) {
                    for (int z = startZ; z < startZ + 16; z++) {
                        for (int y = minY; y <= maxBaseY; y++) {
                            Block base = world.getBlockAt(x, y, z);
                            if (base.getType() != frameMaterial) {
                                continue;
                            }
                            if (!matchesPortalAt(world, x, y, z, frameMaterial, innerMaterial)) {
                                continue;
                            }
                            PortalPurgeResult removal = removePortalAt(world, x, y, z, frameMaterial, innerMaterial);
                            frameRemoved += removal.frameRemoved();
                            innerRemoved += removal.innerRemoved();
                            if (escapeEnabled) {
                                int escapeX = x - 4;
                                int escapeZ = z - 4;
                                Block escapeBlock = world.getBlockAt(escapeX, y, escapeZ);
                                if (escapeBlock.getType() == escapeMaterial) {
                                    escapeBlock.setType(Material.AIR);
                                    escapeRemoved++;
                                    escapeBlocks.remove(escapeBlock.getLocation());
                                }
                            }
                        }
                    }
                }
            }
        }

        return new PortalPurgeResult(frameRemoved, innerRemoved, escapeRemoved);
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        if (!event.toThunderState()) {
            return;
        }
        if (!isTriggerEnabled(TriggerType.THUNDERSTORM)) {
            return;
        }
        World world = event.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        if (!getEligibleWorlds().contains(world)) {
            return;
        }
        trySpawnTriggeredBoss(world, world.getSpawnLocation(), TriggerType.THUNDERSTORM);
    }

    @EventHandler
    public void onStructureInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isTriggerEnabled(TriggerType.STRUCTURE_INTERACTION)) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        if (!getStructureBlocks().contains(clicked.getType())) {
            return;
        }
        trySpawnTriggeredBoss(clicked.getWorld(), clicked.getLocation(), TriggerType.STRUCTURE_INTERACTION);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            String bossTypeName = living.getPersistentDataContainer()
                    .get(bossTypeKey, PersistentDataType.STRING);

            if (bossTypeName == null) {
                continue;
            }

            WorldBossType type = WorldBossType.fromConfigKey(bossTypeName).orElse(null);
            if (type == null) {
                continue;
            }

            if (activeBosses.containsKey(living.getUniqueId())) {
                continue;
            }

            WorldBossController controller = createController(type, living);
            if (controller != null) {
                controller.rebuildFromPersistent();
                activeBosses.put(living.getUniqueId(), controller);
            }
        }
    }

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity target) {
            WorldBossController controller = activeBosses.get(target.getUniqueId());
            if (controller != null) {
                controller.handleBossDamaged(event);
                antiCheese.recordDamage(controller, event);
            }
        }
        if (event.getDamager() instanceof LivingEntity damager) {
            WorldBossController controller = activeBosses.get(damager.getUniqueId());
            if (controller != null) {
                controller.handleBossAttack(event);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Location to = event.getTo();
        Location blockLocation = to.getBlock().getLocation();
        if (escapeBlocks.contains(blockLocation)) {
            long now = System.currentTimeMillis();
            long last = portalCooldowns.getOrDefault(event.getPlayer().getUniqueId(), 0L);
            if (now - last < 3000L) {
                return;
            }
            portalCooldowns.put(event.getPlayer().getUniqueId(), now);
            teleportToExit(event.getPlayer());
            return;
        }
        Set<Location> blocks = portalBlocks.get(to.getWorld());
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        if (!blocks.contains(blockLocation)) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = portalCooldowns.getOrDefault(event.getPlayer().getUniqueId(), 0L);
        if (now - last < 3000L) {
            return;
        }
        portalCooldowns.put(event.getPlayer().getUniqueId(), now);
        teleportViaPortal(event.getPlayer());
    }

    @EventHandler
    public void onBossBlockBreak(BlockBreakEvent event) {
        World arenaWorld = getLoadedArenaWorld();
        if (arenaWorld != null && event.getBlock().getWorld().equals(arenaWorld)) {
            if (!isBreakableMechanicBlock(event.getBlock())) {
                event.setCancelled(true);
                return;
            }
        }
        for (WorldBossController controller : activeBosses.values()) {
            controller.handleBlockBreak(event);
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        WorldBossController controller = activeBosses.remove(event.getEntity().getUniqueId());
        if (controller != null) {
            controller.cleanup();
            antiCheese.clear(event.getEntity());
            bossDefeated = true;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        World arenaWorld = resolveArenaWorld();
        if (arenaWorld == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.getWorld().equals(arenaWorld)) {
            return;
        }
        Location returnLocation = resolveReturnLocation(player);
        if (returnLocation != null) {
            storeReturnLocation(player, returnLocation);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location returnLocation = loadReturnLocation(player);
        if (returnLocation != null) {
            player.teleport(returnLocation);
            player.getPersistentDataContainer().remove(returnLocationKey);
        }
    }

    private void scheduleNextRandomSpawn() {
        if (!enabled) {
            return;
        }
        if (randomSpawnTask != null) {
            randomSpawnTask.cancel();
        }

        int minSeconds = plugin.getConfig().getInt(CONFIG_ROOT + ".spawn.minSeconds", DEFAULT_MIN_SECONDS);
        int maxSeconds = plugin.getConfig().getInt(CONFIG_ROOT + ".spawn.maxSeconds", DEFAULT_MAX_SECONDS);
        boolean enabled = plugin.getConfig().getBoolean(CONFIG_ROOT + ".spawn.enabled", true);

        if (!enabled || minSeconds <= 0 || maxSeconds <= 0) {
            plugin.getLogger().info("World boss random spawn disabled or invalid configuration.");
            return;
        }

        if (maxSeconds < minSeconds) {
            int swap = minSeconds;
            minSeconds = maxSeconds;
            maxSeconds = swap;
        }

        int delaySeconds = minSeconds + random.nextInt(maxSeconds - minSeconds + 1);
        randomSpawnTask = new BukkitRunnable() {
            private int remainingSeconds = delaySeconds;

            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    attemptRandomSpawn();
                    cancel();
                    scheduleNextRandomSpawn();
                    return;
                }
                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void scheduleBloodMoonChecks() {
        if (!enabled) {
            return;
        }
        if (bloodMoonCheckTask != null) {
            bloodMoonCheckTask.cancel();
        }
        if (!isTriggerEnabled(TriggerType.BLOOD_MOON)) {
            return;
        }
        bloodMoonCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                boolean active = bloodMoonManager != null && bloodMoonManager.isActive();
                if (active && !lastBloodMoonActive) {
                    Player targetPlayer = pickRandomEligiblePlayer();
                    if (targetPlayer == null) {
                        plugin.getLogger().warning("Blood moon world boss spawn skipped because no eligible players were online.");
                    } else {
                        Location base = findSpawnLocation(targetPlayer.getWorld(), targetPlayer.getLocation(), BLOOD_MOON_SPAWN_RADIUS, MAX_LOCATION_ATTEMPTS);
                        if (base == null) {
                            plugin.getLogger().warning("Blood moon world boss spawn skipped because no eligible location was found near " + targetPlayer.getName() + ".");
                            return;
                        }
                        if (trySpawnTriggeredBoss(targetPlayer.getWorld(), base, TriggerType.BLOOD_MOON)) {
                            createPortalAt(base);
                            logSpawnDetails("Blood moon world boss portal spawned near " + targetPlayer.getName(), targetPlayer.getWorld(), base);
                        }
                    }
                }
                lastBloodMoonActive = active;
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    private void startBossTick() {
        if (bossTickTask != null) {
            return;
        }
        bossTickTask = new BukkitRunnable() {
            @Override
            public void run() {
                activeBosses.values().removeIf(controller -> {
                    LivingEntity boss = controller.getBoss();
                    if (boss == null || boss.isDead() || !boss.isValid()) {
                        controller.cleanup();
                        return true;
                    }
                    controller.tick();
                    if (antiCheese.tick(controller)) {
                        return true;
                    }
                    return false;
                });
                tickBeaconAndCompass();
                spawnPortalParticles();
                markArenaForDeletionIfEmpty();
                checkArenaForDeletion();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void attemptRandomSpawn() {
        List<World> worlds = getEligibleWorlds();
        if (worlds.isEmpty()) {
            plugin.getLogger().warning("World boss spawn skipped because no worlds are configured.");
            return;
        }
        World world = worlds.get(random.nextInt(worlds.size()));
        if (!spawnWorldBoss(world, pickRandomBaseLocation(world))) {
            plugin.getLogger().warning("World boss random spawn failed to find a valid location.");
        }
    }

    private boolean trySpawnTriggeredBoss(World world, Location origin, TriggerType triggerType) {
        if (!isTriggerOffCooldown(world, triggerType)) {
            return false;
        }
        Location spawnLocation = resolveTriggeredSpawnLocation(world, origin);
        if (spawnLocation == null) {
            plugin.getLogger().warning("World boss trigger skipped for " + triggerType.name().toLowerCase(Locale.ROOT)
                    + " because no eligible location was found.");
            return false;
        }
        if (spawnWorldBoss(world, spawnLocation)) {
            setTriggeredNow(world, triggerType);
            return true;
        } else {
            plugin.getLogger().warning("World boss trigger failed for " + triggerType.name().toLowerCase(Locale.ROOT) + ".");
        }
        return false;
    }

    private Location resolveTriggeredSpawnLocation(World world, Location origin) {
        if (origin == null) {
            return pickRandomBaseLocation(world);
        }
        Biome originBiome = world.getBiome(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
        if (isBiomeEligible(originBiome)) {
            return origin;
        }
        return findSpawnLocation(world, origin, DEFAULT_FALLBACK_RADIUS, MAX_LOCATION_ATTEMPTS);
    }

    private boolean spawnWorldBoss(World world, Location origin) {
        Location base = origin != null ? origin : pickRandomBaseLocation(world);
        if (base == null) {
            return false;
        }

        Biome baseBiome = world.getBiome(base.getBlockX(), base.getBlockY(), base.getBlockZ());
        if (!isBiomeEligible(baseBiome)) {
            return false;
        }

        WorldBossType bossType = resolveBossType(baseBiome)
                .orElseGet(() -> getFallbackBossType().orElse(null));
        if (bossType == null) {
            return false;
        }

        return spawnBossInArena(world, base, bossType);
    }

    private boolean spawnBossInArena(World sourceWorld, Location base, WorldBossType bossType) {
        BossSettings settings = getBossSettings(bossType);
        World arenaWorld = resolveArenaWorld();
        if (arenaWorld == null) {
            plugin.getLogger().warning("World boss arena world could not be created.");
            return false;
        }
        bossDefeated = false;
        arenaMarkedForDeletion = false;
        markedArenaWorldName = null;
        prepareArena(arenaWorld);
        Location spawnLocation = findArenaSpawnLocation(arenaWorld, settings.spawnRadius);
        if (spawnLocation == null) {
            return false;
        }

        if (!(arenaWorld.spawnEntity(spawnLocation, settings.entityType) instanceof LivingEntity bossEntity)) {
            return false;
        }

        bossEntity.setCustomName(bossType.getConfigKey());
        bossEntity.setCustomNameVisible(true);
        bossEntity.getPersistentDataContainer().set(bossTypeKey, PersistentDataType.STRING, bossType.getConfigKey());

        WorldBossController controller = createController(bossType, bossEntity);
        if (controller != null) {
            controller.rebuildFromPersistent();
            activeBosses.put(bossEntity.getUniqueId(), controller);
        }

        if (settings.despawnTimeoutSeconds > 0) {
            long timeoutTicks = Duration.ofSeconds(settings.despawnTimeoutSeconds).toMillis() / 50L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!bossEntity.isDead() && bossEntity.isValid()) {
                    bossEntity.remove();
                }
            }, timeoutTicks);
        }

        Bukkit.broadcastMessage("⚔ " + bossType.getConfigKey() + " rises from the shadows!");
        broadcastSpawnBiome(sourceWorld, base);
        return true;
    }

    private World resolveArenaWorld() {
        String worldName = plugin.getConfig().getString(CONFIG_ROOT + ".arena.worldName", "world_boss_arena");
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            return existing;
        }
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        return creator.createWorld();
    }

    private World getLoadedArenaWorld() {
        String worldName = plugin.getConfig().getString(CONFIG_ROOT + ".arena.worldName", "world_boss_arena");
        return Bukkit.getWorld(worldName);
    }

    private void prepareArena(World world) {
        int radius = Math.max(8, plugin.getConfig().getInt(CONFIG_ROOT + ".arena.radius", DEFAULT_ARENA_RADIUS));
        int wallHeight = Math.max(2, plugin.getConfig().getInt(CONFIG_ROOT + ".arena.wallHeight", DEFAULT_ARENA_WALL_HEIGHT));
        String floorMaterialName = plugin.getConfig().getString(CONFIG_ROOT + ".arena.floorMaterial", "STONE");
        String wallMaterialName = plugin.getConfig().getString(CONFIG_ROOT + ".arena.wallMaterial", "GLASS");
        Material floorMaterial = resolveMaterial(floorMaterialName, Material.STONE);
        Material wallMaterial = resolveMaterial(wallMaterialName, Material.GLASS);

        Location center = world.getSpawnLocation().clone();
        int baseY = Math.max(world.getMinHeight() + 2, center.getBlockY());
        center.setY(baseY);
        world.setSpawnLocation(center);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location floorLocation = center.clone().add(x, -1, z);
                floorLocation.getBlock().setType(floorMaterial);
                for (int y = 0; y <= wallHeight; y++) {
                    Location clear = center.clone().add(x, y, z);
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        clear.getBlock().setType(wallMaterial);
                    } else if (y > 0) {
                        clear.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        int roofHeight = wallHeight + 1;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location ceilingLocation = center.clone().add(x, roofHeight, z);
                ceilingLocation.getBlock().setType(wallMaterial);
            }
        }
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setCenter(center);
        worldBorder.setSize(radius * 2.0);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        createPortal(world, world.getSpawnLocation());
        createEscapePoint(world, world.getSpawnLocation());
    }

    private Location findArenaSpawnLocation(World world, double spawnRadius) {
        Location center = world.getSpawnLocation().clone();
        center.setY(Math.max(world.getMinHeight() + 2, center.getBlockY()));
        double radius = Math.max(2.0, Math.min(spawnRadius, getArenaRadius(world) - 2.0));
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(random.nextDouble()) * radius;
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        return new Location(world, x + 0.5, center.getY() + 1.0, z + 0.5);
    }

    private int getArenaRadius(World world) {
        return Math.max(8, plugin.getConfig().getInt(CONFIG_ROOT + ".arena.radius", DEFAULT_ARENA_RADIUS));
    }

    private Material resolveMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private Material resolvePortalInnerMaterial(String name) {
        return Material.AIR;
    }

    private void prepareInstancePortals() {
        List<World> worlds = getEligibleWorlds();
        for (World world : worlds) {
            createPortal(world, world.getSpawnLocation());
        }
    }

    private void createPortal(World world, Location origin) {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".arena.portal.enabled", true)) {
            return;
        }
        String frameMaterialName = plugin.getConfig().getString(CONFIG_ROOT + ".arena.portal.frameMaterial", "GLOWSTONE");
        String innerMaterialName = plugin.getConfig().getString(CONFIG_ROOT + ".arena.portal.innerMaterial", "PARTICLE");
        Material frameMaterial = resolveMaterial(frameMaterialName, Material.GLOWSTONE);
        Material innerMaterial = resolvePortalInnerMaterial(innerMaterialName);

        Location base = origin.clone().add(2, 0, 2);
        int baseY = resolvePortalBaseY(world, base);
        base.setY(baseY);

        Set<Location> portalSet = portalBlocks.computeIfAbsent(world, ignored -> new HashSet<>());

        for (int y = 0; y < PORTAL_HEIGHT; y++) {
            for (int x = 0; x < PORTAL_WIDTH; x++) {
                Location target = base.clone().add(x, y, 0);
                boolean frame = y == 0 || y == PORTAL_HEIGHT - 1 || x == 0 || x == PORTAL_WIDTH - 1;
                if (frame) {
                    target.getBlock().setType(frameMaterial);
                } else {
                    target.getBlock().setType(innerMaterial);
                    portalSet.add(target.getBlock().getLocation());
                }
            }
        }

        Location labelLocation = base.clone().add(1, PORTAL_HEIGHT, 0);
        labelLocation.getBlock().setType(Material.LANTERN);
    }

    private int resolvePortalBaseY(World world, Location base) {
        int baseY = Math.max(world.getMinHeight() + 2, base.getBlockY());
        Block baseBlock = world.getBlockAt(base.getBlockX(), base.getBlockY(), base.getBlockZ());
        Block below = baseBlock.getRelative(BlockFace.DOWN);
        if (baseBlock.getType().isAir() && below.getType().isSolid()) {
            baseY = Math.max(world.getMinHeight() + 2, below.getY());
        }
        return baseY;
    }

    private void spawnPortalParticles() {
        if (portalBlocks.isEmpty()) {
            return;
        }
        for (Map.Entry<World, Set<Location>> entry : portalBlocks.entrySet()) {
            World world = entry.getKey();
            if (world == null) {
                continue;
            }
            for (Location location : entry.getValue()) {
                Location particleLocation = location.clone().add(0.5, 0.5, 0.5);
                world.spawnParticle(Particle.END_ROD, particleLocation, 2, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    private void createEscapePoint(World world, Location origin) {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".arena.escape.enabled", true)) {
            return;
        }
        String blockName = plugin.getConfig().getString(CONFIG_ROOT + ".arena.escape.blockMaterial", "EMERALD_BLOCK");
        Material blockMaterial = resolveMaterial(blockName, Material.EMERALD_BLOCK);
        Location base = origin.clone().add(-2, 0, -2);
        int baseY = Math.max(world.getMinHeight() + 2, base.getBlockY());
        base.setY(baseY);
        Location blockLocation = base.getBlock().getLocation();
        blockLocation.getBlock().setType(blockMaterial);
        escapeBlocks.add(blockLocation);
    }

    private void removeAllPortals() {
        for (Set<Location> locations : portalBlocks.values()) {
            for (Location location : locations) {
                location.getBlock().setType(Material.AIR);
            }
        }
        for (Location location : escapeBlocks) {
            location.getBlock().setType(Material.AIR);
        }
        portalBlocks.clear();
        escapeBlocks.clear();
    }

    private boolean matchesPortalAt(World world, int baseX, int baseY, int baseZ, Material frameMaterial, Material innerMaterial) {
        for (int y = 0; y < PORTAL_HEIGHT; y++) {
            for (int x = 0; x < PORTAL_WIDTH; x++) {
                boolean isFrame = y == 0 || y == PORTAL_HEIGHT - 1 || x == 0 || x == PORTAL_WIDTH - 1;
                Material expected = isFrame ? frameMaterial : innerMaterial;
                if (world.getBlockAt(baseX + x, baseY + y, baseZ).getType() != expected) {
                    return false;
                }
            }
        }
        return true;
    }

    private PortalPurgeResult removePortalAt(World world, int baseX, int baseY, int baseZ, Material frameMaterial, Material innerMaterial) {
        int frameRemoved = 0;
        int innerRemoved = 0;
        Set<Location> portalSet = portalBlocks.get(world);

        for (int y = 0; y < PORTAL_HEIGHT; y++) {
            for (int x = 0; x < PORTAL_WIDTH; x++) {
                boolean isFrame = y == 0 || y == PORTAL_HEIGHT - 1 || x == 0 || x == PORTAL_WIDTH - 1;
                Block block = world.getBlockAt(baseX + x, baseY + y, baseZ);
                if (isFrame && block.getType() == frameMaterial) {
                    block.setType(Material.AIR);
                    frameRemoved++;
                } else if (!isFrame && block.getType() == innerMaterial) {
                    block.setType(Material.AIR);
                    innerRemoved++;
                    if (portalSet != null) {
                        portalSet.remove(block.getLocation());
                    }
                }
            }
        }

        return new PortalPurgeResult(frameRemoved, innerRemoved, 0);
    }

    public static class PortalPurgeResult {
        private final int frameRemoved;
        private final int innerRemoved;
        private final int escapeRemoved;

        public PortalPurgeResult(int frameRemoved, int innerRemoved, int escapeRemoved) {
            this.frameRemoved = frameRemoved;
            this.innerRemoved = innerRemoved;
            this.escapeRemoved = escapeRemoved;
        }

        public int frameRemoved() {
            return frameRemoved;
        }

        public int innerRemoved() {
            return innerRemoved;
        }

        public int escapeRemoved() {
            return escapeRemoved;
        }
    }

    private void markArenaForDeletionIfEmpty() {
        if (!activeBosses.isEmpty()) {
            return;
        }
        World arenaWorld = getLoadedArenaWorld();
        if (arenaWorld == null) {
            return;
        }
        arenaMarkedForDeletion = true;
        markedArenaWorldName = arenaWorld.getName();
    }

    private void checkArenaForDeletion() {
        if (!arenaMarkedForDeletion) {
            return;
        }
        World arenaWorld = markedArenaWorldName == null ? null : Bukkit.getWorld(markedArenaWorldName);
        if (arenaWorld == null) {
            arenaMarkedForDeletion = false;
            markedArenaWorldName = null;
            return;
        }
        if (!arenaWorld.getPlayers().isEmpty()) {
            return;
        }
        cleanupBossesInArena(arenaWorld);
        removeAllPortals();
        Bukkit.unloadWorld(arenaWorld, false);
        deleteWorldFolder(arenaWorld.getWorldFolder());
        arenaMarkedForDeletion = false;
        markedArenaWorldName = null;
    }

    private void cleanupBossesInArena(World arenaWorld) {
        Iterator<Map.Entry<UUID, WorldBossController>> iterator = activeBosses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WorldBossController> entry = iterator.next();
            WorldBossController controller = entry.getValue();
            LivingEntity boss = controller.getBoss();
            if (boss == null || !boss.getWorld().equals(arenaWorld)) {
                continue;
            }
            if (boss.isValid()) {
                boss.remove();
            }
            controller.cleanup();
            antiCheese.clear(boss);
            iterator.remove();
        }
    }

    private boolean isBreakableMechanicBlock(Block block) {
        for (WorldBossController controller : activeBosses.values()) {
            if (controller.isBreakableMechanicBlock(block)) {
                return true;
            }
        }
        return false;
    }

    public boolean isArenaBlockProtected(Block block) {
        if (block == null) {
            return false;
        }
        World arenaWorld = getLoadedArenaWorld();
        if (arenaWorld == null || !arenaWorld.equals(block.getWorld())) {
            return false;
        }
        return !isBreakableMechanicBlock(block);
    }

    private void deleteWorldFolder(File folder) {
        if (folder == null || !folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteWorldFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    private void teleportViaPortal(Player player) {
        World arenaWorld = resolveArenaWorld();
        if (arenaWorld == null) {
            return;
        }
        World current = player.getWorld();
        World targetWorld;
        if (current.equals(arenaWorld)) {
            teleportPlayerToRespawn(player);
            if (bossDefeated) {
                removeAllPortals();
                markArenaForDeletionIfEmpty();
            }
            return;
        } else {
            targetWorld = arenaWorld;
        }
        player.teleport(targetWorld.getSpawnLocation().clone().add(0.5, 1.0, 0.5));
    }

    private void teleportToExit(Player player) {
        teleportPlayerToRespawn(player);
    }

    private void teleportPlayerToRespawn(Player player) {
        Location target = resolveReturnLocation(player);
        if (target == null) {
            return;
        }
        player.teleport(target);
    }

    private Location resolveReturnLocation(Player player) {
        Location respawnLocation = player.getRespawnLocation();
        if (respawnLocation != null && respawnLocation.getWorld() != null) {
            return respawnLocation;
        }
        World exitWorld = resolveExitWorld();
        if (exitWorld == null) {
            return null;
        }
        return exitWorld.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
    }

    private void storeReturnLocation(Player player, Location location) {
        String encoded = encodeLocation(location);
        player.getPersistentDataContainer().set(returnLocationKey, PersistentDataType.STRING, encoded);
    }

    private Location loadReturnLocation(Player player) {
        String encoded = player.getPersistentDataContainer().get(returnLocationKey, PersistentDataType.STRING);
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        return decodeLocation(encoded);
    }

    private String encodeLocation(Location location) {
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "world";
        return worldName + "|" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Location decodeLocation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] worldSplit = value.split("\\|");
        if (worldSplit.length != 2) {
            return null;
        }
        String worldName = worldSplit[0].trim();
        String[] coords = worldSplit[1].split(",");
        if (coords.length != 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());
            int z = Integer.parseInt(coords[2].trim());
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private World resolveExitWorld() {
        String exitName = plugin.getConfig().getString(CONFIG_ROOT + ".arena.portal.exitWorld", null);
        if (exitName != null) {
            World world = Bukkit.getWorld(exitName);
            if (world != null) {
                return world;
            }
        }
        List<World> worlds = getEligibleWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    private void tickBeaconAndCompass() {
        boolean beaconEnabled = plugin.getConfig().getBoolean(CONFIG_ROOT + ".beacon.enabled", true);
        boolean compassEnabled = plugin.getConfig().getBoolean(CONFIG_ROOT + ".compassTracking.enabled", true);
        if (!beaconEnabled && !compassEnabled) {
            return;
        }
        beaconTickCounter = (beaconTickCounter + 1) % 5;
        List<LivingEntity> bosses = activeBosses.values().stream()
                .map(WorldBossController::getBoss)
                .filter(entity -> entity != null && entity.isValid())
                .toList();
        if (bosses.isEmpty()) {
            return;
        }
        if (beaconEnabled && beaconTickCounter == 0) {
            double intensity = Math.max(0.5, plugin.getConfig().getDouble(CONFIG_ROOT + ".particleIntensity", 1.0));
            for (LivingEntity boss : bosses) {
                spawnBeaconParticles(boss, intensity);
            }
        }
        if (compassEnabled) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                LivingEntity nearest = findNearestBoss(player, bosses);
                if (nearest != null) {
                    player.setCompassTarget(nearest.getLocation());
                }
            }
        }
    }

    private void spawnBeaconParticles(LivingEntity boss, double intensity) {
        World world = boss.getWorld();
        Location base = boss.getLocation().clone().add(0, 1.0, 0);
        int maxHeight = world.getMaxHeight();
        int startY = Math.min(maxHeight - 1, base.getBlockY());
        int step = 5;
        int count = (int) Math.max(1, Math.round(4 * intensity));
        for (int y = startY; y < maxHeight; y += step) {
            Location point = new Location(world, base.getX(), y, base.getZ());
            world.spawnParticle(org.bukkit.Particle.END_ROD, point, count, 0.1, 0.2, 0.1, 0.01);
        }
    }

    private LivingEntity findNearestBoss(Player player, List<LivingEntity> bosses) {
        double closest = Double.MAX_VALUE;
        LivingEntity nearest = null;
        Location playerLocation = player.getLocation();
        for (LivingEntity boss : bosses) {
            if (!boss.getWorld().equals(playerLocation.getWorld())) {
                continue;
            }
            double distance = boss.getLocation().distanceSquared(playerLocation);
            if (distance < closest) {
                closest = distance;
                nearest = boss;
            }
        }
        return nearest;
    }

    private void broadcastSpawnBiome(World world, Location location) {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".broadcasts.enabled", true)) {
            return;
        }
        Biome biome = world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        String formattedBiome = biome.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String display = formattedBiome.isEmpty() ? "wilderness" : formattedBiome;
        Bukkit.broadcastMessage("The ground trembles\u2026 something ancient stirs in the " + display + ".");
    }

    private void logSpawnDetails(String prefix, World world, Location location) {
        Biome biome = world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        String formattedBiome = biome.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String display = formattedBiome.isEmpty() ? "wilderness" : formattedBiome;
        plugin.getLogger().info(prefix + " at [" + location.getBlockX() + ", " + location.getBlockY() + ", "
                + location.getBlockZ() + "] in " + display + " (" + world.getName() + ").");
    }

    private Player pickRandomEligiblePlayer() {
        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(player -> getEligibleWorlds().contains(player.getWorld()))
                .toList();
        if (players.isEmpty()) {
            return null;
        }
        return players.get(random.nextInt(players.size()));
    }

    private WorldBossController createController(WorldBossType type, LivingEntity bossEntity) {
        return switch (type) {
            case GRAVEWARDEN -> new GravewardenBoss(plugin, bossEntity);
            case STORM_HERALD -> new StormHeraldBoss(plugin, bossEntity);
            case HOLLOW_COLOSSUS -> new HollowColossusBoss(plugin, bossEntity);
        };
    }

    private Location pickRandomBaseLocation(World world) {
        WorldBorder border = world.getWorldBorder();
        double borderRadius = border.getSize() / 2.0;
        double minX = border.getCenter().getX() - borderRadius;
        double maxX = border.getCenter().getX() + borderRadius;
        double minZ = border.getCenter().getZ() - borderRadius;
        double maxZ = border.getCenter().getZ() + borderRadius;

        for (int attempt = 0; attempt < MAX_LOCATION_ATTEMPTS; attempt++) {
            double x = minX + random.nextDouble() * (maxX - minX);
            double z = minZ + random.nextDouble() * (maxZ - minZ);
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            int y = world.getHighestBlockYAt(blockX, blockZ);
            Location candidate = new Location(world, blockX + 0.5, y, blockZ + 0.5);
            if (border.isInside(candidate)) {
                return candidate;
            }
        }
        return world.getSpawnLocation();
    }

    private Location findSpawnLocation(World world, Location base, double radius, int attempts) {
        if (radius <= 0.0) {
            radius = DEFAULT_FALLBACK_RADIUS;
        }
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight() - 1;
        double searchRadius = radius;
        double maxRadius = world.getWorldBorder().getSize() / 2.0;
        while (searchRadius <= maxRadius) {
            for (int attempt = 0; attempt < attempts; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = Math.sqrt(random.nextDouble()) * searchRadius;
                double x = base.getX() + Math.cos(angle) * distance;
                double z = base.getZ() + Math.sin(angle) * distance;
                int blockX = (int) Math.floor(x);
                int blockZ = (int) Math.floor(z);
                int y = world.getHighestBlockYAt(blockX, blockZ);
                if (y < minHeight || y >= maxHeight) {
                    continue;
                }
                Material ground = world.getBlockAt(blockX, y, blockZ).getType();
                if (!ground.isSolid() || ground.isAir()) {
                    continue;
                }
                Material above = world.getBlockAt(blockX, y + 1, blockZ).getType();
                if (!above.isAir()) {
                    continue;
                }
                Location candidate = new Location(world, blockX + 0.5, y + 1, blockZ + 0.5);
                Biome candidateBiome = world.getBiome(candidate.getBlockX(), candidate.getBlockY(), candidate.getBlockZ());
                if (!isBiomeEligible(candidateBiome)) {
                    continue;
                }
                return candidate;
            }
            searchRadius += 1000.0;
        }
        return null;
    }

    private Optional<WorldBossType> resolveBossType(Biome biome) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(CONFIG_ROOT + ".biomeBossMap");
        if (section == null) {
            return Optional.empty();
        }
        for (String key : section.getKeys(false)) {
            Optional<Biome> mappedBiome = resolveBiome(key);
            if (mappedBiome.isPresent() && mappedBiome.get() == biome) {
                String bossName = section.getString(key);
                return WorldBossType.fromConfigKey(bossName);
            }
        }
        return Optional.empty();
    }

    private Optional<WorldBossType> getFallbackBossType() {
        ConfigurationSection bossesSection = plugin.getConfig().getConfigurationSection(CONFIG_ROOT + ".bosses");
        if (bossesSection == null) {
            return Optional.empty();
        }
        for (String key : bossesSection.getKeys(false)) {
            Optional<WorldBossType> type = WorldBossType.fromConfigKey(key);
            if (type.isPresent()) {
                return type;
            }
        }
        return Optional.empty();
    }

    private BossSettings getBossSettings(WorldBossType type) {
        String basePath = CONFIG_ROOT + ".bosses." + type.getConfigKey();
        String entityName = plugin.getConfig().getString(basePath + ".entityType", "WARDEN");
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(entityName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown entity type " + entityName + " for world boss " + type.getConfigKey());
            entityType = EntityType.WARDEN;
        }
        double spawnRadius = plugin.getConfig().getDouble(basePath + ".spawnRadius", DEFAULT_FALLBACK_RADIUS);
        int despawnTimeoutSeconds = plugin.getConfig().getInt(basePath + ".despawnTimeoutSeconds", 600);
        return new BossSettings(entityType, spawnRadius, despawnTimeoutSeconds);
    }

    private boolean isBiomeEligible(Biome biome) {
        List<String> allowed = plugin.getConfig().getStringList(CONFIG_ROOT + ".eligibleBiomes");
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        for (String entry : allowed) {
            Optional<Biome> resolved = resolveBiome(entry);
            if (resolved.isPresent() && resolved.get() == biome) {
                return true;
            }
        }
        return false;
    }

    private Optional<Biome> resolveBiome(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Biome.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private List<World> getEligibleWorlds() {
        List<String> configuredWorlds = plugin.getConfig().getStringList(CONFIG_ROOT + ".worlds");
        if (configuredWorlds == null || configuredWorlds.isEmpty()) {
            return Bukkit.getWorlds();
        }
        List<World> worlds = new java.util.ArrayList<>();
        for (String worldName : configuredWorlds) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                worlds.add(world);
            }
        }
        return worlds;
    }

    private boolean isTriggerEnabled(TriggerType triggerType) {
        return plugin.getConfig().getBoolean(CONFIG_ROOT + ".triggers." + triggerType.configKey + ".enabled", true);
    }

    private long getTriggerCooldownSeconds(TriggerType triggerType) {
        return Math.max(0L, plugin.getConfig().getLong(CONFIG_ROOT + ".triggers." + triggerType.configKey + ".cooldownSeconds", 0L));
    }

    private boolean isTriggerOffCooldown(World world, TriggerType triggerType) {
        long cooldownSeconds = getTriggerCooldownSeconds(triggerType);
        if (cooldownSeconds <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        long lastTime = lastTriggerTimes.get(triggerType).getOrDefault(world.getUID(), 0L);
        return now - lastTime >= cooldownSeconds * 1000L;
    }

    private void setTriggeredNow(World world, TriggerType triggerType) {
        lastTriggerTimes.get(triggerType).put(world.getUID(), System.currentTimeMillis());
    }

    private Set<Material> getStructureBlocks() {
        List<String> entries = plugin.getConfig().getStringList(CONFIG_ROOT + ".triggers.structureInteraction.blocks");
        if (entries == null || entries.isEmpty()) {
            return Set.of();
        }
        Set<Material> materials = new HashSet<>();
        for (String entry : entries) {
            try {
                materials.add(Material.valueOf(entry.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown structure interaction material: " + entry);
            }
        }
        return materials;
    }

    private enum TriggerType {
        THUNDERSTORM("thunderstorm"),
        BLOOD_MOON("bloodMoon"),
        STRUCTURE_INTERACTION("structureInteraction");

        private final String configKey;

        TriggerType(String configKey) {
            this.configKey = configKey;
        }
    }

    private record BossSettings(EntityType entityType, double spawnRadius, int despawnTimeoutSeconds) {
    }
}
