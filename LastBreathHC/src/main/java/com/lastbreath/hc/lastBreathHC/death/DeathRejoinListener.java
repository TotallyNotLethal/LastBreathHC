package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathRejoinListener implements Listener {

    private static final String ADMIN_SPECTATE_METADATA = "lastbreathhc.adminSpectate";

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        forceSurvival(player);
        if (!player.isDead()) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isDead()) {
                    player.spigot().respawn();
                }
                forceSurvival(player);
            }
        }.runTaskLater(LastBreathHC.getInstance(), 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        forceSurvival(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() != GameMode.SPECTATOR) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isOp() || player.hasMetadata(ADMIN_SPECTATE_METADATA)) {
            return;
        }

        event.setCancelled(true);
        forceSurvival(player);
    }

    private void forceSurvival(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
}
