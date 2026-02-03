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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AsteroidListener implements Listener {
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

                TitleManager.unlockTitle(
                        player,
                        Title.ASTEROID_HUNTER,
                        "You looted your first asteroid."
                );

                if (stats.asteroidLoots >= 25) {
                    TitleManager.unlockTitle(
                            player,
                            Title.RELIC_SEEKER,
                            "You have looted multiple asteroids."
                    );
                }


                if (stats.asteroidLoots >= 100) {
                    TitleManager.unlockTitle(
                            player,
                            Title.STAR_FORGER,
                            "You have mastered asteroid hunting."
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
        LivingEntity target = event.getTarget();
        if (isAsteroidMob(attacker)) {
            if (target instanceof Player) {
                attacker.addScoreboardTag(AsteroidManager.ASTEROID_AGGRESSIVE_TAG);
            } else {
                attacker.removeScoreboardTag(AsteroidManager.ASTEROID_AGGRESSIVE_TAG);
            }
        }
        if (target != null && isAsteroidMob(attacker) && isAsteroidMob(target)) {
            event.setCancelled(true);
        }
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
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent event) {
        Entity entity = event.getEntity();
        if (entity.getScoreboardTags().contains(AsteroidManager.ASTEROID_MOB_TAG)
                && (entity instanceof PiglinBrute || entity.getType() == EntityType.PIGLIN_BRUTE)) {
            event.setCancelled(true);
        }
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
