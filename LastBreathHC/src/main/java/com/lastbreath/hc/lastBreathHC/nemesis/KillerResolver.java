package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillerResolver implements Listener {

    private static final int MAX_ATTRIBUTION_HISTORY = 8;

    private final Map<UUID, Deque<DamageAttribution>> recentHostileDamage = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        DamageAttribution attribution = resolveAttribution(event.getDamager(), event.getCause());
        if (attribution == null) {
            return;
        }

        recordAttribution(player, attribution);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOtherDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Deque<DamageAttribution> history = recentHostileDamage.get(player.getUniqueId());
        if (history == null) {
            return;
        }

        Instant now = Instant.now();
        history.removeIf(attribution -> !attribution.damager().isValid() || Duration.between(attribution.timestamp(), now).compareTo(Duration.ofMinutes(2)) > 0);

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            DamageAttribution knockback = selectBestAttribution(history, ResolveCategory.KNOCKBACK, ResolveCategory.PROJECTILE, ResolveCategory.AMBIENT);
            if (knockback != null) {
                recordAttribution(player, knockback.withSourceType(SourceType.KNOCKBACK));
            }
        }

        if (history.isEmpty()) {
            recentHostileDamage.remove(player.getUniqueId());
        }
    }

    public ResolvedKiller resolve(Player player, AttributionTimeouts timeouts) {
        if (player == null) {
            return null;
        }

        DamageAttribution directAttribution = resolveDirect(player);
        if (directAttribution != null) {
            recordAttribution(player, directAttribution);
            return new ResolvedKiller(
                    directAttribution.damager().getUniqueId(),
                    directAttribution.damager(),
                    directAttribution.sourceType(),
                    directAttribution.damageCause(),
                    directAttribution.timestamp()
            );
        }

        Deque<DamageAttribution> history = recentHostileDamage.get(player.getUniqueId());
        if (history == null || history.isEmpty()) {
            return null;
        }

        DamageAttribution best = resolveFromHistory(player, history, timeouts);
        if (best == null) {
            return null;
        }

        return new ResolvedKiller(best.damager().getUniqueId(), best.damager(), best.sourceType(), best.damageCause(), best.timestamp());
    }

    private DamageAttribution resolveDirect(Player player) {
        EntityDamageEvent lastDamageCause = player.getLastDamageCause();
        if (!(lastDamageCause instanceof EntityDamageByEntityEvent byEntityEvent)) {
            return null;
        }

        return resolveAttribution(byEntityEvent.getDamager(), byEntityEvent.getCause());
    }

    private DamageAttribution resolveAttribution(Entity rawDamager, EntityDamageEvent.DamageCause damageCause) {
        Entity damager = rawDamager;
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
        } else if (damager instanceof TNTPrimed tntPrimed) {
            sourceType = SourceType.EXPLOSION;
            Entity source = tntPrimed.getSource();
            if (source != null) {
                damager = source;
            }
        } else if (damager instanceof Explosive explosive) {
            sourceType = SourceType.EXPLOSION;
            Entity source = explosive.getSource();
            if (source != null) {
                damager = source;
            }
        }

        if (!(damager instanceof LivingEntity livingEntity)) {
            return null;
        }

        return new DamageAttribution(livingEntity, sourceType, damageCause, Instant.now());
    }

    private void recordAttribution(Player player, DamageAttribution attribution) {
        Deque<DamageAttribution> history = recentHostileDamage.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        history.addFirst(attribution);
        while (history.size() > MAX_ATTRIBUTION_HISTORY) {
            history.removeLast();
        }
    }

    private DamageAttribution resolveFromHistory(Player player, Deque<DamageAttribution> history, AttributionTimeouts timeouts) {
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        EntityDamageEvent.DamageCause cause = lastDamage == null ? EntityDamageEvent.DamageCause.CUSTOM : lastDamage.getCause();

        List<ResolveCategory> categories = switch (cause) {
            case ENTITY_SWEEP_ATTACK, THORNS, MAGIC, ENTITY_ATTACK -> List.of(ResolveCategory.PROJECTILE, ResolveCategory.AMBIENT);
            case FIRE_TICK, LAVA, HOT_FLOOR, FIRE -> List.of(ResolveCategory.AMBIENT, ResolveCategory.PROJECTILE);
            case FALL -> List.of(ResolveCategory.KNOCKBACK, ResolveCategory.PROJECTILE, ResolveCategory.AMBIENT);
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> List.of(ResolveCategory.EXPLOSION, ResolveCategory.PROJECTILE, ResolveCategory.AMBIENT);
            default -> List.of(ResolveCategory.PROJECTILE, ResolveCategory.AMBIENT);
        };

        return selectBestAttribution(history, categories.toArray(new ResolveCategory[0]), timeouts);
    }

    private DamageAttribution selectBestAttribution(Deque<DamageAttribution> history, ResolveCategory... categories) {
        return selectBestAttribution(history, categories, AttributionTimeouts.defaultTimeouts());
    }

    private DamageAttribution selectBestAttribution(Deque<DamageAttribution> history, ResolveCategory[] categories, AttributionTimeouts timeouts) {
        Instant now = Instant.now();
        return history.stream()
                .filter(entry -> entry.damager().isValid())
                .filter(entry -> isWithinTimeout(entry, now, timeouts))
                .filter(entry -> matchesCategory(entry, categories))
                .max(Comparator.comparing(DamageAttribution::timestamp))
                .orElse(null);
    }

    private boolean isWithinTimeout(DamageAttribution entry, Instant now, AttributionTimeouts timeouts) {
        Duration elapsed = Duration.between(entry.timestamp(), now);
        ResolveCategory category = toCategory(entry);
        Duration timeout = switch (category) {
            case PROJECTILE, EXPLOSION -> timeouts.projectile();
            case KNOCKBACK -> timeouts.knockback();
            case AMBIENT -> timeouts.ambient();
        };
        return elapsed.compareTo(timeout) <= 0;
    }

    private boolean matchesCategory(DamageAttribution entry, ResolveCategory[] categories) {
        ResolveCategory category = toCategory(entry);
        for (ResolveCategory value : categories) {
            if (category == value) {
                return true;
            }
        }
        return false;
    }

    private ResolveCategory toCategory(DamageAttribution entry) {
        return switch (entry.sourceType()) {
            case PROJECTILE, POTION, AREA_EFFECT_CLOUD -> ResolveCategory.PROJECTILE;
            case KNOCKBACK -> ResolveCategory.KNOCKBACK;
            case EXPLOSION -> ResolveCategory.EXPLOSION;
            default -> ResolveCategory.AMBIENT;
        };
    }

    private record DamageAttribution(LivingEntity damager, SourceType sourceType, EntityDamageEvent.DamageCause damageCause, Instant timestamp) {
        private DamageAttribution withSourceType(SourceType newSourceType) {
            return new DamageAttribution(damager, newSourceType, damageCause, Instant.now());
        }
    }

    public record ResolvedKiller(UUID entityUuid, LivingEntity entity, SourceType sourceType, EntityDamageEvent.DamageCause damageCause, Instant timestamp) {
    }

    public record AttributionTimeouts(Duration projectile, Duration knockback, Duration ambient) {
        public static AttributionTimeouts defaultTimeouts() {
            return new AttributionTimeouts(Duration.ofSeconds(20), Duration.ofSeconds(8), Duration.ofSeconds(15));
        }
    }

    private enum ResolveCategory { PROJECTILE, KNOCKBACK, AMBIENT, EXPLOSION }

    public enum SourceType {
        DIRECT,
        PROJECTILE,
        POTION,
        AREA_EFFECT_CLOUD,
        KNOCKBACK,
        EXPLOSION
    }
}
