package com.lastbreath.hc.lastBreathHC.asteroid;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AsteroidListener implements Listener {

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;

        Location loc = e.getClickedBlock().getLocation();

        if (!AsteroidManager.isAsteroid(loc)) return;

        e.setCancelled(true);

        Inventory inv = AsteroidManager.getInventory(loc);
        if (inv != null) {
            e.getPlayer().openInventory(inv);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (isInRestrictedZone(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cYou cannot build or break blocks near an active asteroid.");
            return;
        }
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

    private boolean isInRestrictedZone(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        int radius = 16;
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
