package com.lastbreath.hc.lastBreathHC.daily;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;

public class ItemRewardAction implements DailyRewardAction {

    private final Material material;
    private final int amount;

    public ItemRewardAction(Material material, int amount) {
        this.material = material;
        this.amount = Math.max(1, amount);
    }

    @Override
    public String preview() {
        return ChatColor.GOLD + "• " + ChatColor.WHITE + amount + "x " + friendlyName(material);
    }

    @Override
    public String grant(Player player) {
        ItemStack stack = new ItemStack(material, amount);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        return amount + "x " + friendlyName(material);
    }

    private static String friendlyName(Material material) {
        String[] words = material.name().toLowerCase(Locale.US).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }
}
