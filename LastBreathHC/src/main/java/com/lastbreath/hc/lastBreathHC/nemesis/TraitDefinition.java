package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public interface TraitDefinition {
    String id();

    default void apply(LivingEntity captain, CaptainRecord record) {
    }

    default void onTick(LivingEntity captain, CaptainRecord record) {
    }

    default void onDamage(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
    }

    default void onAttack(LivingEntity captain, CaptainRecord record, EntityDamageByEntityEvent event) {
    }

    default void onTargetChange(LivingEntity captain, CaptainRecord record, EntityTargetLivingEntityEvent event) {
    }

    default void onMinionDeath(LivingEntity captain, CaptainRecord record, EntityDeathEvent minionDeathEvent, Player killer) {
    }
}
