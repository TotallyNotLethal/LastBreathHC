package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.daily.DailyClaimResult;
import com.lastbreath.hc.lastBreathHC.daily.DailyClaimStatus;
import com.lastbreath.hc.lastBreathHC.daily.DailyRewardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DailyRewardGUI implements Listener {

    private static final String TITLE = "Daily Rewards";
    private final DailyRewardManager dailyRewardManager;

    public DailyRewardGUI(DailyRewardManager dailyRewardManager) {
        this.dailyRewardManager = dailyRewardManager;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        inventory.setItem(11, buildTodayReward(player));
        inventory.setItem(13, buildClaimButton(player));
        inventory.setItem(15, buildStreakPreview(player));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() != 13) {
            return;
        }

        if (!dailyRewardManager.canClaimToday(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You already claimed your daily reward.");
            open(player);
            return;
        }

        DailyClaimResult result = dailyRewardManager.claim(player);
        if (result.status() == DailyClaimStatus.ALREADY_CLAIMED) {
            player.sendMessage(ChatColor.RED + "You already claimed your daily reward.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Daily reward claimed! Current streak: " + result.streak());
            if (result.status() == DailyClaimStatus.CLAIMED_STREAK_RESET) {
                player.sendMessage(ChatColor.YELLOW + "Your streak had reset because you missed a day.");
            }
            for (String reward : result.grantedRewards()) {
                player.sendMessage(ChatColor.GRAY + " + " + ChatColor.WHITE + reward);
            }
        }

        open(player);
    }

    private ItemStack buildTodayReward(Player player) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Today's Reward");
        meta.setLore(dailyRewardManager.getPreview(player));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildClaimButton(Player player) {
        boolean canClaim = dailyRewardManager.canClaimToday(player.getUniqueId());
        ItemStack item = new ItemStack(canClaim ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(canClaim ? ChatColor.GREEN + "Claim Reward" : ChatColor.RED + "Already Claimed");

        List<String> lore = new ArrayList<>();
        if (canClaim) {
            lore.add(ChatColor.GRAY + "Click to claim today's reward.");
        } else {
            lore.add(ChatColor.GRAY + "Come back after the next UTC day rollover.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildStreakPreview(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Streak Progress");

        List<String> preview = dailyRewardManager.getPreview(player);
        List<String> lore = new ArrayList<>();
        for (String line : preview) {
            if (line.contains("Upcoming milestones") || line.contains("Day ")) {
                lore.add(line);
            }
        }
        if (lore.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "No milestone data.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
