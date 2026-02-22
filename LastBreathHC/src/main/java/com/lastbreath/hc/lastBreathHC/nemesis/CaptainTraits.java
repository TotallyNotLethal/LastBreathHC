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
