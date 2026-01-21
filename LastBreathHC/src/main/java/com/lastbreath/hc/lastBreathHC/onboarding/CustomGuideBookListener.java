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
        meta.addPages(
                "§6§lLastBreathHC Guide§r\n"
                        + "§7Welcome to a harsher world.\n"
                        + "§7Each page maps the lore.\n\n"
                        + "§eRead on to survive.",
                "§6§lAsteroid Events§r\n"
                        + "§7Celestial impacts crash\n"
                        + "§7into marked worlds.\n\n"
                        + "§e• §fFight tagged mobs\n"
                        + "§e• §fLoot the core\n"
                        + "§e• §fLeashed elites linger",
                "§6§lBlood Moon§r\n"
                        + "§7Nightmares rise when\n"
                        + "§7the moon turns red.\n\n"
                        + "§e• §fMobs gain buffs\n"
                        + "§e• §fScaling spikes\n"
                        + "§e• §fSurvive the dawn",
                "§6§lMob Scaling§r\n"
                        + "§7Hardcore mobs adapt\n"
                        + "§7to your progress.\n\n"
                        + "§e• §fBonus stats\n"
                        + "§e• §fDanger builds\n"
                        + "§e• §fBlood Moon synergy",
                "§6§lSoulbound Death§r\n"
                        + "§7Fallen players drop\n"
                        + "§7a head with soul data.\n\n"
                        + "§e• §fEnder chest stored\n"
                        + "§e• §fRevive via token\n"
                        + "§e• §fOne soul per head",
                "§6§lRevive Tokens§r\n"
                        + "§7Tokens fuel revival\n"
                        + "§7and name-based restores.\n\n"
                        + "§e• §fConsume to revive\n"
                        + "§e• §fGUI prompts a name\n"
                        + "§e• §fFailure bans remain",
                "§6§lBounties§r\n"
                        + "§7Wanted souls appear\n"
                        + "§7on the bounty board.\n\n"
                        + "§e• §fTimed contracts\n"
                        + "§e• §fClaims on death\n"
                        + "§e• §fBroadcast rewards",
                "§6§lPotions & Brews§r\n"
                        + "§7Soul fire cauldrons\n"
                        + "§7brew custom effects.\n\n"
                        + "§e• §fUnique buffs/debuffs\n"
                        + "§e• §fEffect triggers\n"
                        + "§e• §fPotion IDs tracked",
                "§6§lEnvironment§r\n"
                        + "§7The world bites back\n"
                        + "§7with scaling hazards.\n\n"
                        + "§e• §fDistance scaling\n"
                        + "§e• §fFire & potion decay\n"
                        + "§e• §fBiome tuning"
        );
        book.setItemMeta(meta);
        return book;
    }
}
