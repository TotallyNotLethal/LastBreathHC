package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptainCombatListener implements Listener {

    private final LastBreathHC plugin;
    private final CaptainRegistry captainRegistry;
    private final KillerResolver killerResolver;
    private final CaptainEntityBinder captainEntityBinder;
    private final CaptainTraitService traitService;
    private final NemesisUI nemesisUI;
    private final NemesisProgressionService progressionService;
    private final CaptainStateMachine stateMachine;

    private final NamespacedKey captainUuidKey;
    private final NamespacedKey captainFlagKey;

    private final Map<UUID, UUID> entityToCaptainId = new ConcurrentHashMap<>();
    private final Set<EntityType> eligibleMobTypes;
    private final Duration attributionTimeout;
    private final long xpPerKill;
    private final double rivalryPerKill;

    public CaptainCombatListener(LastBreathHC plugin, CaptainRegistry captainRegistry, KillerResolver killerResolver, CaptainEntityBinder captainEntityBinder, CaptainTraitService traitService, NemesisUI nemesisUI, NemesisProgressionService progressionService) {
        this.plugin = plugin;
        this.captainRegistry = captainRegistry;
        this.killerResolver = killerResolver;
        this.captainEntityBinder = captainEntityBinder;
        this.traitService = traitService;
        this.nemesisUI = nemesisUI;
        this.progressionService = progressionService;
        this.stateMachine = new CaptainStateMachine();
        this.captainUuidKey = new NamespacedKey(plugin, "nemesis_captain_uuid");
        this.captainFlagKey = new NamespacedKey(plugin, "nemesis_captain");
        this.eligibleMobTypes = loadEligibleMobTypes(plugin.getConfig().getConfigurationSection("nemesis.eligibleMobTypes"));
        this.attributionTimeout = Duration.ofSeconds(Math.max(1L, plugin.getConfig().getLong("nemesis.recentHostileTimeoutSeconds", 20L)));
        this.xpPerKill = Math.max(1L, plugin.getConfig().getLong("nemesis.progression.xpPerVictim", 25L));
        this.rivalryPerKill = Math.max(0.0, plugin.getConfig().getDouble("nemesis.scores.rivalryPerVictim", 5.0));
        indexExistingCaptains();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        KillerResolver.ResolvedKiller resolvedKiller = killerResolver.resolve(victim, attributionTimeout);
        if (resolvedKiller == null || resolvedKiller.entity() == null) {
            return;
        }

        LivingEntity killer = resolvedKiller.entity();
        if (!isEligibleKiller(killer)) {
            return;
        }

        UUID captainUuid = resolveCaptainUuid(killer);
        if (captainUuid != null) {
            CaptainRecord existing = captainRegistry.getByCaptainUuid(captainUuid);
            if (existing != null) {
                CaptainRecord updated = progressionService.onPlayerKill(existing, victim.getUniqueId());
                captainRegistry.upsert(updated);
                captainEntityBinder.tryUpgradeInPlace(updated);
                Player nemesis = victim.getServer().getPlayer(updated.identity().nemesisOf());
                if (nemesis != null) {
                    nemesisUI.taunt(nemesis, updated, "You cannot escape me.");
                }
                return;
            }
        }

        CaptainRecord created = createCaptainRecord(killer, victim.getUniqueId());
        entityToCaptainId.put(killer.getUniqueId(), created.identity().captainUuid());
        stampCaptainPdc(killer, created.identity().captainUuid());
        captainEntityBinder.bind(killer, created);
        captainRegistry.upsert(created);
        nemesisUI.announceCaptainBirth(created, victim);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCaptainDamaged(EntityDamageByEntityEvent event) {
        traitService.onDamage(event);
        if (event.getEntity() instanceof LivingEntity living) {
            CaptainRecord record = captainEntityBinder.resolveCaptainRecord(living).orElse(null);
            if (record != null) {
                progressionService.onCaptainDamaged(living, record, event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCaptainAttack(EntityDamageByEntityEvent event) {
        traitService.onAttack(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetChange(EntityTargetLivingEntityEvent event) {
        traitService.onTargetChange(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMinionDeath(EntityDeathEvent event) {
        traitService.onMinionDeath(event);
    }

    private void indexExistingCaptains() {
        for (CaptainRecord record : captainRegistry.getAll()) {
            if (record.identity() == null || record.identity().captainUuid() == null) {
                continue;
            }
            UUID entityUuid = record.identity().spawnEntityUuid();
            if (entityUuid != null) {
                entityToCaptainId.put(entityUuid, record.identity().captainUuid());
            }
        }
    }

    private boolean isEligibleKiller(LivingEntity killer) {
        if (killer instanceof Player) {
            return false;
        }

        if (!eligibleMobTypes.isEmpty() && !eligibleMobTypes.contains(killer.getType())) {
            return false;
        }

        UUID existing = resolveCaptainUuid(killer);
        if (existing == null) {
            return true;
        }

        return captainRegistry.getByCaptainUuid(existing) != null;
    }

    private UUID resolveCaptainUuid(LivingEntity entity) {
        UUID mapped = entityToCaptainId.get(entity.getUniqueId());
        if (mapped != null) {
            return mapped;
        }

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String stored = pdc.get(captainUuidKey, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return null;
        }

        try {
            UUID parsed = UUID.fromString(stored);
            entityToCaptainId.put(entity.getUniqueId(), parsed);
            return parsed;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private CaptainRecord createCaptainRecord(LivingEntity killer, UUID victimUuid) {
        long now = System.currentTimeMillis();
        UUID captainUuid = UUID.randomUUID();
        Location location = killer.getLocation();

        CaptainRecord.Identity identity = new CaptainRecord.Identity(
                captainUuid,
                victimUuid,
                killer.getUniqueId(),
                now
        );

        CaptainRecord.Origin origin = new CaptainRecord.Origin(
                location.getWorld() == null ? "unknown" : location.getWorld().getName(),
                location.getChunk().getX(),
                location.getChunk().getZ(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getBlock().getBiome().name()
        );

        CaptainRecord.Victims victims = new CaptainRecord.Victims(List.of(victimUuid), 1, now);
        CaptainRecord.NemesisScores scores = new CaptainRecord.NemesisScores(1.0, rivalryPerKill, 0.5, 0.5);
        CaptainRecord.Progression progression = new CaptainRecord.Progression(1, xpPerKill, "COMMON");
        CaptainRecord.Naming naming = new CaptainRecord.Naming(killer.getType().name() + " Captain", "the Relentless", "Captain", killer.getType().name());
        CaptainRecord.Traits traits = traitService.selectInitialTraits(identity, killer, scores);
        int minionCount = Math.max(0, plugin.getConfig().getInt("nemesis.minions.defaultCount", 2));
        List<String> minionArchetypes = plugin.getConfig().getStringList("nemesis.minions.defaultArchetypes");
        CaptainRecord.MinionPack minionPack = new CaptainRecord.MinionPack("default", minionCount, minionArchetypes, plugin.getConfig().getDouble("nemesis.minions.reinforcementChance", 0.0));
        CaptainRecord.State createdState = stateMachine.onCreate(now);
        CaptainRecord.State state = stateMachine.onSpawn(createdState.lastSeenEpochMs());

        Map<String, Long> counters = new HashMap<>();
        counters.put("kills", 1L);
        counters.put("spawns", 1L);
        CaptainRecord.Telemetry telemetry = new CaptainRecord.Telemetry(now, now, 1, counters);

        return new CaptainRecord(identity, origin, victims, scores, progression, naming, traits, minionPack, state, telemetry);
    }

    private void stampCaptainPdc(Entity entity, UUID captainUuid) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(captainUuidKey, PersistentDataType.STRING, captainUuid.toString());
        pdc.set(captainFlagKey, PersistentDataType.BYTE, (byte) 1);
    }

    private Set<EntityType> loadEligibleMobTypes(ConfigurationSection section) {
        Set<EntityType> types = new HashSet<>();
        if (section == null) {
            return types;
        }

        for (String key : section.getKeys(false)) {
            if (!section.getBoolean(key, false)) {
                continue;
            }

            try {
                types.add(EntityType.valueOf(key.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown nemesis eligible mob type: " + key);
            }
        }
        return types;
    }
}
