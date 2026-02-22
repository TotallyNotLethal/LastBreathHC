package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MinionController implements Listener {
    public static final String MINION_SCOREBOARD_TAG = "LB_NEMESIS_MINION";

    private final LastBreathHC plugin;
    private final CaptainRegistry captainRegistry;
    private final CaptainEntityBinder captainEntityBinder;
    private final NemesisProgressionService progressionService;
    private final NamespacedKey captainIdKey;
    private final CaptainStateMachine stateMachine = new CaptainStateMachine();

    private final Map<UUID, Set<UUID>> captainToMinions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextRespawnAt = new ConcurrentHashMap<>();
    private BukkitTask task;

    private final long respawnCooldownMs;
    private final double leashDistance;
    private final double retreatHealthThreshold;
    private final String captainDeathOutcome;
    private final long minionXpPerDeath;
    private final double minionAngerPerDeath;

    public MinionController(LastBreathHC plugin, CaptainRegistry captainRegistry, CaptainEntityBinder captainEntityBinder, NemesisProgressionService progressionService) {
        this.plugin = plugin;
        this.captainRegistry = captainRegistry;
        this.captainEntityBinder = captainEntityBinder;
        this.progressionService = progressionService;
        this.captainIdKey = captainEntityBinder.getCaptainIdKey();
        this.respawnCooldownMs = Math.max(500L, plugin.getConfig().getLong("nemesis.minions.respawnCooldownMs", 5000L));
        this.leashDistance = Math.max(4.0, plugin.getConfig().getDouble("nemesis.minions.leashDistance", 24.0));
        this.retreatHealthThreshold = Math.max(0.05, Math.min(0.95, plugin.getConfig().getDouble("nemesis.minions.retreatHealthThreshold", 0.35)));
        this.captainDeathOutcome = plugin.getConfig().getString("nemesis.minions.captainDeathOutcome", "flee");
        this.minionXpPerDeath = Math.max(1L, plugin.getConfig().getLong("nemesis.minions.xpPerMinionDeath", 8L));
        this.minionAngerPerDeath = Math.max(0.0, plugin.getConfig().getDouble("nemesis.minions.angerPerMinionDeath", 1.5));
    }

    public void start() {
        stop();
        long period = Math.max(2L, plugin.getConfig().getLong("nemesis.minions.tickPeriodTicks", 10L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        captainToMinions.clear();
        nextRespawnAt.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity dead = event.getEntity();
        if (dead.getScoreboardTags().contains(CaptainEntityBinder.CAPTAIN_SCOREBOARD_TAG)) {
            UUID captainId = parseCaptainId(dead);
            if (captainId != null) {
                handleCaptainDeath(captainId);
            }
            return;
        }

        if (!dead.getScoreboardTags().contains(MINION_SCOREBOARD_TAG)) {
            return;
        }

        UUID captainId = parseCaptainId(dead);
        if (captainId == null) {
            return;
        }
        Set<UUID> minions = captainToMinions.getOrDefault(captainId, Set.of());
        minions.remove(dead.getUniqueId());
        nextRespawnAt.put(captainId, System.currentTimeMillis() + respawnCooldownMs);
        CaptainRecord record = captainRegistry.getByCaptainUuid(captainId);
        double scale = record != null && record.minionPack() != null ? Math.max(0.5, record.minionPack().reinforcementChance() + 0.5) : 1.0;
        progressionService.onMinionDeath(captainId, Math.round(minionXpPerDeath * scale), minionAngerPerDeath * scale);
    }

    private void tick() {
        cleanupOrphans();
        for (CaptainRecord record : captainRegistry.getAll()) {
            if (!isActiveCaptain(record)) {
                continue;
            }

            LivingEntity captain = resolveCaptainEntity(record);
            if (captain == null || !captain.isValid()) {
                continue;
            }

            UUID captainId = record.identity().captainId();
            Set<UUID> minionIds = captainToMinions.computeIfAbsent(captainId, ignored -> ConcurrentHashMap.newKeySet());
            minionIds.removeIf(id -> {
                Entity entity = plugin.getServer().getEntity(id);
                return !(entity instanceof LivingEntity living) || !living.isValid();
            });

            mirrorTargetAndRetreat(captain, minionIds);
            leashMinions(captain, minionIds);
            maintainMinionCap(record, captain, minionIds);
        }
    }

    private void maintainMinionCap(CaptainRecord record, LivingEntity captain, Set<UUID> minionIds) {
        CaptainRecord.MinionPack minionPack = record.minionPack();
        if (minionPack == null) {
            return;
        }

        int desired = Math.max(0, minionPack.minionCount());
        if (minionIds.size() >= desired) {
            return;
        }

        long now = System.currentTimeMillis();
        long allowedAt = nextRespawnAt.getOrDefault(record.identity().captainId(), 0L);
        if (now < allowedAt) {
            return;
        }

        List<String> archetypes = minionPack.minionArchetypes() == null ? List.of() : minionPack.minionArchetypes();
        int missing = desired - minionIds.size();
        for (int i = 0; i < missing; i++) {
            String archetype = archetypes.isEmpty() ? minionPack.packType() : archetypes.get(i % archetypes.size());
            LivingEntity minion = spawnMinion(captain, record.identity().captainId(), archetype);
            if (minion != null) {
                minionIds.add(minion.getUniqueId());
            }
        }
        nextRespawnAt.put(record.identity().captainId(), now + respawnCooldownMs);
    }

    private LivingEntity spawnMinion(LivingEntity captain, UUID captainId, String archetype) {
        EntityType type = resolveArchetype(archetype);
        Location spawn = captain.getLocation().clone().add((Math.random() - 0.5) * 4.0, 0, (Math.random() - 0.5) * 4.0);
        World world = captain.getWorld();
        Entity entity = world.spawnEntity(spawn, type);
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return null;
        }

        living.addScoreboardTag(MINION_SCOREBOARD_TAG);
        living.getPersistentDataContainer().set(captainIdKey, PersistentDataType.STRING, captainId.toString());
        living.customName(net.kyori.adventure.text.Component.text("§7Nemesis Minion"));
        living.setCustomNameVisible(false);

        if (living instanceof Mob minionMob && captain instanceof Mob captainMob && captainMob.getTarget() != null) {
            minionMob.setTarget(captainMob.getTarget());
        }

        return living;
    }

    private void mirrorTargetAndRetreat(LivingEntity captain, Set<UUID> minionIds) {
        boolean retreat = captain.getHealth() <= captain.getMaxHealth() * retreatHealthThreshold;
        LivingEntity captainTarget = (captain instanceof Mob mob) ? mob.getTarget() : null;

        for (UUID minionId : minionIds) {
            Entity entity = plugin.getServer().getEntity(minionId);
            if (!(entity instanceof Mob minion)) {
                continue;
            }

            if (retreat) {
                minion.setTarget(null);
                Location fallback = captain.getLocation().clone().add((Math.random() - 0.5) * 3.0, 0, (Math.random() - 0.5) * 3.0);
                if (minion.getLocation().distanceSquared(captain.getLocation()) > 64.0) {
                    minion.teleport(fallback);
                }
                continue;
            }

            if (captainTarget != null) {
                minion.setTarget(captainTarget);
            }
        }
    }

    private void leashMinions(LivingEntity captain, Set<UUID> minionIds) {
        double maxDistanceSq = leashDistance * leashDistance;
        for (UUID minionId : minionIds) {
            Entity entity = plugin.getServer().getEntity(minionId);
            if (!(entity instanceof LivingEntity minion)) {
                continue;
            }
            if (!minion.getWorld().equals(captain.getWorld())) {
                minion.teleport(captain.getLocation());
                continue;
            }
            if (minion.getLocation().distanceSquared(captain.getLocation()) > maxDistanceSq) {
                minion.teleport(captain.getLocation().clone().add(1.5, 0, 1.5));
            }
        }
    }

    private void handleCaptainDeath(UUID captainId) {
        CaptainRecord record = captainRegistry.getByCaptainUuid(captainId);
        if (record != null) {
            CaptainRecord.State deadState = stateMachine.onKilled(System.currentTimeMillis());
            captainRegistry.upsert(new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), record.naming(), record.traits(), record.minionPack(), deadState, record.telemetry()));
        }
        Set<UUID> minions = new HashSet<>(captainToMinions.getOrDefault(captainId, Set.of()));
        if (minions.isEmpty()) {
            captainToMinions.remove(captainId);
            return;
        }

        boolean enrage = "enrage".equalsIgnoreCase(captainDeathOutcome);
        for (UUID minionId : minions) {
            Entity entity = plugin.getServer().getEntity(minionId);
            if (!(entity instanceof LivingEntity minion)) {
                continue;
            }
            if (enrage) {
                if (minion instanceof Mob mob) {
                    Player nearest = nearestPlayer(minion.getLocation());
                    if (nearest != null) {
                        mob.setTarget(nearest);
                    }
                }
            } else {
                if (minion instanceof Mob mob) {
                    mob.setTarget(null);
                }
                minion.getPathfinder().moveTo(minion.getLocation().clone().add((Math.random() - 0.5) * 12, 0, (Math.random() - 0.5) * 12));
            }
        }

        captainToMinions.remove(captainId);
    }

    public int cleanupOrphansNow() {
        return cleanupOrphans();
    }

    private int cleanupOrphans() {
        int removed = 0;
        for (Map.Entry<UUID, Set<UUID>> entry : new ArrayList<>(captainToMinions.entrySet())) {
            UUID captainId = entry.getKey();
            if (captainRegistry.getByCaptainUuid(captainId) == null) {
                for (UUID minionId : entry.getValue()) {
                    Entity entity = plugin.getServer().getEntity(minionId);
                    if (entity != null && entity.isValid()) {
                        entity.remove();
                        removed++;
                    }
                }
                captainToMinions.remove(captainId);
            }
        }

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.getScoreboardTags().contains(MINION_SCOREBOARD_TAG)) {
                    continue;
                }
                UUID captainId = parseCaptainId(entity);
                if (captainId != null && captainRegistry.getByCaptainUuid(captainId) != null) {
                    captainToMinions.computeIfAbsent(captainId, ignored -> ConcurrentHashMap.newKeySet()).add(entity.getUniqueId());
                } else {
                    entity.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    private UUID parseCaptainId(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(captainIdKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private LivingEntity resolveCaptainEntity(CaptainRecord record) {
        LivingEntity living = captainEntityBinder.resolveLiveKillerEntity(record);
        if (living == null) {
            return null;
        }
        if (!living.getScoreboardTags().contains(CaptainEntityBinder.CAPTAIN_SCOREBOARD_TAG)) {
            return null;
        }
        return living;
    }

    private boolean isActiveCaptain(CaptainRecord record) {
        return record != null && record.state() != null && record.state().state() == CaptainState.ACTIVE;
    }

    private EntityType resolveArchetype(String archetype) {
        if (archetype == null || archetype.isBlank()) {
            return EntityType.ZOMBIE;
        }
        String normalized = archetype.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return EntityType.ZOMBIE;
        }
    }

    private Player nearestPlayer(Location location) {
        Player best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(location.getWorld())) {
                continue;
            }
            double dist = player.getLocation().distanceSquared(location);
            if (dist < bestDistanceSq) {
                bestDistanceSq = dist;
                best = player;
            }
        }
        return best;
    }
}
