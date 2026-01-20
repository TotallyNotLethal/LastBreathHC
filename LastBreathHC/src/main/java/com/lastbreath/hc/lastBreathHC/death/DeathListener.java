package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.bounty.BountyManager;
import com.lastbreath.hc.lastBreathHC.bounty.BountyRecord;
import com.lastbreath.hc.lastBreathHC.gui.ReviveGUI;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import com.lastbreath.hc.lastBreathHC.token.ReviveToken;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.Map;

public class DeathListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
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

        boolean hasToken = hasReviveToken(player);

        // Stop vanilla behavior
        event.setDeathMessage(null);
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
                    triggerReviveFlow(player);
                } else {
                    banPlayer(player, "You died with no revival token.");
                }
            }
        }.runTaskLater(LastBreathHC.getInstance(), 1L);
    }

    private boolean hasReviveToken(Player player) {
        for (ItemStack item : player.getInventory().getContents())
            if (ReviveToken.isToken(item)) return true;

        for (ItemStack item : player.getEnderChest().getContents())
            if (ReviveToken.isToken(item)) return true;

        return false;
    }

    private void triggerReviveFlow(Player player) {
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
                "§4☠ " + TitleManager.getTitleTag(player) + player.getName() + " has fallen..."
        );

        ReviveGUI.open(player);
    }

    public static void banPlayer(Player player, String reason) {
        Bukkit.broadcastMessage(
                "§4☠ " + TitleManager.getTitleTag(player) + player.getName() + " has perished permanently."
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
}
