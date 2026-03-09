package com.lastbreath.hc.lastBreathHC.onboarding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
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
import java.util.concurrent.ThreadLocalRandom;

public class CustomGuideBookListener implements Listener {

    private static final int FIRST_JOIN_SPAWN_RADIUS = 1000;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sendWelcomeMessage(player);

        if (!player.hasPlayedBefore()) {
            teleportFirstJoinSpawn(player);
            giveGuideBook(player);
        }
    }

    private void sendWelcomeMessage(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.RED + "Welcome to " + ChatColor.GOLD + "Last Breath");
        player.sendMessage(ChatColor.GRAY + "Hardcore world: death has consequences.");
        player.sendMessage(ChatColor.GRAY + "Respawns exist via crafted revive loot from asteroid events.");
        sendLastDeathCoordinates(player);
        player.sendMessage(ChatColor.RED + "/discord " + ChatColor.GRAY + "- Join our Discord");
        player.sendMessage(ChatColor.DARK_RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendLastDeathCoordinates(Player player) {
        if (player.getStatistic(Statistic.DEATHS) <= 0) {
            return;
        }
        Location lastDeathLocation = safeLastDeathLocation(player);
        if (lastDeathLocation == null || lastDeathLocation.getWorld() == null) {
            return;
        }

        player.sendMessage(
                ChatColor.GRAY + "Last death: "
                        + ChatColor.RED + lastDeathLocation.getBlockX()
                        + ChatColor.DARK_GRAY + ", "
                        + ChatColor.RED + lastDeathLocation.getBlockY()
                        + ChatColor.DARK_GRAY + ", "
                        + ChatColor.RED + lastDeathLocation.getBlockZ()
                        + ChatColor.DARK_GRAY + " ("
                        + ChatColor.GRAY + lastDeathLocation.getWorld().getName()
                        + ChatColor.DARK_GRAY + ")"
        );
    }

    private Location safeLastDeathLocation(Player player) {
        try {
            return player.getLastDeathLocation();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void teleportFirstJoinSpawn(Player player) {
        World world = player.getWorld();
        int x = randomCoordinate();
        int z = randomCoordinate();
        int y = world.getHighestBlockYAt(x, z) + 1;
        Location spawn = new Location(world, x + 0.5, y, z + 0.5);
        player.teleport(spawn);
    }

    private int randomCoordinate() {
        return ThreadLocalRandom.current().nextInt(-FIRST_JOIN_SPAWN_RADIUS, FIRST_JOIN_SPAWN_RADIUS + 1);
    }

    private void giveGuideBook(Player player) {

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

        meta.setTitle("Custom Features");
        meta.setAuthor("Last Breath HC");
        meta.lore(List.of(
                Component.text("Custom Features guide", NamedTextColor.DARK_PURPLE),
                Component.text("Your lore-bound handbook.", NamedTextColor.GRAY)
        ));
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addPages(
                Component.text(
                        """
                                §6Last Breath Links

                                §0For plugin info and
                                §0custom server changes,
                                §0visit:
                                §9https://www.lastbreath.net/

                                §0Join our Discord with:
                                §c/discord
                                §0Direct invite:
                                §9https://discord.com/invite/msZcMxmzyH"""
                )
        );

        book.setItemMeta(meta);
        return book;
    }
}
