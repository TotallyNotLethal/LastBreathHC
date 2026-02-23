package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

class NightStalkerTrait implements TraitDefinition {
    public String id() { return "context_night_stalker"; }
    public void apply(LivingEntity captain, CaptainRecord record) {
        if (captain.getWorld().getTime() > 13000L) {
            AttributeInstance speed = captain.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.1);
        }
    }
}

class FrostboundTrait implements TraitDefinition {
    public String id() { return "context_frostbound"; }
    public void onAttack(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player) {
            player.setFreezeTicks(Math.min(player.getMaxFreezeTicks(), player.getFreezeTicks() + 40));
        }
    }
}

class BerserkerTrait implements TraitDefinition {
    public String id() { return "personality_berserker"; }
    public void onAttack(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (captain.getHealth() <= captain.getMaxHealth() * 0.5) event.setDamage(event.getDamage() * 1.2);
    }
}

class PredatorTrait implements TraitDefinition {
    public String id() { return "personality_predator"; }
    public void onAttack(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player) event.setDamage(event.getDamage() * 1.1);
    }
}

class BrutalStrikesTrait implements TraitDefinition {
    public String id() { return "strength_brutal_strikes"; }
    public void onAttack(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 1.15);
    }
}

class FragileWeaknessTrait implements TraitDefinition {
    public String id() { return "weakness_fragile"; }
    public void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 1.15);
    }
}

class SunboundWeaknessTrait implements TraitDefinition {
    public String id() { return "weakness_sunbound"; }
    public void apply(LivingEntity captain, CaptainRecord record) {
        World world = captain.getWorld();
        if (world.getEnvironment() == World.Environment.NORMAL && world.getTime() < 12000L) {
            captain.setFireTicks(Math.max(captain.getFireTicks(), 40));
        }
    }
}

class SlowRecoveryWeaknessTrait implements TraitDefinition {
    public String id() { return "weakness_slow_recovery"; }
    public void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        captain.setNoDamageTicks(0);
    }
}

class KnockbackImmunityTrait implements TraitDefinition {
    public String id() { return "immunity_knockback"; }
    public void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        captain.setVelocity(captain.getVelocity().multiply(0.25));
    }
}

class FireproofImmunityTrait implements TraitDefinition {
    public String id() { return "immunity_fireproof"; }
    public void apply(LivingEntity captain, CaptainRecord record) {
        captain.setFireTicks(0);
    }

    public void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.LAVA) {
            event.setCancelled(true);
        }
    }
}

class GoldCursedWeaknessTrait implements TraitDefinition {
    public String id() { return "weakness_gold_cursed"; }
    public void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Player player && player.getInventory().getItemInMainHand().getType().name().contains("GOLD")) {
            event.setDamage(event.getDamage() * 1.35);
        }
    }
}

class FireVulnerableWeaknessTrait implements TraitDefinition {
    public String id() { return "weakness_fire_vulnerable"; }
    public void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.LAVA) {
            event.setDamage(event.getDamage() * 1.45);
        }
    }
}

class DaylightHuntedWeaknessTrait implements TraitDefinition {
    public String id() { return "weakness_daylight_hunted"; }
    public void apply(LivingEntity captain, CaptainRecord record) {
        if (captain.getWorld().getTime() < 12000L) {
            captain.setFireTicks(Math.max(60, captain.getFireTicks()));
        }
    }
}

class HolyWaterWeaknessTrait implements TraitDefinition {
    public String id() { return "weakness_holy_water"; }
    public void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Snowball) {
            event.setDamage(event.getDamage() * 1.3);
        }
    }
}

class ViciousComboTrait implements TraitDefinition {
    public String id() { return "strength_vicious_combo"; }
    public void onAttack(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 1.2);
    }
}

class WarlordPresenceTrait implements TraitDefinition {
    public String id() { return "strength_warlord_presence"; }
    public void apply(LivingEntity captain, CaptainRecord record) {
        AttributeInstance maxHealth = captain.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * 1.1);
            captain.setHealth(Math.min(captain.getHealth() + 4.0, maxHealth.getBaseValue()));
        }
    }
}

class VenomBladeTrait implements TraitDefinition {
    public String id() { return "strength_venom_blade"; }
    public void onAttack(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 60, 0));
        }
    }
}

class ProjectileGuardImmunityTrait implements TraitDefinition {
    public String id() { return "immunity_projectile_guard"; }
    public void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            event.setDamage(event.getDamage() * 0.45);
        }
    }
}
