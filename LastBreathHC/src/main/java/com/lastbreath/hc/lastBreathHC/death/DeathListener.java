package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.bounty.BountyManager;
import com.lastbreath.hc.lastBreathHC.bounty.BountyRecord;
import com.lastbreath.hc.lastBreathHC.revive.ReviveStateManager;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import com.lastbreath.hc.lastBreathHC.token.ReviveTokenHelper;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.Map;

public class DeathListener implements Listener {

    private static final String REVIVE_INTERCEPT_METADATA = "lastbreathhc.reviveIntercept";

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreDeathDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!ReviveTokenHelper.hasToken(player)) {
            return;
        }

        double remainingHealth = player.getHealth() - event.getFinalDamage();
        if (remainingHealth > 0.0) {
            return;
        }

        if (!ReviveTokenHelper.consumeToken(player)) {
            return;
        }

        event.setCancelled(true);
        player.setMetadata(
                REVIVE_INTERCEPT_METADATA,
                new FixedMetadataValue(LastBreathHC.getInstance(), true)
        );

        player.setHealth(1.0);
        Location destination = player.getBedSpawnLocation() != null
                ? player.getBedSpawnLocation()
                : player.getWorld().getSpawnLocation();
        player.teleport(destination);

        Bukkit.broadcastMessage(
                "§6⚡ " + TitleManager.getTitleTag(player) + player.getName()
                        + " defied death! §7(" + formatDamageCause(player.getLastDamageCause()) + ")"
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata(REVIVE_INTERCEPT_METADATA, LastBreathHC.getInstance());
            }
        }.runTaskLater(LastBreathHC.getInstance(), 40L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasMetadata(REVIVE_INTERCEPT_METADATA)) {
            player.removeMetadata(REVIVE_INTERCEPT_METADATA, LastBreathHC.getInstance());
            return;
        }
        String deathMessage = event.getDeathMessage();
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        stats.deaths++;
        TitleManager.unlockTitle(player, Title.THE_FALLEN, "You have tasted defeat.");
        if (stats.deaths >= 3) {
            TitleManager.unlockTitle(player, Title.DEATH_DEFIER, "You keep fighting after repeated deaths.");
        }
        TitleManager.checkTimeBasedTitles(player);
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            handleBountyClaim(player, killer);
            BountyManager.createBounty(killer.getUniqueId());
        }

        boolean hasToken = ReviveTokenHelper.hasToken(player);

        // Stop vanilla behavior
        event.setDeathMessage(null);
        playGlobalDeathSound();
        if (hasToken) {
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
        } else {
            event.setKeepInventory(false);
            event.setKeepLevel(false);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (hasToken) {
                    if (!ReviveTokenHelper.consumeToken(player)) {
                        banPlayer(player, "Revival token missing at time of death.", deathMessage);
                        return;
                    }
                    triggerReviveFlow(player, deathMessage);
                } else {
                    banPlayer(player, "You died with no revival token.", deathMessage);
                }
            }
        }.runTaskLater(LastBreathHC.getInstance(), 1L);
    }

    private void triggerReviveFlow(Player player, String deathMessage) {
        player.setHealth(1.0);
        player.setGameMode(GameMode.SPECTATOR);

        // Global boom
        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_DEATH,
                1.5f,
                0.6f
        );

        Bukkit.broadcastMessage(
                "§4☠ " + TitleManager.getTitleTag(player) + player.getName() + " has fallen... §7("
                        + formatDeathReason(player, deathMessage) + ")"
        );
        applyRevive(player);
    }

    private void applyRevive(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        stats.revives++;
        TitleManager.unlockTitle(player, Title.REVIVED, "You returned from the brink.");
        if (stats.revives >= 3) {
            TitleManager.unlockTitle(player, Title.SOUL_RECLAIMER, "You have reclaimed your soul multiple times.");
        }

        player.setGameMode(GameMode.SURVIVAL);
        ReviveStateManager.markRevivePending(player.getUniqueId());
        player.teleport(
                player.getBedSpawnLocation() != null
                        ? player.getBedSpawnLocation()
                        : player.getWorld().getSpawnLocation()
        );

        Bukkit.broadcastMessage(
                "§6⚡ " + TitleManager.getTitleTag(player) + player.getName() + " defied death!"
        );
    }

    public static void banPlayer(Player player, String reason, String deathMessage) {
        Bukkit.broadcastMessage(
                "§4☠ " + TitleManager.getTitleTag(player) + player.getName()
                        + " has perished permanently. §7(" + formatDeathReason(player, deathMessage) + ")"
        );

        Bukkit.getBanList(BanList.Type.NAME)
                .addBan(player.getName(), reason, null, null);

        player.kickPlayer("You have died.\nNo revival token was used.");
    }

    private void handleBountyClaim(Player victim, Player killer) {
        BountyRecord record = BountyManager.claimBounty(
                victim.getUniqueId(),
                "Killed by " + killer.getName()
        );
        if (record == null) {
            return;
        }

        ItemStack reward = BountyManager.getRewardItemStack(record);
        if (reward == null) {
            return;
        }

        Map<Integer, ItemStack> leftover = killer.getInventory().addItem(reward);
        for (ItemStack item : leftover.values()) {
            killer.getWorld().dropItemNaturally(killer.getLocation(), item);
        }

        String targetName = record.targetName == null ? victim.getName() : record.targetName;
        String rewardLabel = reward.getAmount() + " " + formatMaterialName(reward.getType());
        Bukkit.broadcastMessage(
                "§6⚔ " + killer.getName() + " claimed the bounty on " + targetName + " for " + rewardLabel + "."
        );
        LastBreathHC.getInstance().getLogger().info(
                killer.getName() + " claimed the bounty on " + targetName + " for " + rewardLabel + "."
        );
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase(Locale.US).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(' ');
        }
        return builder.toString().trim();
    }

    private void playGlobalDeathSound() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }
    }

    private static String formatDamageCause(EntityDamageEvent event) {
        if (event == null) {
            return "Unknown cause";
        }

        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            if (byEntityEvent.getDamager() instanceof Player damager) {
                return "slain by " + damager.getName();
            }
            String name = byEntityEvent.getDamager().getType().name().toLowerCase(Locale.US)
                    .replace('_', ' ');
            return "slain by " + name;
        }

        String cause = event.getCause().name().toLowerCase(Locale.US).replace('_', ' ');
        return cause.isBlank() ? "Unknown cause" : cause;
    }

    private static String formatDeathReason(Player player, String deathMessage) {
        if (deathMessage == null || deathMessage.isBlank()) {
            return "Unknown cause";
        }
        String prefix = player.getName() + " ";
        if (deathMessage.startsWith(prefix)) {
            return deathMessage.substring(prefix.length()).trim();
        }
        return deathMessage.trim();
    }
}
