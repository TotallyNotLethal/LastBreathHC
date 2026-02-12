package com.lastbreath.hc.lastBreathHC.daily;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RandomDailyCosmeticRewardAction implements DailyRewardAction {

    private final DailyRewardManager dailyRewardManager;

    public RandomDailyCosmeticRewardAction(DailyRewardManager dailyRewardManager) {
        this.dailyRewardManager = dailyRewardManager;
    }

    @Override
    public String preview() {
        return ChatColor.AQUA + "• " + ChatColor.WHITE + "Random Daily Aura/Trail Unlock";
    }

    @Override
    public String grant(Player player) {
        DailyCosmeticType unlocked = dailyRewardManager.unlockRandomCosmetic(player.getUniqueId());
        if (unlocked == null) {
            player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, 16));
            return "All daily cosmetics unlocked (compensation: 16x Experience Bottle)";
        }
        return "Unlocked daily cosmetic: " + unlocked.displayName();
    }
}
