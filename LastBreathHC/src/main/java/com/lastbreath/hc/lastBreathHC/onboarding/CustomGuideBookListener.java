package com.lastbreath.hc.lastBreathHC.onboarding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        meta.setTitle(String.valueOf(Component.text("Custom Features", NamedTextColor.GOLD)));
        meta.setAuthor(String.valueOf(Component.text("Last Breath HC", NamedTextColor.GOLD)));
        meta.lore(List.of(
                Component.text("Custom Features guide", NamedTextColor.DARK_PURPLE),
                Component.text("Your lore-bound handbook.", NamedTextColor.GRAY)
        ));
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addPages(
                Component.text(
                        """
                                §6LastBreathHC Guide
                                
                                §0Welcome to a harsher world.
                                §0Each page maps the lore.
                                
                                §8Read on to survive."""
                ),

                Component.text(
                        """
                                §6Asteroid Events
                                
                                §0Celestial impacts crash
                                §0into marked worlds.
                                
                                §8• Fight tagged mobs
                                §8• Loot the core
                                §8• Elites linger"""
                ),

                Component.text(
                        """
                                §6Custom Enchants
                                
                                §0Asteroid-only pages bind
                                §0into a 6-page recipe.
                                §0Anvil cost: 30 levels.
                                
                                §8• Netherite tools only
                                §8• Not for PvP"""
                ),

                Component.text(
                        """
                                §6Blood Moon
                                
                                §0Nightmares rise when
                                §0the moon turns red.
                                
                                §8• Mobs gain buffs
                                §8• Scaling spikes
                                §8• Survive the dawn"""
                ),

                Component.text(
                        """
                                §6Mob Scaling
                                
                                §0Hardcore mobs adapt
                                §0to your progress.
                                
                                §8• Bonus stats
                                §8• Danger builds
                                §8• Blood Moon synergy"""
                ),

                Component.text(
                        """
                                §6Soulbound Death
                                
                                §0Fallen players drop
                                §0a head with soul data.
                                
                                §8• Ender chest stored
                                §8• Revive via token
                                §8• One soul per head"""
                ),

                Component.text(
                        """
                                §6Revive Tokens
                                
                                §0Tokens fuel revival
                                §0and name-based restores.
                                
                                §8• Consume to revive
                                §8• GUI prompts a name
                                §8• Failure bans remain"""
                ),

                Component.text(
                        """
                                §6Bounties
                                
                                §0Wanted souls appear
                                §0on the bounty board.
                                
                                §8• Timed contracts
                                §8• Claims on death
                                §8• Broadcast rewards"""
                ),

                Component.text(
                        """
                                §6Potions & Brews
                                
                                §0Soul fire cauldrons
                                §0brew custom effects.
                                
                                §8• Unique buffs
                                §8• Effect triggers
                                §8• Potion tracking"""
                ),

                Component.text(
                        """
                                §6Environment
                                
                                §0The world bites back
                                §0with scaling hazards.
                                
                                §8• Distance scaling
                                §8• Fire decay
                                §8• Biome tuning"""
                )
        );

        book.setItemMeta(meta);
        return book;
    }
}
