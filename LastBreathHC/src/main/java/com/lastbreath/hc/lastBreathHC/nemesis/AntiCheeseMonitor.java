package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiCheeseMonitor implements Listener {
    private final LastBreathHC plugin;
    private final CaptainEntityBinder binder;
    private final Map<String, Integer> losAbuse = new HashMap<>();
    private final int threshold;

    public AntiCheeseMonitor(LastBreathHC plugin, CaptainEntityBinder binder) {
        this.plugin = plugin;
        this.binder = binder;
        this.threshold = Math.max(3, plugin.getConfig().getInt("nemesis.antiCheese.losThreshold", 8));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        CaptainRecord record = binder.resolveCaptainRecord(living).orElse(null);
        if (record == null) {
            return;
        }

        String key = player.getUniqueId() + ":" + record.identity().captainUuid();
        if (!living.hasLineOfSight(player)) {
            int count = losAbuse.getOrDefault(key, 0) + 1;
            losAbuse.put(key, count);
            if (count >= threshold) {
                applySiegebreaker(living, player, record);
                losAbuse.put(key, 0);
            }
        } else {
            losAbuse.put(key, Math.max(0, losAbuse.getOrDefault(key, 0) - 1));
        }
    }

    private void applySiegebreaker(LivingEntity captain, Player player, CaptainRecord record) {
        captain.teleport(player.getLocation().clone().add(1.2, 0.0, 1.2));
        captain.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 8, 1, true, false, true));
        captain.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 8, 0, true, false, true));
        captain.getWorld().spawnParticle(Particle.CRIT, captain.getLocation(), 30, 0.4, 0.6, 0.4, 0.03);
        if (captain instanceof Mob mob) {
            mob.setTarget(player);
        }
        player.sendMessage("§c[SIEGEBREAKER] Your safe-spot has been breached.");
    }
}
