package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.worldboss.WorldBossConstants;
import com.lastbreath.hc.lastBreathHC.spawners.SpawnerTags;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private final CaptainTraitRegistry traitRegistry;
    private final NemesisUI nemesisUI;
    private final NemesisProgressionService progressionService;
    private final DeathOutcomeResolver deathOutcomeResolver;
    private final CaptainStateMachine stateMachine;
    private final CaptainNameGenerator nameGenerator;

    private final NamespacedKey captainUuidKey;
    private final NamespacedKey captainFlagKey;
    private final NamespacedKey worldBossTypeKey;

    private final Map<UUID, UUID> entityToCaptainId = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerFleeProbeCooldown = new ConcurrentHashMap<>();
    private final Set<EntityType> eligibleMobTypes;
    private final KillerResolver.AttributionTimeouts attributionTimeouts;
    private final long xpPerKill;
    private final double rivalryPerKill;
    private final double dedupeRadius;
    private final int creationThrottleMaxCaptains;
    private final long creationThrottleWindowMs;
    private final double lowHealthFleePromotionChance;
    private final long lowHealthFleeProbeCooldownMs;
    private final double playerEventRadiusCap;
    private final double playerEventRadiusBlocks;
    private final boolean defyDeathPromotionEnabled;
    private final int defyDeathStartingLevel;
    private final String defyDeathStartingTier;
    private final long defyDeathBonusXp;
    private final double defyDeathBonusRivalry;

    public CaptainCombatListener(LastBreathHC plugin, CaptainRegistry captainRegistry, KillerResolver killerResolver, CaptainEntityBinder captainEntityBinder, CaptainTraitService traitService, CaptainTraitRegistry traitRegistry, NemesisUI nemesisUI, NemesisProgressionService progressionService, DeathOutcomeResolver deathOutcomeResolver, CaptainNameGenerator nameGenerator) {
        this.plugin = plugin;
        this.captainRegistry = captainRegistry;
        this.killerResolver = killerResolver;
        this.captainEntityBinder = captainEntityBinder;
        this.traitService = traitService;
        this.traitRegistry = traitRegistry;
        this.nemesisUI = nemesisUI;
        this.progressionService = progressionService;
        this.deathOutcomeResolver = deathOutcomeResolver;
        this.stateMachine = new CaptainStateMachine();
        this.nameGenerator = nameGenerator;
        this.captainUuidKey = new NamespacedKey(plugin, "nemesis_captain_uuid");
        this.captainFlagKey = new NamespacedKey(plugin, "nemesis_captain");
        this.worldBossTypeKey = new NamespacedKey(plugin, WorldBossConstants.WORLD_BOSS_TYPE_KEY);
        this.eligibleMobTypes = loadEligibleMobTypes(plugin.getConfig().getConfigurationSection("nemesis.eligibleMobTypes"));
        this.attributionTimeouts = new KillerResolver.AttributionTimeouts(
                Duration.ofMillis(Math.max(500L, plugin.getConfig().getLong("nemesis.creation.timeoutMs.projectile", 20_000L))),
                Duration.ofMillis(Math.max(500L, plugin.getConfig().getLong("nemesis.creation.timeoutMs.knockback", 8_000L))),
                Duration.ofMillis(Math.max(500L, plugin.getConfig().getLong("nemesis.creation.timeoutMs.ambient", 15_000L)))
        );
        this.xpPerKill = Math.max(1L, plugin.getConfig().getLong("nemesis.progression.xpPerVictim", 25L));
        this.rivalryPerKill = Math.max(0.0, plugin.getConfig().getDouble("nemesis.scores.rivalryPerVictim", 5.0));
        this.dedupeRadius = Math.max(0.0, plugin.getConfig().getDouble("nemesis.creation.dedupeRadius", 48.0));
        this.creationThrottleMaxCaptains = Math.max(0, plugin.getConfig().getInt("nemesis.creation.throttle.maxCaptainsPerWorld", 0));
        long throttleWindowMinutes = Math.max(0L, plugin.getConfig().getLong("nemesis.creation.throttle.windowMinutes", 0L));
        this.creationThrottleWindowMs = Duration.ofMinutes(throttleWindowMinutes).toMillis();
        this.lowHealthFleePromotionChance = clampChance(plugin.getConfig().getDouble("nemesis.creation.lowHealthFleeChance", 0.25));
        this.lowHealthFleeProbeCooldownMs = Math.max(250L, plugin.getConfig().getLong("nemesis.creation.lowHealthFleeProbeCooldownMs", 2000L));
        this.playerEventRadiusBlocks = Math.max(32.0, plugin.getConfig().getDouble("nemesis.creation.playerEvent.maxRadiusBlocks", 5000.0));
        this.playerEventRadiusCap = Math.max(1.0, plugin.getConfig().getDouble("nemesis.creation.playerEvent.activeCapWithinRadius", 20.0));
        this.defyDeathPromotionEnabled = plugin.getConfig().getBoolean("nemesis.creation.defyDeath.enabled", true);
        this.defyDeathStartingLevel = Math.max(1, plugin.getConfig().getInt("nemesis.creation.defyDeath.startingLevel", 4));
        this.defyDeathStartingTier = normalizeTier(plugin.getConfig().getString("nemesis.creation.defyDeath.tier", "ELITE"));
        this.defyDeathBonusXp = Math.max(0L, plugin.getConfig().getLong("nemesis.creation.defyDeath.bonusXP", 300L));
        this.defyDeathBonusRivalry = Math.max(0.0, plugin.getConfig().getDouble("nemesis.creation.defyDeath.bonusRivalry", 20.0));
        indexExistingCaptains();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSprinting() || player.getWorld() == null) {
            return;
        }

        double maxHealth = Math.max(1.0, player.getMaxHealth());
        if (player.getHealth() > maxHealth * 0.5) {
            return;
        }

        long now = System.currentTimeMillis();
        long next = playerFleeProbeCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now < next) {
            return;
        }
        playerFleeProbeCooldown.put(player.getUniqueId(), now + lowHealthFleeProbeCooldownMs);

        if (Math.random() > lowHealthFleePromotionChance) {
            return;
        }

        List<LivingEntity> nearbyAggressive = player.getWorld().getLivingEntities().stream()
                .filter(living -> living instanceof Mob mob && mob.getTarget() != null && mob.getTarget().getUniqueId().equals(player.getUniqueId()))
                .filter(living -> living.getLocation().distanceSquared(player.getLocation()) <= 48.0 * 48.0)
                .toList();
        for (LivingEntity mob : nearbyAggressive) {
            if (createCaptainFromEntity(new KillerResolver.ResolvedKiller(mob.getUniqueId(), mob, KillerResolver.SourceType.DIRECT, null, DefeatSignature.ENVIRONMENT, java.time.Instant.now()), player.getUniqueId(), player, true)) {
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        DeathOutcomeResolver.DeathOutcome deathOutcome = deathOutcomeResolver.resolve(victim);
        if (deathOutcome == DeathOutcomeResolver.DeathOutcome.DEATH_PREVENTED) {
            return;
        }

        KillerResolver.ResolvedKiller resolvedKiller = killerResolver.resolve(victim, attributionTimeouts);
        if (resolvedKiller == null || resolvedKiller.entity() == null) {
            return;
        }

        boolean promoted;
        if (deathOutcome == DeathOutcomeResolver.DeathOutcome.DEATH_SOFT) {
            promoted = createHighRankingCaptainFromDefyDeath(resolvedKiller, victim.getUniqueId(), victim, true);
            if (!promoted) {
                plugin.getLogger().info("[Nemesis] Defy-death promotion skipped for " + victim.getName() + " due to anti-abuse constraints.");
            }
            return;
        }

        createCaptainFromEntity(resolvedKiller, victim.getUniqueId(), victim, true);
    }

    private boolean createHighRankingCaptainFromDefyDeath(KillerResolver.ResolvedKiller resolvedKiller, UUID victimUuid, Player victimPlayer, boolean enforcePlayerEventRadiusCap) {
        LivingEntity killer = resolvedKiller == null ? null : resolvedKiller.entity();
        if (!defyDeathPromotionEnabled) {
            return createCaptainFromEntity(resolvedKiller, victimUuid, victimPlayer, enforcePlayerEventRadiusCap);
        }

        if (!isEligibleKiller(killer)) {
            return false;
        }

        if (enforcePlayerEventRadiusCap && isPlayerEventAreaOverCap(killer.getLocation())) {
            return false;
        }

        UUID captainUuid = resolveCaptainUuid(killer);
        if (captainUuid != null) {
            CaptainRecord existing = captainRegistry.getByCaptainUuid(captainUuid);
            if (existing != null) {
                CaptainRecord promoted = promoteExistingCaptainFromDefyDeath(existing, victimUuid, resolvedKiller);
                maybeTauntDefyDeathReturn(victimPlayer, promoted);
                captainRegistry.upsert(promoted);
                captainEntityBinder.tryUpgradeInPlace(promoted);
                return true;
            }
        }

        CaptainRecord nearbyMatch = findNearbyDedupeMatch(killer, victimUuid);
        if (nearbyMatch != null) {
            entityToCaptainId.put(killer.getUniqueId(), nearbyMatch.identity().captainId());
            stampCaptainPdc(killer, nearbyMatch.identity().captainId());
            captainEntityBinder.bind(killer, nearbyMatch);
            CaptainRecord promoted = promoteExistingCaptainFromDefyDeath(nearbyMatch, victimUuid, resolvedKiller);
            maybeTauntDefyDeathReturn(victimPlayer, promoted);
            captainRegistry.upsert(promoted);
            captainEntityBinder.tryUpgradeInPlace(promoted);
            return true;
        }

        if (isCreationThrottled(killer)) {
            return false;
        }

        CaptainRecord created = createCaptainRecord(killer, victimUuid, true, resolvedKiller);
        entityToCaptainId.put(killer.getUniqueId(), created.identity().captainId());
        stampCaptainPdc(killer, created.identity().captainId());
        captainEntityBinder.bind(killer, created);
        captainRegistry.upsert(created);
        nemesisUI.announceCaptainBirth(created, victimPlayer);
        return true;
    }

    private boolean createCaptainFromEntity(KillerResolver.ResolvedKiller resolvedKiller, UUID victimUuid, Player victimPlayer, boolean enforcePlayerEventRadiusCap) {
        LivingEntity killer = resolvedKiller == null ? null : resolvedKiller.entity();
        if (!isEligibleKiller(killer)) {
            return false;
        }

        if (enforcePlayerEventRadiusCap && isPlayerEventAreaOverCap(killer.getLocation())) {
            return false;
        }

        UUID captainUuid = resolveCaptainUuid(killer);
        if (captainUuid != null) {
            CaptainRecord existing = captainRegistry.getByCaptainUuid(captainUuid);
            if (existing != null) {
                updateExistingCaptain(existing, victimUuid, victimPlayer, resolvedKiller);
                return true;
            }
        }

        CaptainRecord nearbyMatch = findNearbyDedupeMatch(killer, victimUuid);
        if (nearbyMatch != null) {
            entityToCaptainId.put(killer.getUniqueId(), nearbyMatch.identity().captainId());
            stampCaptainPdc(killer, nearbyMatch.identity().captainId());
            captainEntityBinder.bind(killer, nearbyMatch);
            updateExistingCaptain(nearbyMatch, victimUuid, victimPlayer, resolvedKiller);
            return true;
        }

        if (isCreationThrottled(killer)) {
            return false;
        }

        CaptainRecord created = createCaptainRecord(killer, victimUuid, false, resolvedKiller);
        entityToCaptainId.put(killer.getUniqueId(), created.identity().captainId());
        stampCaptainPdc(killer, created.identity().captainId());
        captainEntityBinder.bind(killer, created);
        captainRegistry.upsert(created);
        nemesisUI.announceCaptainBirth(created, victimPlayer);
        return true;
    }

    private boolean isPlayerEventAreaOverCap(Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }
        int nearbyCount = captainRegistry.getActiveByRadius(
                location.getWorld().getName(),
                location.getX(),
                location.getZ(),
                playerEventRadiusBlocks,
                null,
                null
        ).size();
        return nearbyCount > playerEventRadiusCap;
    }

    private void updateExistingCaptain(CaptainRecord existing, UUID victimUuid, Player victimPlayer, KillerResolver.ResolvedKiller resolvedKiller) {
        CaptainRecord updated = progressionService.onPlayerKill(existing, victimUuid);
        updated = withDefeatMemory(updated, resolvedKiller);
        captainRegistry.upsert(updated);
        captainEntityBinder.tryUpgradeInPlace(updated);
        Player nemesis = victimPlayer.getServer().getPlayer(updated.identity().nemesisOf());
        if (nemesis != null) {
            nemesisUI.taunt(nemesis, updated, "You cannot escape me.");
        }
    }

    private CaptainRecord findNearbyDedupeMatch(LivingEntity killer, UUID victimUuid) {
        if (dedupeRadius <= 0.0 || killer.getWorld() == null) {
            return null;
        }
        Location location = killer.getLocation();
        List<CaptainRecord> matches = captainRegistry.getActiveByRadius(
                killer.getWorld().getName(),
                location.getX(),
                location.getZ(),
                dedupeRadius,
                killer.getType(),
                victimUuid
        );
        if (matches.isEmpty()) {
            return null;
        }

        CaptainRecord best = null;
        double bestDistanceSquared = Double.MAX_VALUE;
        for (CaptainRecord match : matches) {
            CaptainRecord.Origin origin = match.origin();
            if (origin == null) {
                continue;
            }
            double dx = origin.spawnX() - location.getX();
            double dz = origin.spawnZ() - location.getZ();
            double distanceSquared = (dx * dx) + (dz * dz);
            if (distanceSquared < bestDistanceSquared) {
                best = match;
                bestDistanceSquared = distanceSquared;
            }
        }
        return best;
    }

    private boolean isCreationThrottled(LivingEntity killer) {
        if (creationThrottleMaxCaptains <= 0 || creationThrottleWindowMs <= 0 || killer.getWorld() == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long cutoff = now - creationThrottleWindowMs;
        int recentActiveCaptains = 0;
        for (CaptainRecord record : captainRegistry.getActiveByWorld(killer.getWorld().getName())) {
            if (record.identity() != null && record.identity().createdAtEpochMillis() >= cutoff) {
                recentActiveCaptains++;
            }
        }

        return recentActiveCaptains >= creationThrottleMaxCaptains;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCaptainDamaged(EntityDamageByEntityEvent event) {
        traitService.onDamage(event);
        if (event.getEntity() instanceof LivingEntity living) {
            CaptainRecord record = captainEntityBinder.resolveCaptainRecord(living).orElse(null);
            if (record != null) {
                progressionService.onCaptainDamaged(living, record, event);
                if (event.getDamager() instanceof LivingEntity attacker) {
                    CaptainRecord attackerRecord = captainEntityBinder.resolveCaptainRecord(attacker).orElse(null);
                    if (attackerRecord != null && !attackerRecord.identity().captainId().equals(record.identity().captainId())) {
                        progressionService.onCaptainSkirmish(record);
                        progressionService.onCaptainSkirmish(attackerRecord);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCaptainAttack(EntityDamageByEntityEvent event) {
        traitService.onAttack(event);

        if (!(event.getDamager() instanceof LivingEntity attackingEntity)) {
            return;
        }
        CaptainRecord attackingCaptain = captainEntityBinder.resolveCaptainRecord(attackingEntity).orElse(null);
        if (attackingCaptain == null) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            progressionService.onPlayerInteraction(attackingCaptain);
            return;
        }

        if (event.getEntity() instanceof LivingEntity victimEntity) {
            CaptainRecord victimCaptain = captainEntityBinder.resolveCaptainRecord(victimEntity).orElse(null);
            if (victimCaptain != null && !victimCaptain.identity().captainId().equals(attackingCaptain.identity().captainId())) {
                progressionService.onCaptainSkirmish(attackingCaptain);
                progressionService.onCaptainSkirmish(victimCaptain);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetChange(EntityTargetLivingEntityEvent event) {
        traitService.onTargetChange(event);

        if (!(event.getTarget() instanceof Player) || !(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }
        CaptainRecord record = captainEntityBinder.resolveCaptainRecord(livingEntity).orElse(null);
        if (record != null) {
            progressionService.onPlayerInteraction(record);
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCaptainDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity deadCaptainEntity)) {
            return;
        }
        CaptainRecord deadCaptain = captainEntityBinder.resolveCaptainRecord(deadCaptainEntity).orElse(null);
        if (deadCaptain == null || deadCaptain.identity() == null) {
            return;
        }

        CaptainRecord.State deadState = stateMachine.onKilled(System.currentTimeMillis());
        captainRegistry.upsert(deadCaptain.copyCore(
                deadCaptain.identity(),
                deadCaptain.origin(),
                deadCaptain.victims(),
                deadCaptain.nemesisScores(),
                deadCaptain.progression(),
                deadCaptain.naming(),
                deadCaptain.traits(),
                deadCaptain.minionPack(),
                deadState,
                deadCaptain.telemetry()
        ));

        DamageSource damageSource = event.getDamageSource();
        if (damageSource == null || !(damageSource.getCausingEntity() instanceof LivingEntity killerEntity)) {
            return;
        }
        CaptainRecord winner = captainEntityBinder.resolveCaptainRecord(killerEntity).orElse(null);
        if (winner == null || winner.identity() == null || winner.identity().captainId().equals(deadCaptain.identity().captainId())) {
            return;
        }

        CaptainRecord updatedWinner = progressionService.onCaptainVictory(winner, deadCaptain);
        captainRegistry.upsert(updatedWinner);
        captainEntityBinder.tryUpgradeInPlace(updatedWinner);

        String winnerName = updatedWinner.naming() == null ? "Nemesis Captain" : updatedWinner.naming().displayName();
        String loserName = deadCaptain.naming() == null ? "Nemesis Captain" : deadCaptain.naming().displayName();
        plugin.getServer().broadcastMessage("§4☠ §6" + winnerName + " §7slays rival captain §c" + loserName + "§7.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMinionDeath(EntityDeathEvent event) {
        traitService.onMinionDeath(event);
    }

    private void indexExistingCaptains() {
        for (CaptainRecord record : captainRegistry.getAll()) {
            if (record.identity() == null || record.identity().captainId() == null) {
                continue;
            }
            UUID entityUuid = record.state() == null ? null : record.state().runtimeEntityUuid();
            if (entityUuid != null) {
                entityToCaptainId.put(entityUuid, record.identity().captainId());
            }
        }
    }

    private boolean isEligibleKiller(LivingEntity killer) {
        if (killer instanceof Player) {
            return false;
        }

        if (!NemesisMobRules.isHostileOrAggressive(killer)) {
            return false;
        }

        if (killer.getScoreboardTags().contains(SpawnerTags.PLAYER_SPAWNER_MOB_TAG)) {
            return false;
        }

        if (!eligibleMobTypes.isEmpty() && !eligibleMobTypes.contains(killer.getType())) {
            return false;
        }

        if (NemesisMobRules.isExcludedFromCaptainPromotion(killer, worldBossTypeKey)) {
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

    private CaptainRecord createCaptainRecord(LivingEntity killer, UUID victimUuid, boolean elevatedDefyDeath, KillerResolver.ResolvedKiller resolvedKiller) {
        long now = System.currentTimeMillis();
        UUID captainId = UUID.randomUUID();
        Location location = killer.getLocation();

        CaptainRecord.Identity identity = new CaptainRecord.Identity(
                captainId,
                victimUuid,
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

        int startingLevel = elevatedDefyDeath ? defyDeathStartingLevel : 1;
        long startingXp = Math.max((long) (startingLevel - 1) * 100L, elevatedDefyDeath ? defyDeathBonusXp : xpPerKill);
        String startingTier = elevatedDefyDeath ? defyDeathStartingTier : "COMMON";
        double rivalry = elevatedDefyDeath ? rivalryPerKill + defyDeathBonusRivalry : rivalryPerKill;

        CaptainRecord.Victims victims = new CaptainRecord.Victims(List.of(victimUuid), 1, now);
        CaptainRecord.NemesisScores scores = new CaptainRecord.NemesisScores(elevatedDefyDeath ? 3.0 : 1.0, rivalry, elevatedDefyDeath ? 1.5 : 0.5, elevatedDefyDeath ? 1.0 : 0.5);
        CaptainRecord.Progression progression = new CaptainRecord.Progression(startingLevel, startingXp, startingTier);
        CaptainRecord.Traits traits = traitService.selectInitialTraits(identity, killer, scores);
        CaptainRecord.Naming naming = nameGenerator.generate(captainId, killer, traits);
        int minionCount = Math.max(0, plugin.getConfig().getInt("nemesis.minions.defaultCount", 2));
        List<String> minionArchetypes = plugin.getConfig().getStringList("nemesis.minions.defaultArchetypes");
        CaptainRecord.MinionPack minionPack = new CaptainRecord.MinionPack("default", minionCount, minionArchetypes, plugin.getConfig().getDouble("nemesis.minions.reinforcementChance", 0.0));
        CaptainRecord.State createdState = stateMachine.onCreate(now);
        CaptainRecord.State spawnedState = stateMachine.onSpawn(createdState.lastSeenEpochMs());
        CaptainRecord.State state = new CaptainRecord.State(spawnedState.state(), spawnedState.cooldownUntilEpochMs(), spawnedState.lastSeenEpochMs(), killer.getUniqueId());

        Map<String, Long> counters = new HashMap<>();
        counters.put("kills", 1L);
        counters.put("spawns", 1L);
        if (elevatedDefyDeath) {
            counters.put("defyDeathPromotions", 1L);
        }
        CaptainRecord.Telemetry telemetry = new CaptainRecord.Telemetry(now, now, 1, counters);

        CaptainRecord.Political political = new CaptainRecord.Political(
                plugin.getConfig().getString("nemesis.political.defaultRank", "UNALIGNED"),
                plugin.getConfig().getString("nemesis.political.defaultRegion",
                        plugin.getConfig().getString("nemesis.territory.defaultRegion", "UNKNOWN")),
                plugin.getConfig().getString("nemesis.political.defaultSeatId", ""),
                plugin.getConfig().getDouble("nemesis.political.defaultPromotionScore", 0.0),
                plugin.getConfig().getDouble("nemesis.political.defaultInfluence", 0.0)
        );
        CaptainRecord.Social social = new CaptainRecord.Social(
                plugin.getConfig().getDouble("nemesis.social.defaultLoyalty", 0.0),
                plugin.getConfig().getDouble("nemesis.social.defaultFear", 0.0),
                plugin.getConfig().getDouble("nemesis.social.defaultAmbition", 0.0),
                plugin.getConfig().getDouble("nemesis.social.defaultConfidence", 0.0)
        );
        CaptainRecord.Relationships relationships = new CaptainRecord.Relationships(List.of(), List.of(), null, null);
        String defaultCause = plugin.getConfig().getString("nemesis.memory.defaultLastDefeatCause", "");
        DefeatSignature defaultSignature = DefeatSignature.fromString(plugin.getConfig().getString("nemesis.memory.defaultDefeatSignature", "ENVIRONMENT"), DefeatSignature.ENVIRONMENT);
        String defeatCause = resolvedKiller == null || resolvedKiller.damageCause() == null ? defaultCause : resolvedKiller.damageCause().name();
        DefeatSignature defeatSignature = resolvedKiller == null ? defaultSignature : resolvedKiller.defeatSignature();
        CaptainRecord.Memory memory = new CaptainRecord.Memory(
                defeatCause,
                defeatSignature,
                List.of(),
                List.of(),
                List.of(),
                plugin.getConfig().getLong("nemesis.memory.defaultCallbackLinesSeed", 0L)
        );
        CaptainRecord.Persona persona = new CaptainRecord.Persona(
                plugin.getConfig().getString("nemesis.persona.defaultArchetype", "UNSPECIFIED"),
                plugin.getConfig().getString("nemesis.persona.defaultTemperament", "NEUTRAL"),
                List.of(),
                plugin.getConfig().getString("nemesis.persona.defaultVoicePackId", "")
        );

        return new CaptainRecord(identity, origin, victims, scores, progression, naming, traits, minionPack, state, telemetry,
                political, social, relationships, memory, persona);
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

    private double clampChance(double raw) {
        if (raw < 0.0) {
            return 0.0;
        }
        return Math.min(raw, 1.0);
    }

    private CaptainRecord promoteExistingCaptainFromDefyDeath(CaptainRecord existing, UUID victimUuid, KillerResolver.ResolvedKiller resolvedKiller) {
        long now = System.currentTimeMillis();
        List<UUID> victims = new ArrayList<>(existing.victims().playerVictims());
        if (!victims.contains(victimUuid)) {
            victims.add(victimUuid);
        }
        CaptainRecord.Victims updatedVictims = new CaptainRecord.Victims(victims, existing.victims().totalVictimCount() + 1, now);

        CaptainRecord.Progression currentProgression = existing.progression();
        int baseLevel = Math.max(currentProgression.level(), defyDeathStartingLevel);
        long boostedXp = Math.max(currentProgression.experience() + defyDeathBonusXp, (long) (baseLevel - 1) * 100L);
        int boostedLevel = Math.max(baseLevel, (int) (boostedXp / 100L) + 1);
        String bumpedTier = bumpTier(currentProgression.tier(), defyDeathStartingTier);
        CaptainRecord.Progression boostedProgression = new CaptainRecord.Progression(boostedLevel, boostedXp, bumpedTier);

        CaptainRecord.NemesisScores currentScores = existing.nemesisScores();
        CaptainRecord.NemesisScores boostedScores = new CaptainRecord.NemesisScores(
                currentScores.threat() + 2.0,
                currentScores.rivalry() + defyDeathBonusRivalry,
                currentScores.brutality() + 1.0,
                currentScores.cunning() + 0.5
        );

        Map<String, Long> counters = new HashMap<>(existing.telemetry().counters());
        counters.put("defyDeathPromotions", counters.getOrDefault("defyDeathPromotions", 0L) + 1L);
        counters.put("playerKills", counters.getOrDefault("playerKills", 0L) + 1L);
        CaptainRecord.Telemetry telemetry = new CaptainRecord.Telemetry(
                now,
                now,
                existing.telemetry().encounters(),
                counters
        );

        CaptainRecord promoted = existing.copyCore(
                existing.identity(),
                existing.origin(),
                updatedVictims,
                boostedScores,
                boostedProgression,
                existing.naming(),
                existing.traits(),
                existing.minionPack(),
                existing.state(),
                telemetry
        );
        return applyDefyDeathMutation(withDefeatMemory(promoted, resolvedKiller), resolvedKiller);
    }

    private CaptainRecord withDefeatMemory(CaptainRecord record, KillerResolver.ResolvedKiller resolvedKiller) {
        CaptainRecord.Memory previous = record.memory().orElseGet(() -> new CaptainRecord.Memory("", DefeatSignature.ENVIRONMENT, List.of(), List.of(), List.of(), 0L));
        String cause = resolvedKiller == null || resolvedKiller.damageCause() == null ? previous.lastDefeatCause() : resolvedKiller.damageCause().name();
        DefeatSignature signature = resolvedKiller == null ? previous.defeatSignature() : resolvedKiller.defeatSignature();
        CaptainRecord.Memory memory = new CaptainRecord.Memory(cause, signature, previous.scars(), previous.humiliations(), previous.notablePlayers(), previous.callbackLinesSeed());
        return new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), record.naming(), record.traits(), record.minionPack(), record.state(), record.telemetry(), record.political().orElse(null), record.social().orElse(null), record.relationships().orElse(null), memory, record.persona().orElse(null));
    }

    private CaptainRecord applyDefyDeathMutation(CaptainRecord record, KillerResolver.ResolvedKiller resolvedKiller) {
        CaptainRecord.Memory memory = record.memory().orElseGet(() -> new CaptainRecord.Memory("", DefeatSignature.ENVIRONMENT, List.of(), List.of(), List.of(), 0L));
        int repeatWindow = Math.max(1, plugin.getConfig().getInt("nemesis.memory.scarRepeatWindow", 3));
        List<String> history = new ArrayList<>(memory.humiliations());
        List<String> recent = history.subList(Math.max(0, history.size() - repeatWindow), history.size());

        List<String> paths = new ArrayList<>(List.of("SCAR", "TRAIT", "EPITHET", "RETURN_LINE"));
        paths.removeIf(path -> recent.contains("mutation:" + path));
        if (paths.isEmpty()) {
            paths = new ArrayList<>(List.of("SCAR", "TRAIT", "EPITHET", "RETURN_LINE"));
        }

        int index = Math.floorMod((int) (record.identity().captainId().getLeastSignificantBits() + history.size()), paths.size());
        String selected = paths.get(index);
        history.add("mutation:" + selected);

        List<String> scars = new ArrayList<>(memory.scars());
        CaptainRecord.Traits traits = record.traits();
        CaptainRecord.Naming naming = record.naming();

        if ("SCAR".equals(selected)) {
            String scar = switch (memory.defeatSignature()) {
                case FIRE -> "charred hide";
                case EXPLOSION -> "blast-rent armor";
                case PROJECTILE -> "arrow-pierced jaw";
                case MELEE -> "split brow";
                case FALL -> "cracked spine";
                case MAGIC -> "hex-burned skin";
                case ENVIRONMENT -> "withered scars";
            };
            if (!scars.contains(scar)) {
                scars.add(scar);
            }
        } else if ("TRAIT".equals(selected)) {
            String mappedWeakness = switch (memory.defeatSignature()) {
                case FIRE -> "weakness_fire_vulnerable";
                case PROJECTILE -> "weakness_fragile";
                case MAGIC -> "weakness_holy_water";
                case FALL, EXPLOSION, MELEE, ENVIRONMENT -> "weakness_slow_recovery";
            };
            String mappedImmunity = switch (memory.defeatSignature()) {
                case FIRE -> "immunity_fireproof";
                case PROJECTILE -> "immunity_projectile_guard";
                default -> "immunity_knockback";
            };
            String behaviorTrait = "personality_predator";

            CaptainRecord.Traits withWeakness = traitRegistry.mergeTrait(traits, mappedWeakness, false);
            if (!withWeakness.equals(traits)) {
                traits = withWeakness;
            } else {
                CaptainRecord.Traits withImmunity = traitRegistry.mergeTrait(traits, mappedImmunity, true);
                if (!withImmunity.equals(traits)) {
                    traits = withImmunity;
                } else {
                    traits = traitRegistry.mergeTrait(traits, behaviorTrait, true);
                }
            }
        } else if ("EPITHET".equals(selected)) {
            String epithet = switch (memory.defeatSignature()) {
                case FIRE -> "the Cinder-Returned";
                case EXPLOSION -> "the Blastborn";
                case PROJECTILE -> "the Arrow-Bitten";
                case MELEE -> "the Split-Brow";
                case FALL -> "the Stone-Broken";
                case MAGIC -> "the Hex-Scarred";
                case ENVIRONMENT -> "the Gravebound";
            };
            naming = new CaptainRecord.Naming(naming.displayName(), epithet, naming.title(), naming.aliasSeed());
        }

        CaptainRecord.Memory updatedMemory = new CaptainRecord.Memory(memory.lastDefeatCause(), memory.defeatSignature(), scars, history, memory.notablePlayers(), memory.callbackLinesSeed());
        CaptainRecord updated = new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), naming, traits, record.minionPack(), record.state(), record.telemetry(), record.political().orElse(null), record.social().orElse(null), record.relationships().orElse(null), updatedMemory, record.persona().orElse(null));
        updated = NemesisTelemetry.incrementCounter(updated, "returns", 1);
        if ("SCAR".equals(selected)) {
            updated = NemesisTelemetry.incrementCounter(updated, "scarTypes." + memory.defeatSignature().name().toLowerCase(Locale.ROOT), 1);
        }
        return updated;
    }

    private void maybeTauntDefyDeathReturn(Player victimPlayer, CaptainRecord promoted) {
        if (victimPlayer == null || promoted == null) {
            return;
        }
        Player nemesis = victimPlayer.getServer().getPlayer(promoted.identity().nemesisOf());
        if (nemesis == null) {
            return;
        }
        CaptainRecord.Memory memory = promoted.memory().orElse(null);
        String cause = memory == null || memory.lastDefeatCause().isBlank() ? "death itself" : memory.lastDefeatCause().toLowerCase(Locale.ROOT).replace('_', ' ');
        nemesisUI.taunt(nemesis, promoted, "I crawled back from " + cause + ".");
    }

    private String bumpTier(String currentTier, String minimumTier) {
        List<String> tiers = List.of("COMMON", "ELITE", "MYTHIC", "LEGENDARY");
        String normalizedCurrent = normalizeTier(currentTier);
        String normalizedMinimum = normalizeTier(minimumTier);
        int currentIndex = tiers.indexOf(normalizedCurrent);
        int minimumIndex = tiers.indexOf(normalizedMinimum);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        if (minimumIndex < 0) {
            minimumIndex = 1;
        }
        int nextIndex = Math.min(tiers.size() - 1, Math.max(minimumIndex, currentIndex + 1));
        return tiers.get(nextIndex);
    }

    private String normalizeTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return "COMMON";
        }
        String normalized = tier.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "COMMON", "ELITE", "MYTHIC", "LEGENDARY" -> normalized;
            default -> "COMMON";
        };
    }
}
