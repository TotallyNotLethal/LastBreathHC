package com.lastbreath.hc.lastBreathHC.holiday;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HolidayThemedEncounterListener implements Listener {

    private final HolidayGameplayManager holidayGameplayManager;

    public HolidayThemedEncounterListener(HolidayGameplayManager holidayGameplayManager) {
        this.holidayGameplayManager = holidayGameplayManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        HolidayType activeHoliday = holidayGameplayManager.getActiveHolidayType().orElse(null);
        if (activeHoliday == null) {
            return;
        }
        if (!isInsideActiveZone(event.getLocation())) {
            return;
        }

        switch (activeHoliday) {
            case NEW_YEAR -> buffNewYear(event.getEntity());
            case VALENTINES -> buffValentines(event.getEntity());
            case ST_PATRICKS -> buffStPatricks(event.getEntity());
            case EASTER -> buffEaster(event.getEntity());
            case INDEPENDENCE_DAY -> buffIndependenceDay(event.getEntity());
            case HALLOWEEN -> buffHalloween(event.getEntity());
            case THANKSGIVING -> buffThanksgiving(event.getEntity());
            case CHRISTMAS -> buffChristmas(event.getEntity());
        }
    }

    private boolean isInsideActiveZone(Location location) {
        return holidayGameplayManager.getActiveDefinition()
                .map(HolidayEventDefinition::zone)
                .map(zone -> zone.contains(location))
                .orElse(false);
    }

    private void buffNewYear(LivingEntity entity) {
        if (entity.getType() != EntityType.PHANTOM) {
            return;
        }
        elite(entity, ChatColor.AQUA + "Countdown Phantom", 30.0D, 1, 0, 0);
    }

    private void buffValentines(LivingEntity entity) {
        if (entity.getType() != EntityType.BEE) {
            return;
        }
        elite(entity, ChatColor.LIGHT_PURPLE + "Cupid's Guard", 24.0D, 1, 0, 0);
    }

    private void buffStPatricks(LivingEntity entity) {
        if (entity.getType() != EntityType.CAVE_SPIDER) {
            return;
        }
        elite(entity, ChatColor.GREEN + "Emerald Gremlin", 28.0D, 1, 0, 0);
    }

    private void buffEaster(LivingEntity entity) {
        if (entity.getType() != EntityType.RABBIT) {
            return;
        }
        Rabbit rabbit = (Rabbit) entity;
        rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
        elite(rabbit, ChatColor.LIGHT_PURPLE + "Angry Giant Bunny", 40.0D, 1, 0, 2);
    }

    private void buffIndependenceDay(LivingEntity entity) {
        if (entity.getType() != EntityType.CREEPER) {
            return;
        }
        Creeper creeper = (Creeper) entity;
        creeper.setPowered(true);
        elite(creeper, ChatColor.RED + "Powder Saboteur", 36.0D, 1, 0, 0);
    }

    private void buffHalloween(LivingEntity entity) {
        if (entity.getType() != EntityType.ZOMBIE) {
            return;
        }
        elite(entity, ChatColor.GOLD + "Hollow Lanternfiend", 34.0D, 1, 1, 0);
    }

    private void buffThanksgiving(LivingEntity entity) {
        if (entity.getType() != EntityType.PILLAGER) {
            return;
        }
        elite(entity, ChatColor.GOLD + "Harvest Raider", 34.0D, 1, 0, 0);
    }

    private void buffChristmas(LivingEntity entity) {
        if (entity.getType() != EntityType.STRAY) {
            return;
        }
        elite(entity, ChatColor.AQUA + "Frostbound Stray", 34.0D, 1, 0, 0);
    }

    private void elite(LivingEntity entity, String name, double maxHealth, int speedAmp, int strengthAmp, int jumpAmp) {
        if (entity.getScoreboardTags().contains("holiday_elite")) {
            return;
        }
        entity.addScoreboardTag("holiday_elite");
        entity.customName(Component.text(name));
        entity.setCustomNameVisible(true);

        AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(maxHealth);
            entity.setHealth(maxHealth);
        }

        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, Math.max(0, speedAmp), true, false));
        if (strengthAmp >= 0) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, strengthAmp, true, false));
        }
        if (jumpAmp > 0) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, jumpAmp, true, false));
        }
    }
}
