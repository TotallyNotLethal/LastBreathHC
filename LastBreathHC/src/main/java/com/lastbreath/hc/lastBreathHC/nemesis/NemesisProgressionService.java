package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NemesisProgressionService {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;
    private final Map<UUID, Long> combatTrackedAt = new ConcurrentHashMap<>();
    private final CaptainStateMachine stateMachine = new CaptainStateMachine();
    private BukkitTask combatTask;

    private final long xpPerKill;
    private final long xpPerCombatTick;
    private final double angerPerKill;

    public NemesisProgressionService(LastBreathHC plugin, CaptainRegistry registry, CaptainEntityBinder binder) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.xpPerKill = Math.max(1L, plugin.getConfig().getLong("nemesis.progression.xpPerVictim", 25L));
        this.xpPerCombatTick = Math.max(1L, plugin.getConfig().getLong("nemesis.progression.xpPerCombatTick", 2L));
        this.angerPerKill = Math.max(0.0, plugin.getConfig().getDouble("nemesis.progression.angerPerKill", 3.0));
    }

    public void start() {
        stop();
        combatTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickCombatXp, 100L, 100L);
    }

    public void stop() {
        if (combatTask != null) {
            combatTask.cancel();
            combatTask = null;
        }
        combatTrackedAt.clear();
    }

    public CaptainRecord onPlayerKill(CaptainRecord record, UUID victimUuid) {
        long now = System.currentTimeMillis();
        CaptainRecord.Progression progression = applyXp(record, xpPerKill);
        CaptainRecord.NemesisScores scores = new CaptainRecord.NemesisScores(
                record.nemesisScores().threat() + 1.0,
                record.nemesisScores().rivalry() + angerPerKill,
                record.nemesisScores().brutality() + 0.5,
                record.nemesisScores().cunning()
        );
        List<UUID> victims = new ArrayList<>(record.victims().playerVictims());
        if (!victims.contains(victimUuid)) {
            victims.add(victimUuid);
        }
        CaptainRecord.Victims updatedVictims = new CaptainRecord.Victims(victims, record.victims().totalVictimCount() + 1, now);
        CaptainRecord.Telemetry telemetry = withCounter(record, "playerKills", 1L, now);
        CaptainRecord updated = new CaptainRecord(record.identity(), record.origin(), updatedVictims, scores, progression, record.naming(), record.traits(), record.minionPack(), record.state(), telemetry);
        combatTrackedAt.put(record.identity().captainId(), now);
        return updated;
    }

    public void onMinionDeath(UUID captainId, long xp, double anger) {
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null) {
            return;
        }
        long now = System.currentTimeMillis();
        CaptainRecord.Progression progression = applyXp(record, xp);
        CaptainRecord.NemesisScores scores = new CaptainRecord.NemesisScores(
                record.nemesisScores().threat() + (anger * 0.25),
                record.nemesisScores().rivalry() + anger,
                record.nemesisScores().brutality() + (anger * 0.5),
                record.nemesisScores().cunning()
        );
        CaptainRecord.Telemetry telemetry = withCounter(record, "minionDeaths", 1L, now);
        registry.upsert(new CaptainRecord(record.identity(), record.origin(), record.victims(), scores, progression, record.naming(), record.traits(), record.minionPack(), record.state(), telemetry));
    }

    public void onCaptainDamaged(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        combatTrackedAt.put(record.identity().captainId(), System.currentTimeMillis());

        double remaining = captain.getHealth() - event.getFinalDamage();
        double escapeHpPct = plugin.getConfig().getDouble("nemesis.escape.healthThreshold", 0.12);
        double escapeChance = plugin.getConfig().getDouble("nemesis.escape.chance", 0.25);
        if (remaining > captain.getMaxHealth() * escapeHpPct || Math.random() > escapeChance) {
            return;
        }
        if (record.state() == null || record.state().state() != CaptainState.ACTIVE) {
            return;
        }

        long now = System.currentTimeMillis();
        List<String> traitIds = new ArrayList<>(record.traits().traits());
        if (!traitIds.contains("personality_berserker")) {
            traitIds.add("personality_berserker");
        }
        CaptainRecord.Traits traits = new CaptainRecord.Traits(traitIds, record.traits().weaknesses(), record.traits().immunities());
        long cooldownUntil = now + plugin.getConfig().getLong("nemesis.spawn.recordCooldownMs", 30_000L);
        CaptainRecord.State state = stateMachine.onEscapeOrDespawn(now, cooldownUntil);
        CaptainRecord.Telemetry telemetry = withCounter(record, "escapes", 1L, now);

        registry.upsert(new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), record.naming(), traits, record.minionPack(), state, telemetry));
        captain.getWorld().spawnParticle(Particle.SMOKE, captain.getLocation(), 50, 0.6, 0.6, 0.6, 0.02);
        captain.remove();
    }

    private void tickCombatXp() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : new ArrayList<>(combatTrackedAt.entrySet())) {
            if (now - entry.getValue() > 20_000L) {
                combatTrackedAt.remove(entry.getKey());
                continue;
            }
            CaptainRecord record = registry.getByCaptainUuid(entry.getKey());
            if (record == null || record.state() == null || record.state().state() != CaptainState.ACTIVE) {
                continue;
            }
            CaptainRecord.Progression progression = applyXp(record, xpPerCombatTick);
            CaptainRecord.Telemetry telemetry = withCounter(record, "combatTicks", 1L, now);
            CaptainRecord.Traits evolvedTraits = maybeGrantProgressionStrength(record, progression.level());
            registry.upsert(new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), progression, maybeUnlockTrait(record, progression.level()), evolvedTraits, record.minionPack(), record.state(), telemetry));
        }
    }

    private CaptainRecord.Naming maybeUnlockTrait(CaptainRecord record, int level) {
        int unlockLevel = plugin.getConfig().getInt("nemesis.progression.traitUnlockLevel", 5);
        if (level < unlockLevel || level % unlockLevel != 0) {
            return record.naming();
        }
        maybeGrantProgressionStrength(record, level);
        return record.naming();
    }

    public CaptainRecord onCaptainVictory(CaptainRecord winner, CaptainRecord loser) {
        long bonusXp = Math.max(10L, plugin.getConfig().getLong("nemesis.progression.xpPerCaptainVictory", 55L));
        CaptainRecord.Progression progression = applyXp(winner, bonusXp);
        List<UUID> victims = new ArrayList<>(winner.victims().playerVictims());
        if (loser.identity() != null && loser.identity().nemesisOf() != null && !victims.contains(loser.identity().nemesisOf())) {
            victims.add(loser.identity().nemesisOf());
        }
        long now = System.currentTimeMillis();
        CaptainRecord.Victims updatedVictims = new CaptainRecord.Victims(victims, winner.victims().totalVictimCount() + 1, now);
        CaptainRecord.NemesisScores updatedScores = new CaptainRecord.NemesisScores(
                winner.nemesisScores().threat() + 1.5,
                winner.nemesisScores().rivalry() + 2.0,
                winner.nemesisScores().brutality() + 1.0,
                winner.nemesisScores().cunning() + 0.5
        );
        CaptainRecord.Traits boostedTraits = maybeGrantProgressionStrength(winner, progression.level());
        CaptainRecord.Telemetry telemetry = withCounter(winner, "captainVictories", 1L, now);
        return new CaptainRecord(winner.identity(), winner.origin(), updatedVictims, updatedScores, progression, maybeUnlockTrait(winner, progression.level()), boostedTraits, winner.minionPack(), winner.state(), telemetry);
    }

    private CaptainRecord.Traits maybeGrantProgressionStrength(CaptainRecord record, int level) {
        int unlockLevel = Math.max(2, plugin.getConfig().getInt("nemesis.progression.traitUnlockLevel", 5));
        if (level < unlockLevel || level % unlockLevel != 0) {
            return record.traits();
        }

        List<String> strengths = new ArrayList<>(record.traits().traits());
        List<String> immunities = new ArrayList<>(record.traits().immunities());
        if (!strengths.contains("strength_vicious_combo")) {
            strengths.add("strength_vicious_combo");
        } else if (!strengths.contains("strength_warlord_presence")) {
            strengths.add("strength_warlord_presence");
        } else if (!immunities.contains("immunity_fireproof")) {
            immunities.add("immunity_fireproof");
        } else if (!immunities.contains("immunity_projectile_guard")) {
            immunities.add("immunity_projectile_guard");
        }
        return new CaptainRecord.Traits(strengths, record.traits().weaknesses(), immunities);
    }

    private CaptainRecord.Progression applyXp(CaptainRecord record, long delta) {
        long xp = record.progression().experience() + delta;
        int oldLevel = record.progression().level();
        int level = Math.max(1, (int) (xp / 100L) + 1);
        double perLevelStat = plugin.getConfig().getDouble("nemesis.progression.statPerLevel", 0.15);
        if (level > oldLevel) {
            CaptainRecord.NemesisScores s = record.nemesisScores();
            CaptainRecord.NemesisScores ns = new CaptainRecord.NemesisScores(
                    s.threat() + (level - oldLevel) * perLevelStat,
                    s.rivalry() + (level - oldLevel) * perLevelStat,
                    s.brutality() + (level - oldLevel) * perLevelStat,
                    s.cunning() + (level - oldLevel) * perLevelStat
            );
            registry.upsert(new CaptainRecord(record.identity(), record.origin(), record.victims(), ns, new CaptainRecord.Progression(level, xp, record.progression().tier()), record.naming(), record.traits(), record.minionPack(), record.state(), record.telemetry()));
            return new CaptainRecord.Progression(level, xp, record.progression().tier());
        }
        return new CaptainRecord.Progression(level, xp, record.progression().tier());
    }

    private CaptainRecord.Telemetry withCounter(CaptainRecord record, String key, long inc, long now) {
        Map<String, Long> counters = new HashMap<>(record.telemetry().counters());
        counters.put(key, counters.getOrDefault(key, 0L) + inc);
        return new CaptainRecord.Telemetry(now, now, record.telemetry().encounters(), counters);
    }
}
