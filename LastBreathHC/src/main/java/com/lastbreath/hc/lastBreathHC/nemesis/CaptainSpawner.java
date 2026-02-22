package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CaptainSpawner implements Listener {

    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;
    private final ProtectedRegionChecker protectedRegionChecker;

    private final long movementCheckCooldownMs;
    private final double joinHuntChance;
    private final double deathHuntChance;
    private final double worldTickSpawnChance;
    private final int nearbyRangeBlocks;
    private final int activeCaptainCap;
    private final int perWorldPerMinuteLimit;
    private final long recordCooldownMs;

    private final Map<UUID, Long> playerMovementProbeCooldown = new HashMap<>();
    private final Map<UUID, Integer> perWorldSpawnCounter = new HashMap<>();
    private final Map<UUID, Long> perWorldSpawnWindowStart = new HashMap<>();
    private BukkitTask worldTickTask;

    public CaptainSpawner(LastBreathHC plugin,
                          CaptainRegistry registry,
                          CaptainEntityBinder binder,
                          ProtectedRegionChecker protectedRegionChecker) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.protectedRegionChecker = protectedRegionChecker;

        this.movementCheckCooldownMs = Math.max(250L, plugin.getConfig().getLong("nemesis.spawner.movementCheckCooldownMs", 2000L));
        this.joinHuntChance = clampChance(plugin.getConfig().getDouble("nemesis.spawner.joinHuntChance", 0.2));
        this.deathHuntChance = clampChance(plugin.getConfig().getDouble("nemesis.spawner.deathHuntChance", 0.35));
        this.worldTickSpawnChance = clampChance(plugin.getConfig().getDouble("nemesis.spawner.worldTickSpawnChance", 0.001));
        this.nearbyRangeBlocks = Math.max(16, plugin.getConfig().getInt("nemesis.spawner.nearbyRangeBlocks", 96));
        this.activeCaptainCap = Math.max(1, plugin.getConfig().getInt("nemesis.spawner.activeCaptainCap", 20));
        this.perWorldPerMinuteLimit = Math.max(1, plugin.getConfig().getInt("nemesis.spawner.perWorldPerMinuteLimit", 3));
        this.recordCooldownMs = Math.max(1000L, plugin.getConfig().getLong("nemesis.spawner.recordCooldownMs", 30_000L));
    }

    public void start() {
        stop();
        long periodTicks = Math.max(20L, plugin.getConfig().getLong("nemesis.spawner.worldTickPeriodTicks", 200L));
        worldTickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runWorldTickProbe, periodTicks, periodTicks);
    }

    public void stop() {
        if (worldTickTask != null) {
            worldTickTask.cancel();
            worldTickTask = null;
        }
        playerMovementProbeCooldown.clear();
        perWorldSpawnCounter.clear();
        perWorldSpawnWindowStart.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long nextAllowed = playerMovementProbeCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now < nextAllowed) {
            return;
        }
        playerMovementProbeCooldown.put(player.getUniqueId(), now + movementCheckCooldownMs);

        triggerHunt(player, 0.2, "movement");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        triggerHunt(event.getPlayer(), joinHuntChance, "join");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        triggerHunt(event.getEntity(), deathHuntChance, "death");
    }

    private void runWorldTickProbe() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() > worldTickSpawnChance) {
            return;
        }

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        Player chosen = online.get(ThreadLocalRandom.current().nextInt(online.size()));
        triggerHunt(chosen, 1.0, "world_tick");
    }

    public boolean triggerHunt(Player nearPlayer, double chance, String reason) {
        if (nearPlayer == null || nearPlayer.getWorld() == null || !nearPlayer.isOnline()) {
            return false;
        }

        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return false;
        }

        if (!canSpawnInWorld(nearPlayer.getWorld())) {
            return false;
        }

        Optional<CaptainRecord> candidateOpt = selectCandidate(nearPlayer);
        if (candidateOpt.isEmpty()) {
            return false;
        }

        CaptainRecord candidate = candidateOpt.get();
        Location target = findValidSpawnLocation(candidate, nearPlayer);
        if (target == null) {
            return false;
        }

        CaptainRecord relocated = withLocationAndSeen(candidate, target);
        Optional<org.bukkit.entity.LivingEntity> bound = binder.spawnOrBind(relocated);
        if (bound.isEmpty()) {
            return false;
        }

        registry.upsert(relocated);
        markSpawnInWorld(target.getWorld());
        announceSpawn(relocated, nearPlayer, reason);
        return true;
    }


    public boolean forceSpawn(UUID captainId) {
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null || record.origin() == null) {
            return false;
        }
        Optional<org.bukkit.entity.LivingEntity> bound = binder.spawnOrBind(record);
        if (bound.isEmpty()) {
            return false;
        }
        markSpawnInWorld(bound.get().getWorld());
        return true;
    }

    public boolean retireCaptain(UUID captainId) {
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null || record.state() == null || !record.state().active()) {
            return false;
        }
        org.bukkit.entity.Entity entity = record.identity() == null ? null : Bukkit.getEntity(record.identity().spawnEntityUuid());
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        CaptainRecord.State retired = new CaptainRecord.State("RETIRED", false, record.state().spawnedAtEpochMillis(), System.currentTimeMillis());
        registry.upsert(new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), record.naming(), record.traits(), record.minionPack(), retired, record.telemetry()));
        return true;
    }

    public String debugSummary() {
        return "records=" + registry.getAll().size() + ", worldsTracked=" + perWorldSpawnCounter.size();
    }
    private Optional<CaptainRecord> selectCandidate(Player nearPlayer) {
        long now = System.currentTimeMillis();
        return registry.getAll().stream()
                .filter(this::isAliveRecord)
                .filter(record -> isNearNemesisPlayer(record, nearPlayer))
                .filter(record -> !isOnCooldown(record, now))
                .max(Comparator.comparingDouble(this::hateScore));
    }

    private boolean isAliveRecord(CaptainRecord record) {
        if (record == null || record.state() == null) {
            return false;
        }
        return record.state().active() && !"dead".equalsIgnoreCase(record.state().status());
    }

    private boolean isNearNemesisPlayer(CaptainRecord record, Player nearPlayer) {
        UUID nemesis = record.identity() == null ? null : record.identity().nemesisOf();
        if (nemesis == null) {
            return false;
        }
        return nemesis.equals(nearPlayer.getUniqueId());
    }

    private boolean isOnCooldown(CaptainRecord record, long now) {
        if (record.telemetry() == null) {
            return false;
        }
        long lastSeen = record.telemetry().lastSeenAtEpochMillis();
        return lastSeen > 0 && now - lastSeen < recordCooldownMs;
    }

    private double hateScore(CaptainRecord record) {
        if (record.nemesisScores() == null) {
            return 0.0;
        }
        return record.nemesisScores().rivalry() + record.nemesisScores().threat();
    }

    private Location findValidSpawnLocation(CaptainRecord record, Player nearPlayer) {
        World world = nearPlayer.getWorld();
        int radius = nearbyRangeBlocks;
        for (int i = 0; i < 8; i++) {
            int dx = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int dz = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int x = nearPlayer.getLocation().getBlockX() + dx;
            int z = nearPlayer.getLocation().getBlockZ() + dz;
            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            if (!isSpawnLocationValid(candidate)) {
                continue;
            }
            if (!protectedRegionChecker.canSpawnCaptain(candidate, record)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private boolean isSpawnLocationValid(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        World world = location.getWorld();
        int y = location.getBlockY();
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) {
            return false;
        }

        String biome = location.getBlock().getBiome().name().toLowerCase(Locale.ROOT);
        List<String> deniedBiomes = plugin.getConfig().getStringList("nemesis.spawner.deniedBiomes");
        for (String denied : deniedBiomes) {
            if (biome.equals(denied.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        int light = location.getBlock().getLightLevel();
        int minLight = plugin.getConfig().getInt("nemesis.spawner.minLight", 0);
        int maxLight = plugin.getConfig().getInt("nemesis.spawner.maxLight", 15);
        return light >= minLight && light <= maxLight;
    }

    private boolean canSpawnInWorld(World world) {
        if (world == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        UUID worldId = world.getUID();
        long windowStart = perWorldSpawnWindowStart.getOrDefault(worldId, now);
        if (now - windowStart >= 60_000L) {
            perWorldSpawnWindowStart.put(worldId, now);
            perWorldSpawnCounter.put(worldId, 0);
        }

        int worldSpawns = perWorldSpawnCounter.getOrDefault(worldId, 0);
        if (worldSpawns >= perWorldPerMinuteLimit) {
            return false;
        }

        long activeInWorld = world.getEntities().stream()
                .filter(e -> e.getScoreboardTags().contains(CaptainEntityBinder.CAPTAIN_SCOREBOARD_TAG))
                .count();
        return activeInWorld < activeCaptainCap;
    }

    private void markSpawnInWorld(World world) {
        if (world == null) {
            return;
        }
        UUID worldId = world.getUID();
        perWorldSpawnCounter.put(worldId, perWorldSpawnCounter.getOrDefault(worldId, 0) + 1);
        perWorldSpawnWindowStart.putIfAbsent(worldId, System.currentTimeMillis());
    }

    private CaptainRecord withLocationAndSeen(CaptainRecord record, Location location) {
        long now = System.currentTimeMillis();
        CaptainRecord.Origin updatedOrigin = new CaptainRecord.Origin(
                location.getWorld() == null ? "unknown" : location.getWorld().getName(),
                location.getChunk().getX(),
                location.getChunk().getZ(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getBlock().getBiome().name()
        );

        CaptainRecord.Telemetry telemetry = record.telemetry() == null
                ? new CaptainRecord.Telemetry(now, now, 1, Map.of())
                : new CaptainRecord.Telemetry(now, now, record.telemetry().encounters() + 1, record.telemetry().counters());

        return new CaptainRecord(
                record.identity(),
                updatedOrigin,
                record.victims(),
                record.nemesisScores(),
                record.progression(),
                record.naming(),
                record.traits(),
                record.minionPack(),
                record.state(),
                telemetry
        );
    }

    private void announceSpawn(CaptainRecord record, Player nearPlayer, String reason) {
        String name = record.naming() == null ? "Nemesis Captain" : record.naming().displayName();
        nearPlayer.sendMessage("§4⚔ Your nemesis captain has entered the hunt: §c" + name + " §7(" + reason + ")");
    }

    private double clampChance(double raw) {
        if (raw < 0.0) {
            return 0.0;
        }
        return Math.min(raw, 1.0);
    }

    public interface ProtectedRegionChecker {
        boolean canSpawnCaptain(Location location, CaptainRecord record);
    }

    public static class NoOpProtectedRegionChecker implements ProtectedRegionChecker {
        @Override
        public boolean canSpawnCaptain(Location location, CaptainRecord record) {
            return true;
        }
    }
}
