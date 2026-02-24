package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class LoyaltyService implements Listener {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;
    private final ArmyGraphService graphService;
    private final boolean enabled;
    private final boolean betrayalEnabled;
    private final double betrayalBaseChance;
    private final StructureEventOrchestrator structureEventOrchestrator;

    public LoyaltyService(LastBreathHC plugin, CaptainRegistry registry, CaptainEntityBinder binder, ArmyGraphService graphService, StructureEventOrchestrator structureEventOrchestrator) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.graphService = graphService;
        this.enabled = plugin.getConfig().getBoolean("nemesis.loyalty.enabled", true);
        this.betrayalEnabled = plugin.getConfig().getBoolean("nemesis.loyalty.betrayal.enabled", true);
        this.betrayalBaseChance = Math.max(0.0, plugin.getConfig().getDouble("nemesis.loyalty.betrayal.baseChance", 0.08));
        this.structureEventOrchestrator = structureEventOrchestrator;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCaptainSkirmish(EntityDamageByEntityEvent event) {
        if (!enabled || !betrayalEnabled) {
            return;
        }
        evaluateBetrayal(event.getDamager(), event.getEntity());
    }

    public boolean evaluateBetrayal(Entity aggressor, Entity defender) {
        if (!enabled || !betrayalEnabled) {
            return false;
        }
        Optional<CaptainRecord> attackerRecord = binder.resolveCaptainRecord(aggressor);
        Optional<CaptainRecord> victimRecord = binder.resolveCaptainRecord(defender);
        if (attackerRecord.isEmpty() || victimRecord.isEmpty()) {
            return false;
        }

        CaptainRecord attacker = attackerRecord.get();
        CaptainRecord victim = victimRecord.get();
        CaptainRecord.Social social = attacker.social().orElse(new CaptainRecord.Social(0.5, 0.0, 0.5, 0.5));
        double chance = betrayalBaseChance + (social.ambition() * 0.08) + (social.fear() * 0.04) - (social.loyalty() * 0.10);
        if (Math.random() > Math.max(0.0, Math.min(1.0, chance))) {
            return false;
        }

        graphService.addRivalry(attacker.identity().captainId(), victim.identity().captainId());
        CaptainRecord shifted = NemesisTelemetry.incrementCounter(attacker, "betrayals", 1);
        shifted = NemesisTelemetry.incrementCounter(shifted, "loyaltyShifts", 1);
        registry.upsert(shifted);
        plugin.getServer().broadcastMessage("§4Nemesis betrayal: §c" + attacker.naming().displayName() + " §7turned on §c" + victim.naming().displayName());
        structureEventOrchestrator.onBetrayal(new CaptainBetrayalEvent(
                attacker.identity().captainId(),
                victim.identity().captainId(),
                "captain_betrayal",
                System.currentTimeMillis()
        ));
        return true;
    }
}
