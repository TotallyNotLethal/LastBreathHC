package com.lastbreath.hc.lastBreathHC.asteroid;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

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
        if (AsteroidManager.isAsteroid(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cThis asteroid cannot be mined.");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();

        Location asteroidLoc = null;

        for (var entry : AsteroidManager.ASTEROIDS.entrySet()) {
            if (entry.getValue().equals(inv)) {
                asteroidLoc = entry.getKey();
                break;
            }
        }

        if (asteroidLoc == null) return;

        boolean empty = true;
        for (var item : inv.getContents()) {
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
            }

            AsteroidManager.remove(asteroidLoc);
        }
    }
}
