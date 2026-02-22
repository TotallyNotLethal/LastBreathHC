package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import java.util.ArrayList;
import java.util.List;

public class CaptainTraitService {
    private final CaptainEntityBinder binder;
    private final CaptainTraitRegistry traitRegistry;

    public CaptainTraitService(CaptainEntityBinder binder, CaptainTraitRegistry traitRegistry) {
        this.binder = binder;
        this.traitRegistry = traitRegistry;
    }

    public CaptainRecord.Traits selectInitialTraits(CaptainRecord.Identity identity,
                                                    LivingEntity killer,
                                                    CaptainRecord.NemesisScores scores) {
        return traitRegistry.selectTraits(identity.captainUuid(), killer, identity.nemesisOf(), scores);
    }

    public void applyOnBind(LivingEntity captain, CaptainRecord record) {
        for (TraitDefinition definition : definitionsFor(record)) {
            definition.apply(captain, record);
        }
    }

    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        binder.resolveCaptainRecord(living).ifPresent(record -> {
            for (TraitDefinition definition : definitionsFor(record)) {
                definition.onDamage(living, record, event);
            }
        });
    }

    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity living)) {
            return;
        }
        binder.resolveCaptainRecord(living).ifPresent(record -> {
            for (TraitDefinition definition : definitionsFor(record)) {
                definition.onAttack(living, record, event);
            }
        });
    }

    public void onTargetChange(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        binder.resolveCaptainRecord(living).ifPresent(record -> {
            for (TraitDefinition definition : definitionsFor(record)) {
                definition.onTargetChange(living, record, event);
            }
        });
    }

    public void onMinionDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) {
            return;
        }
        String captainId = event.getEntity().getPersistentDataContainer().get(binder.getCaptainIdKey(), org.bukkit.persistence.PersistentDataType.STRING);
        if (captainId == null) {
            return;
        }
        try {
            java.util.UUID uuid = java.util.UUID.fromString(captainId);
            CaptainRecord record = binder.getCaptainRegistry().getByCaptainUuid(uuid);
            if (record == null) {
                return;
            }
            org.bukkit.entity.Entity entity = killer.getServer().getEntity(record.identity().spawnEntityUuid());
            if (!(entity instanceof LivingEntity livingCaptain)) {
                return;
            }
            for (TraitDefinition definition : definitionsFor(record)) {
                definition.onMinionDeath(livingCaptain, record, event, killer);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private List<TraitDefinition> definitionsFor(CaptainRecord record) {
        List<TraitDefinition> defs = new ArrayList<>();
        if (record.traits() != null) {
            for (String id : record.traits().traits()) {
                TraitDefinition definition = traitRegistry.definition(id);
                if (definition != null) {
                    defs.add(definition);
                }
            }
            for (String id : record.traits().weaknesses()) {
                TraitDefinition definition = traitRegistry.definition(id);
                if (definition != null) {
                    defs.add(definition);
                }
            }
        }
        return defs;
    }
}
