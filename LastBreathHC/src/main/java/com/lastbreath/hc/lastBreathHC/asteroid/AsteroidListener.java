package com.lastbreath.hc.lastBreathHC.asteroid;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AsteroidListener implements Listener {
    private static final long RETALIATION_TARGET_WINDOW_MILLIS = 15_000L;

    private final Map<UUID, Long> retaliatoryTargets = new HashMap<>();

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;

        Location loc = e.getClickedBlock().getLocation();

        if (!AsteroidManager.isAsteroid(loc)) return;

        e.setCancelled(true);

        AsteroidManager.AsteroidEntry entry = AsteroidManager.getEntry(loc);
        if (entry == null) return;

        if (hasActiveAsteroidMobs(loc, entry)) {
            e.getPlayer().sendMessage("§cDefeat the asteroid mobs before looting this crash site.");
            return;
        }

        Inventory inv = entry.inventory();
        e.getPlayer().openInventory(inv);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (AsteroidManager.isAsteroid(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cThis asteroid cannot be mined.");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (isInRestrictedZone(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cYou cannot build or break blocks near an active asteroid.");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();

        Location asteroidLoc = null;

        for (var entry : AsteroidManager.ASTEROIDS.entrySet()) {
            if (entry.getValue().inventory().equals(inv)) {
                asteroidLoc = entry.getKey();
                break;
            }
        }

        if (asteroidLoc == null) return;

        boolean empty = true;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                empty = false;
                break;
            }
        }


        if (empty) {
            if (e.getPlayer() instanceof Player player) {

                PlayerStats stats = StatsManager.get(player.getUniqueId());
                stats.asteroidLoots++;
                StatsManager.markDirty(player.getUniqueId());

                TitleManager.unlockTitle(
                        player,
                        Title.ASTEROID_HUNTER,
                        Title.ASTEROID_HUNTER.requirementDescription()
                );

                if (stats.asteroidLoots >= 25) {
                    TitleManager.unlockTitle(
                            player,
                            Title.RELIC_SEEKER,
                            Title.RELIC_SEEKER.requirementDescription()
                    );
                }


                if (stats.asteroidLoots >= 100) {
                    TitleManager.unlockTitle(
                            player,
                            Title.STAR_FORGER,
                            Title.STAR_FORGER.requirementDescription()
                    );
                }

                TitleManager.checkProgressTitles(player);
            }

            AsteroidManager.remove(asteroidLoc);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        retaliatoryTargets.remove(entity.getUniqueId());

        if (!entity.getScoreboardTags().contains(AsteroidManager.ASTEROID_MOB_TAG)) {
            return;
        }

        String asteroidKey = null;
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(AsteroidManager.ASTEROID_KEY_TAG_PREFIX)) {
                asteroidKey = tag.substring(AsteroidManager.ASTEROID_KEY_TAG_PREFIX.length());
                break;
            }
        }
        if (asteroidKey == null || asteroidKey.isBlank()) {
            return;
        }

        AsteroidManager.AsteroidEntry entry = AsteroidManager.getEntryByKey(asteroidKey);
        if (entry != null) {
            entry.mobs().remove(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onAsteroidMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (!isAsteroidMob(attacker)) {
            return;
        }

        LivingEntity target = event.getTarget();
        if (target == null) {
            attacker.removeScoreboardTag(AsteroidManager.ASTEROID_AGGRESSIVE_TAG);
            return;
        }

        if (isAsteroidMob(target)) {
            event.setCancelled(true);
            attacker.removeScoreboardTag(AsteroidManager.ASTEROID_AGGRESSIVE_TAG);
            return;
        }

        if (target instanceof Player || canRetaliateAgainst(target)) {
            attacker.addScoreboardTag(AsteroidManager.ASTEROID_AGGRESSIVE_TAG);
            return;
        }

        event.setCancelled(true);
        attacker.removeScoreboardTag(AsteroidManager.ASTEROID_AGGRESSIVE_TAG);
    }

    @EventHandler
    public void onAsteroidMobDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Entity damagerEntity = event.getDamager();
        LivingEntity attacker = null;
        if (damagerEntity instanceof LivingEntity livingDamager) {
            attacker = livingDamager;
        } else if (damagerEntity instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof LivingEntity livingShooter) {
            attacker = livingShooter;
        }
        if (attacker == null) {
            return;
        }
        if (isAsteroidMob(attacker) && isAsteroidMob(target)) {
            event.setCancelled(true);
            return;
        }
        if (isAsteroidMob(target) && damagerEntity instanceof org.bukkit.entity.Projectile) {
            event.setDamage(event.getDamage() * 0.75);
        }

        if (!isAsteroidMob(target) || isAsteroidMob(attacker)) {
            return;
        }

        if (!(attacker instanceof Player)) {
            markRetaliatoryTarget(attacker);
            if (target instanceof Mob mobTarget) {
                mobTarget.setTarget(attacker);
            }
        }

        String asteroidKeyTag = getAsteroidKeyTag(target);
        if (asteroidKeyTag == null || asteroidKeyTag.isBlank()) {
            return;
        }

        Player playerAttacker = null;
        if (damagerEntity instanceof Player playerDamager) {
            playerAttacker = playerDamager;
        } else if (damagerEntity instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player playerShooter) {
            playerAttacker = playerShooter;
        }

        if (playerAttacker == null) {
            return;
        }

        alertAsteroidPack(target, asteroidKeyTag, playerAttacker);
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent event) {
        Entity entity = event.getEntity();
        if (entity.getScoreboardTags().contains(AsteroidManager.ASTEROID_MOB_TAG)
                && (entity instanceof PiglinBrute || entity.getType() == EntityType.PIGLIN_BRUTE)) {
            event.setCancelled(true);
        }
    }

    private void markRetaliatoryTarget(LivingEntity attacker) {
        retaliatoryTargets.put(attacker.getUniqueId(), System.currentTimeMillis() + RETALIATION_TARGET_WINDOW_MILLIS);
    }

    private boolean canRetaliateAgainst(LivingEntity target) {
        Long expiry = retaliatoryTargets.get(target.getUniqueId());
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            retaliatoryTargets.remove(target.getUniqueId());
            return false;
        }
        return true;
    }

    private void alertAsteroidPack(LivingEntity target, String asteroidKeyTag, Player attacker) {
        String asteroidKey = asteroidKeyTag.substring(AsteroidManager.ASTEROID_KEY_TAG_PREFIX.length());
        AsteroidManager.AsteroidEntry entry = AsteroidManager.getEntryByKey(asteroidKey);
        if (entry == null || target.getWorld() == null) {
            return;
        }

        Set<UUID> tracked = entry.mobs();
        Set<UUID> stale = new HashSet<>();
        for (UUID uuid : tracked) {
            Entity entity = target.getWorld().getEntity(uuid);
            if (!(entity instanceof Mob mob) || mob.isDead()) {
                stale.add(uuid);
                continue;
            }
            mob.setTarget(attacker);
            mob.addScoreboardTag(AsteroidManager.ASTEROID_AGGRESSIVE_TAG);
        }
        if (!stale.isEmpty()) {
            tracked.removeAll(stale);
        }

        if (!tracked.isEmpty()) {
            return;
        }

        int radius = AsteroidManager.getMobLeashRadius();
        for (Entity entity : target.getWorld().getNearbyEntities(target.getLocation(), radius, 12, radius)) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (!isAsteroidMob(mob) || !mob.getScoreboardTags().contains(asteroidKeyTag)) {
                continue;
            }
            tracked.add(mob.getUniqueId());
            mob.setTarget(attacker);
            mob.addScoreboardTag(AsteroidManager.ASTEROID_AGGRESSIVE_TAG);
        }
    }

    private String getAsteroidKeyTag(LivingEntity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(AsteroidManager.ASTEROID_KEY_TAG_PREFIX)) {
                return tag;
            }
        }
        return null;
    }

    private boolean hasActiveAsteroidMobs(Location loc, AsteroidManager.AsteroidEntry entry) {
        Location blockLoc = loc.getBlock().getLocation();
        if (blockLoc.getWorld() == null) {
            return false;
        }
        Set<UUID> tracked = entry.mobs();
        if (!tracked.isEmpty()) {
            Set<UUID> toRemove = new HashSet<>();
            for (UUID uuid : tracked) {
                Entity entity = blockLoc.getWorld().getEntity(uuid);
                if (!(entity instanceof LivingEntity livingEntity) || livingEntity.isDead()) {
                    toRemove.add(uuid);
                    continue;
                }
                return true;
            }
            tracked.removeAll(toRemove);
        }

        String keyTag = AsteroidManager.ASTEROID_KEY_TAG_PREFIX + AsteroidManager.asteroidKey(blockLoc);
        int radius = 16;
        for (Entity entity : blockLoc.getWorld().getNearbyEntities(blockLoc, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            if (livingEntity.getScoreboardTags().contains(AsteroidManager.ASTEROID_MOB_TAG)
                    && livingEntity.getScoreboardTags().contains(keyTag)) {
                tracked.add(livingEntity.getUniqueId());
                return true;
            }
        }
        return false;
    }

    private boolean isAsteroidMob(LivingEntity entity) {
        return entity.getScoreboardTags().contains(AsteroidManager.ASTEROID_MOB_TAG);
    }

    private boolean isInRestrictedZone(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        int radius = AsteroidManager.getMobLeashRadius();
        for (Location asteroidLoc : AsteroidManager.ASTEROIDS.keySet()) {
            if (!asteroidLoc.getWorld().equals(blockLoc.getWorld())) {
                continue;
            }
            int dx = Math.abs(blockLoc.getBlockX() - asteroidLoc.getBlockX());
            int dz = Math.abs(blockLoc.getBlockZ() - asteroidLoc.getBlockZ());
            if (dx <= radius && dz <= radius) {
                return true;
            }
        }
        return false;
    }
}
