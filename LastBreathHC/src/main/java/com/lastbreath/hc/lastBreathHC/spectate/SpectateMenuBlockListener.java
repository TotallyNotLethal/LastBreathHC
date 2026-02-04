package com.lastbreath.hc.lastBreathHC.spectate;

import com.lastbreath.hc.lastBreathHC.commands.SpectateCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpectateMenuBlockListener implements Listener {

    private final SpectateCommand spectateCommand;

    public SpectateMenuBlockListener(SpectateCommand spectateCommand) {
        this.spectateCommand = spectateCommand;
    }

    @EventHandler
    public void onSpectatorMenuInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        SpectateSession session = spectateCommand.getSession(player.getUniqueId());
        if (session == null || session.isAdminSpectate()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        event.setCancelled(true);
    }
}
