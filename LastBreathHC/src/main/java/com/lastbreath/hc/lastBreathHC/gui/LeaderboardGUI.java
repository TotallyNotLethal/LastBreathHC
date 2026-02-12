package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaderboardGUI implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 36;
    private static final String METRIC_TITLE = "Leaderboards";
    private static final String ENTRY_TITLE_PREFIX = "Leaderboard: ";

    public static void openMetricSelect(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, METRIC_TITLE);
        decorate(inventory);

        StatsManager.LeaderboardMetric[] metrics = StatsManager.LeaderboardMetric.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22};
        for (int i = 0; i < Math.min(metrics.length, slots.length); i++) {
            inventory.setItem(slots[i], buildMetricItem(metrics[i]));
        }

        player.openInventory(inventory);
    }

    public static void openEntries(Player player, StatsManager.LeaderboardMetric metric, int requestedPage) {
        List<StatsManager.LeaderboardEntry> entries = StatsManager.getLeaderboard(metric, Math.max(ENTRIES_PER_PAGE, requestedPage * ENTRIES_PER_PAGE));
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) ENTRIES_PER_PAGE));
        int page = Math.min(Math.max(requestedPage, 1), totalPages);

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, entryTitle(metric, page, totalPages));
        decorate(inventory);

        int start = (page - 1) * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());
        int slot = 0;
        for (int index = start; index < end; index++) {
            inventory.setItem(slot++, buildEntryItem(index + 1, entries.get(index), metric));
        }

        if (entries.isEmpty()) {
            inventory.setItem(22, buildInfo(Material.BARRIER, ChatColor.YELLOW + "No entries", List.of(ChatColor.GRAY + "No data available yet.")));
        }

        inventory.setItem(45, buildInfo(Material.COMPASS, ChatColor.GOLD + "Metrics", List.of(ChatColor.GRAY + "Return to category list")));
        if (page > 1) {
            inventory.setItem(48, buildInfo(Material.ARROW, ChatColor.YELLOW + "Previous", List.of(ChatColor.GRAY + "Go to page " + (page - 1))));
        }
        if (page < totalPages) {
            inventory.setItem(50, buildInfo(Material.ARROW, ChatColor.YELLOW + "Next", List.of(ChatColor.GRAY + "Go to page " + (page + 1))));
        }
        inventory.setItem(53, buildInfo(Material.BOOK, ChatColor.GOLD + metric.displayName(), List.of(
                ChatColor.GRAY + "Page " + page + "/" + totalPages,
                ChatColor.GRAY + "Entries: " + entries.size()
        )));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!METRIC_TITLE.equals(title) && !title.startsWith(ENTRY_TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) {
            return;
        }

        if (METRIC_TITLE.equals(title)) {
            StatsManager.LeaderboardMetric metric = parseMetric(meta.getDisplayName());
            if (metric != null) {
                openEntries(player, metric, 1);
            }
            return;
        }

        ViewData viewData = parseViewData(title);
        if (viewData == null) {
            return;
        }

        if (clicked.getType() == Material.COMPASS) {
            openMetricSelect(player);
            return;
        }

        String stripped = ChatColor.stripColor(meta.getDisplayName());
        if ("Previous".equalsIgnoreCase(stripped)) {
            openEntries(player, viewData.metric(), Math.max(1, viewData.page() - 1));
        } else if ("Next".equalsIgnoreCase(stripped)) {
            openEntries(player, viewData.metric(), viewData.page() + 1);
        }
    }

    private static ItemStack buildMetricItem(StatsManager.LeaderboardMetric metric) {
        Material material = switch (metric) {
            case PLAYTIME -> Material.CLOCK;
            case MOBS_KILLED -> Material.IRON_SWORD;
            case PLAYER_KILLS -> Material.DIAMOND_SWORD;
            case DEATHS -> Material.SKELETON_SKULL;
            case REVIVES -> Material.TOTEM_OF_UNDYING;
            case BLOCKS_MINED -> Material.DIAMOND_PICKAXE;
            case BLOCKS_PLACED -> Material.BRICKS;
            case RARE_ORES_MINED -> Material.DIAMOND_ORE;
            case CROPS_HARVESTED -> Material.WHEAT;
            case FISH_CAUGHT -> Material.FISHING_ROD;
            case ASTEROIDS_LOOTED -> Material.NETHER_STAR;
        };

        return buildInfo(material, ChatColor.GOLD + metric.displayName(), List.of(
                ChatColor.GRAY + "Click to view top players",
                ChatColor.DARK_GRAY + metric.key()
        ));
    }

    private static ItemStack buildEntryItem(int rank, StatsManager.LeaderboardEntry entry, StatsManager.LeaderboardMetric metric) {
        String value = metric == StatsManager.LeaderboardMetric.PLAYTIME
                ? StatsManager.formatTicks(entry.value())
                : String.valueOf(entry.value());
        String displayName = entry.banned()
                ? ChatColor.STRIKETHROUGH + entry.displayName() + ChatColor.RESET
                : entry.displayName();
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        SkullMeta meta = rawMeta instanceof SkullMeta ? (SkullMeta) rawMeta : null;
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid()));
            meta.setDisplayName(ChatColor.YELLOW + "#" + rank + " " + ChatColor.WHITE + displayName);
            meta.setLore(new ArrayList<>(List.of(
                ChatColor.GRAY + metric.displayName() + ": " + ChatColor.WHITE + value,
                entry.banned() ? ChatColor.RED + "Banned account" : ChatColor.DARK_GRAY + "Top survivor"
            )));
            item.setItemMeta(meta);
            return item;
        }

        return buildInfo(Material.PAPER, ChatColor.YELLOW + "#" + rank + " " + ChatColor.WHITE + displayName, List.of(
                ChatColor.GRAY + metric.displayName() + ": " + ChatColor.WHITE + value
        ));
    }

    private static ItemStack buildInfo(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static StatsManager.LeaderboardMetric parseMetric(String displayName) {
        String clean = ChatColor.stripColor(displayName);
        if (clean == null) {
            return null;
        }
        for (StatsManager.LeaderboardMetric metric : StatsManager.LeaderboardMetric.values()) {
            if (metric.displayName().equalsIgnoreCase(clean)) {
                return metric;
            }
        }
        return null;
    }

    private static String entryTitle(StatsManager.LeaderboardMetric metric, int page, int totalPages) {
        return ENTRY_TITLE_PREFIX + metric.key() + " (" + page + "/" + totalPages + ")";
    }

    private static ViewData parseViewData(String title) {
        String body = title.substring(ENTRY_TITLE_PREFIX.length());
        int space = body.indexOf(' ');
        if (space < 0) {
            return null;
        }

        StatsManager.LeaderboardMetric metric = StatsManager.LeaderboardMetric.fromInput(body.substring(0, space).toLowerCase(Locale.US));
        if (metric == null) {
            return null;
        }

        int open = body.indexOf('(');
        int slash = body.indexOf('/');
        int close = body.indexOf(')');
        if (open < 0 || slash < 0 || close < 0 || slash <= open) {
            return new ViewData(metric, 1);
        }

        try {
            int page = Integer.parseInt(body.substring(open + 1, slash));
            return new ViewData(metric, page);
        } catch (NumberFormatException ignored) {
            return new ViewData(metric, 1);
        }
    }

    private record ViewData(StatsManager.LeaderboardMetric metric, int page) {
    }

    private static void decorate(Inventory inventory) {
        ItemStack filler = buildInfo(Material.BLACK_STAINED_GLASS_PANE, ChatColor.BLACK + " ", List.of());
        int[] borderSlots = {
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36, 44,
                46, 47, 49, 51, 52
        };
        for (int slot : borderSlots) {
            inventory.setItem(slot, filler);
        }
    }
}
