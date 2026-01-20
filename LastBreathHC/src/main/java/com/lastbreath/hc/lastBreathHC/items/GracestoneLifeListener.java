package com.lastbreath.hc.lastBreathHC.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Attribute;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class GracestoneLifeListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Gracestone.refreshDisplay(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        double remainingHealth = player.getHealth() - event.getFinalDamage();
        if (remainingHealth > 0) {
            return;
        }
        if (!Gracestone.consumeLife(player)) {
            return;
        }

        event.setCancelled(true);
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(maxHealth);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setNoDamageTicks(20);

        player.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.getLocation().add(0, 1, 0),
                60,
                0.6,
                0.8,
                0.6,
                0.1
        );
        player.getWorld().playSound(
                player.getLocation(),
                Sound.ITEM_TOTEM_USE,
                1.0f,
                1.0f
        );
        player.sendActionBar(Component.text("Grace saved you from death."));
    }
}
