package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillerResolver implements Listener {

    private final Map<UUID, DamageAttribution> recentHostileDamage = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Entity damager = event.getDamager();
        SourceType sourceType = SourceType.DIRECT;

        if (damager instanceof ThrownPotion thrownPotion) {
            sourceType = SourceType.POTION;
            Object shooter = thrownPotion.getShooter();
            if (shooter instanceof Entity shooterEntity) {
                damager = shooterEntity;
            }
        } else if (damager instanceof Projectile projectile) {
            sourceType = SourceType.PROJECTILE;
            Object shooter = projectile.getShooter();
            if (shooter instanceof Entity shooterEntity) {
                damager = shooterEntity;
            }
        } else if (damager instanceof AreaEffectCloud cloud) {
            sourceType = SourceType.AREA_EFFECT_CLOUD;
            Object source = cloud.getSource();
            if (source instanceof Entity sourceEntity) {
                damager = sourceEntity;
            }
        }

        if (!(damager instanceof LivingEntity livingDamager)) {
            return;
        }

        recentHostileDamage.put(player.getUniqueId(), new DamageAttribution(livingDamager, sourceType, Instant.now()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOtherDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        DamageAttribution attribution = recentHostileDamage.get(player.getUniqueId());
        if (attribution == null) {
            return;
        }

        if (!attribution.damager().isValid()) {
            recentHostileDamage.remove(player.getUniqueId());
        }
    }

    public ResolvedKiller resolve(Player player, Duration timeout) {
        if (player == null) {
            return null;
        }

        DamageAttribution directAttribution = resolveDirect(player);
        if (directAttribution != null) {
            recentHostileDamage.put(player.getUniqueId(), directAttribution);
            return new ResolvedKiller(
                    directAttribution.damager().getUniqueId(),
                    directAttribution.damager(),
                    directAttribution.sourceType(),
                    directAttribution.timestamp()
            );
        }

        DamageAttribution recent = recentHostileDamage.get(player.getUniqueId());
        if (recent == null || !recent.damager().isValid()) {
            return null;
        }

        Duration elapsed = Duration.between(recent.timestamp(), Instant.now());
        if (elapsed.compareTo(timeout) > 0) {
            return null;
        }

        return new ResolvedKiller(recent.damager().getUniqueId(), recent.damager(), SourceType.RECENT_HOSTILE, recent.timestamp());
    }

    private DamageAttribution resolveDirect(Player player) {
        EntityDamageEvent lastDamageCause = player.getLastDamageCause();
        if (!(lastDamageCause instanceof EntityDamageByEntityEvent byEntityEvent)) {
            return null;
        }

        Entity damager = byEntityEvent.getDamager();
        SourceType sourceType = SourceType.DIRECT;

        if (damager instanceof ThrownPotion thrownPotion) {
            sourceType = SourceType.POTION;
            Object shooter = thrownPotion.getShooter();
            if (shooter instanceof Entity shooterEntity) {
                damager = shooterEntity;
            }
        } else if (damager instanceof Projectile projectile) {
            sourceType = SourceType.PROJECTILE;
            Object shooter = projectile.getShooter();
            if (shooter instanceof Entity shooterEntity) {
                damager = shooterEntity;
            }
        } else if (damager instanceof AreaEffectCloud cloud) {
            sourceType = SourceType.AREA_EFFECT_CLOUD;
            Object source = cloud.getSource();
            if (source instanceof Entity sourceEntity) {
                damager = sourceEntity;
            }
        }

        if (!(damager instanceof LivingEntity livingEntity)) {
            return null;
        }

        return new DamageAttribution(livingEntity, sourceType, Instant.now());
    }

    private record DamageAttribution(LivingEntity damager, SourceType sourceType, Instant timestamp) {
    }

    public record ResolvedKiller(UUID entityUuid, LivingEntity entity, SourceType sourceType, Instant timestamp) {
    }

    public enum SourceType {
        DIRECT,
        PROJECTILE,
        POTION,
        AREA_EFFECT_CLOUD,
        RECENT_HOSTILE
    }
}
