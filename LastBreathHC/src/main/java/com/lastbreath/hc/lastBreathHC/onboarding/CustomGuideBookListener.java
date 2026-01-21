package com.lastbreath.hc.lastBreathHC.onboarding;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.Map;

public class CustomGuideBookListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }

        ItemStack guideBook = buildGuideBook();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(guideBook);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    private ItemStack buildGuideBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }

        meta.setTitle(ChatColor.GOLD + "Custom Features");
        meta.setAuthor(ChatColor.YELLOW + "LastBreathHC");
        meta.setLore(List.of(
                ChatColor.DARK_PURPLE + "Custom Features guide",
                ChatColor.GRAY + "Your lore-bound handbook."
        ));
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setPages(
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Welcome, survivor!" + ChatColor.RESET + "\n"
                        + ChatColor.GRAY + "This realm is forged with custom systems.\n\n"
                        + ChatColor.LIGHT_PURPLE + "• " + ChatColor.WHITE + "Hardcore balance tweaks\n"
                        + ChatColor.LIGHT_PURPLE + "• " + ChatColor.WHITE + "Unique mobs & scaling\n"
                        + ChatColor.LIGHT_PURPLE + "• " + ChatColor.WHITE + "World events & hazards\n\n"
                        + ChatColor.AQUA + "Study this guide to stay alive.",
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Custom Items" + ChatColor.RESET + "\n"
                        + ChatColor.GRAY + "Craft or discover powerful artifacts:\n\n"
                        + ChatColor.AQUA + "• " + ChatColor.WHITE + "Revive tokens restore allies\n"
                        + ChatColor.AQUA + "• " + ChatColor.WHITE + "Gracestones preserve gear\n"
                        + ChatColor.AQUA + "• " + ChatColor.WHITE + "Enhanced grindstones refine loot\n\n"
                        + ChatColor.DARK_GRAY + "Treat them with respect.",
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Events & Threats" + ChatColor.RESET + "\n"
                        + ChatColor.GRAY + "Stay alert for escalating dangers:\n\n"
                        + ChatColor.GREEN + "• " + ChatColor.WHITE + "Blood Moons intensify mobs\n"
                        + ChatColor.GREEN + "• " + ChatColor.WHITE + "Asteroids reshape terrain\n"
                        + ChatColor.GREEN + "• " + ChatColor.WHITE + "Environmental hazards strike\n\n"
                        + ChatColor.YELLOW + "Prepare, adapt, survive."
        );
        book.setItemMeta(meta);
        return book;
    }
}
