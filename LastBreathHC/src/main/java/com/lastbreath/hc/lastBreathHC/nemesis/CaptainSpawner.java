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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CaptainSpawner implements Listener {

    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;
    private final ProtectedRegionChecker protectedRegionChecker;
    private final CaptainStateMachine stateMachine;

    private final long movementCheckCooldownMs;
    private final double movementInterestChance;
    private final double joinHuntChance;
    private final double deathHuntChance;
    private final long directorMinTickDelay;
    private final long directorMaxTickDelay;
    private final int attemptsPerTick;
    private final long interestDurationMs;
    private final int nearbyRangeBlocks;
    private final int activeCaptainCap;
    private final int perWorldPerMinuteLimit;

    private final Map<UUID, Long> playerSpawnInterest = new HashMap<>();
    private final Map<UUID, Long> playerMovementProbeCooldown = new HashMap<>();
    private final Map<UUID, Integer> perWorldSpawnCounter = new HashMap<>();
    private final Map<UUID, Long> perWorldSpawnWindowStart = new HashMap<>();
    private BukkitTask directorTask;

    public CaptainSpawner(LastBreathHC plugin,
                          CaptainRegistry registry,
                          CaptainEntityBinder binder,
                          ProtectedRegionChecker protectedRegionChecker) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.protectedRegionChecker = protectedRegionChecker;
        this.stateMachine = new CaptainStateMachine();

        this.movementCheckCooldownMs = Math.max(250L, plugin.getConfig().getLong("nemesis.spawn.interest.movementCheckCooldownMs", 2000L));
        this.movementInterestChance = clampChance(plugin.getConfig().getDouble("nemesis.spawn.interest.movementChance", 0.2));
        this.joinHuntChance = clampChance(plugin.getConfig().getDouble("nemesis.spawn.interest.joinChance", 0.2));
        this.deathHuntChance = clampChance(plugin.getConfig().getDouble("nemesis.spawn.interest.deathChance", 0.35));
        this.directorMinTickDelay = Math.max(20L, plugin.getConfig().getLong("nemesis.spawn.director.minTickDelay", 20L));
        this.directorMaxTickDelay = Math.max(this.directorMinTickDelay, plugin.getConfig().getLong("nemesis.spawn.director.maxTickDelay", 40L));
        this.attemptsPerTick = Math.max(1, plugin.getConfig().getInt("nemesis.spawn.director.attemptsPerTick", 2));
        this.interestDurationMs = Math.max(1_000L, plugin.getConfig().getLong("nemesis.spawn.director.interestDurationMs", 30_000L));
        this.nearbyRangeBlocks = Math.max(16, plugin.getConfig().getInt("nemesis.spawn.location.nearbyRangeBlocks", 96));
        this.activeCaptainCap = Math.max(1, plugin.getConfig().getInt("nemesis.spawn.world.activeCaptainCap", 20));
        this.perWorldPerMinuteLimit = Math.max(1, plugin.getConfig().getInt("nemesis.spawn.world.perMinuteAttemptCap", 3));
    }

    public void start() {
        stop();
        scheduleDirectorTick();
    }

    public void stop() {
        if (directorTask != null) {
            directorTask.cancel();
            directorTask = null;
        }
        playerSpawnInterest.clear();
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

        queueInterest(player, movementInterestChance);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        queueInterest(event.getPlayer(), joinHuntChance);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        queueInterest(event.getEntity(), deathHuntChance);
    }

    private void scheduleDirectorTick() {
        long delay = ThreadLocalRandom.current().nextLong(directorMinTickDelay, directorMaxTickDelay + 1);
        directorTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                runDirectorTick();
            } finally {
                if (directorTask != null) {
                    scheduleDirectorTick();
                }
            }
        }, delay);
    }

    private void runDirectorTick() {
        if (playerSpawnInterest.isEmpty() || Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Set<UUID> toRemove = new HashSet<>();
        List<Player> eligiblePlayers = new ArrayList<>(playerSpawnInterest.entrySet().stream()
                .filter(entry -> {
                    if (entry.getValue() < now) {
                        toRemove.add(entry.getKey());
                        return false;
                    }
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline() || player.getWorld() == null) {
                        toRemove.add(entry.getKey());
                        return false;
                    }
                    return canSpawnInWorld(player.getWorld());
                })
                .map(entry -> Bukkit.getPlayer(entry.getKey()))
                .filter(player -> player != null && player.isOnline() && player.getWorld() != null)
                .toList());

        toRemove.forEach(playerSpawnInterest::remove);
        if (eligiblePlayers.isEmpty()) {
            return;
        }

        for (int i = 0; i < attemptsPerTick && !eligiblePlayers.isEmpty(); i++) {
            int index = ThreadLocalRandom.current().nextInt(eligiblePlayers.size());
            Player chosen = eligiblePlayers.remove(index);
            boolean spawned = attemptSpawn(chosen, "director_tick");
            if (spawned) {
                playerSpawnInterest.remove(chosen.getUniqueId());
            }
        }
    }

    public boolean triggerHunt(Player nearPlayer, double chance, String reason) {
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return false;
        }
        return attemptSpawn(nearPlayer, reason);
    }

    private boolean attemptSpawn(Player nearPlayer, String reason) {
        if (nearPlayer == null || nearPlayer.getWorld() == null || !nearPlayer.isOnline()) {
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
        CaptainRecord spawned = withState(relocated, stateMachine.onSpawn(System.currentTimeMillis()));
        Optional<org.bukkit.entity.LivingEntity> bound = binder.spawnOrBind(spawned);
        if (bound.isEmpty()) {
            return false;
        }

        CaptainRecord persisted = withRuntimeEntity(spawned, bound.get().getUniqueId());
        registry.upsert(persisted);
        markSpawnInWorld(target.getWorld());
        announceSpawn(persisted, nearPlayer, reason);
        return true;
    }

    private void queueInterest(Player player, double chance) {
        if (player == null || player.getWorld() == null || !player.isOnline()) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        playerSpawnInterest.put(player.getUniqueId(), System.currentTimeMillis() + interestDurationMs);
    }


    public boolean forceSpawn(UUID captainId) {
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null || record.origin() == null) {
            return false;
        }
        CaptainRecord spawned = withState(record, stateMachine.onSpawn(System.currentTimeMillis()));
        Optional<org.bukkit.entity.LivingEntity> bound = binder.spawnOrBind(spawned);
        if (bound.isEmpty()) {
            return false;
        }
        markSpawnInWorld(bound.get().getWorld());
        registry.upsert(withRuntimeEntity(spawned, bound.get().getUniqueId()));
        return true;
    }

    public boolean retireCaptain(UUID captainId) {
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null || record.state() == null || record.state().state() != CaptainState.ACTIVE) {
            return false;
        }
        org.bukkit.entity.LivingEntity entity = binder.resolveLiveKillerEntity(record);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        CaptainRecord.State retired = stateMachine.onRetire(System.currentTimeMillis());
        registry.upsert(new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), record.naming(), record.traits(), record.minionPack(), retired, record.telemetry()));
        return true;
    }

    public String debugSummary() {
        return "records=" + registry.getAll().size()
                + ", worldsTracked=" + perWorldSpawnCounter.size()
                + ", interestedPlayers=" + playerSpawnInterest.size();
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
        return record.state().state() == CaptainState.DORMANT;
    }

    private boolean isNearNemesisPlayer(CaptainRecord record, Player nearPlayer) {
        UUID nemesis = record.identity() == null ? null : record.identity().nemesisOf();
        if (nemesis == null) {
            return false;
        }
        return nemesis.equals(nearPlayer.getUniqueId());
    }

    private boolean isOnCooldown(CaptainRecord record, long now) {
        if (record.state() == null) {
            return false;
        }
        if (record.state().state() != CaptainState.COOLDOWN) {
            return false;
        }
        if (now >= record.state().cooldownUntilEpochMs()) {
            CaptainRecord.State dormant = stateMachine.onCooldownElapsed(now);
            registry.upsert(withState(record, dormant));
            return false;
        }
        return true;
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
        List<String> deniedBiomes = plugin.getConfig().getStringList("nemesis.spawn.location.deniedBiomes");
        for (String denied : deniedBiomes) {
            if (biome.equals(denied.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        int light = location.getBlock().getLightLevel();
        int minLight = plugin.getConfig().getInt("nemesis.spawn.location.minLight", 0);
        int maxLight = plugin.getConfig().getInt("nemesis.spawn.location.maxLight", 15);
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

        return registry.getActiveByWorld(world.getName()).size() < activeCaptainCap;
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

    private CaptainRecord withState(CaptainRecord record, CaptainRecord.State state) {
        return new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), record.naming(), record.traits(), record.minionPack(), state, record.telemetry());
    }

    private CaptainRecord withRuntimeEntity(CaptainRecord record, UUID runtimeEntityUuid) {
        if (record.state() == null) {
            return record;
        }
        CaptainRecord.State state = new CaptainRecord.State(record.state().state(), record.state().cooldownUntilEpochMs(), record.state().lastSeenEpochMs(), runtimeEntityUuid);
        return withState(record, state);
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
