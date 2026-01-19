package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.bounty.BountyManager;
import com.lastbreath.hc.lastBreathHC.bounty.BountyRecord;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BountyBoardGUI implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 45;
    private static final String TITLE_BASE = "Bounty Board";
    private static final String PREVIOUS_LABEL = "§ePrevious Page";
    private static final String NEXT_LABEL = "§eNext Page";

    public static void open(Player player) {
        open(player, 1);
    }

    public static void open(Player player, int requestedPage) {
        List<BountyRecord> records = new ArrayList<>(BountyManager.getBounties().values());
        records.sort(Comparator
                .comparingInt((BountyRecord record) -> record.rewardTier).reversed()
                .thenComparing(record -> record.targetName == null ? "" : record.targetName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil(records.size() / (double) ENTRIES_PER_PAGE));
        int page = Math.min(Math.max(requestedPage, 1), totalPages);

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, titleFor(page, totalPages));

        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, records.size());
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(slot++, buildEntry(records.get(i)));
        }

        if (records.isEmpty()) {
            inventory.setItem(22, buildEmptyItem());
        }

        if (page > 1) {
            inventory.setItem(45, buildNavItem(Material.ARROW, PREVIOUS_LABEL));
        }
        if (page < totalPages) {
            inventory.setItem(53, buildNavItem(Material.ARROW, NEXT_LABEL));
        }

        inventory.setItem(49, buildInfoItem(records.size(), page, totalPages));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_BASE)) {
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

        int[] pageData = parsePage(title);
        int currentPage = pageData[0];
        int totalPages = pageData[1];

        if (clicked.getType() == Material.ARROW) {
            if (PREVIOUS_LABEL.equals(meta.getDisplayName())) {
                open(player, Math.max(1, currentPage - 1));
            } else if (NEXT_LABEL.equals(meta.getDisplayName())) {
                open(player, Math.min(totalPages, currentPage + 1));
            }
        }
    }

    private static ItemStack buildEntry(BountyRecord record) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta && record.targetUuid != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(record.targetUuid));
            meta = skullMeta;
        }

        String targetName = record.targetName == null ? "Unknown" : record.targetName;
        meta.setDisplayName(ChatColor.GOLD + targetName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Time accrued: " + ChatColor.WHITE + formatHours(record));

        ItemStack rewardStack = BountyManager.getRewardItemStack(record);
        if (rewardStack != null) {
            lore.add(ChatColor.GRAY + "Reward: " + ChatColor.GOLD + formatReward(rewardStack));
        } else {
            lore.add(ChatColor.GRAY + "Reward: " + ChatColor.RED + "Unknown");
        }

        lore.add(ChatColor.GRAY + "Value: " + ChatColor.WHITE + formatValue(record.rewardValue));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildEmptyItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "No bounties posted");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Check back later for targets.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildNavItem(Material material, String label) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(label);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildInfoItem(int totalEntries, int page, int totalPages) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Bounty Board");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Targets: " + ChatColor.WHITE + totalEntries);
        lore.add(ChatColor.GRAY + "Page: " + ChatColor.WHITE + page + "/" + totalPages);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String titleFor(int page, int totalPages) {
        return TITLE_BASE + " (Page " + page + "/" + totalPages + ")";
    }

    private static int[] parsePage(String title) {
        int page = 1;
        int total = 1;
        int start = title.indexOf("Page ");
        if (start >= 0) {
            int slash = title.indexOf('/', start);
            int end = title.indexOf(')', start);
            if (slash > start && end > slash) {
                try {
                    page = Integer.parseInt(title.substring(start + 5, slash));
                    total = Integer.parseInt(title.substring(slash + 1, end));
                } catch (NumberFormatException ignored) {
                    page = 1;
                    total = 1;
                }
            }
        }
        return new int[]{page, total};
    }

    private static String formatHours(BountyRecord record) {
        long totalSeconds = record.accumulatedOnlineSeconds + (record.accumulatedOnlineTicks / 20L);
        long totalTicks = totalSeconds * 20L;
        double inGameHours = totalTicks / (double) BountyManager.IN_GAME_HOUR_TICKS;
        return String.format(Locale.US, "%.1fh", inGameHours);
    }

    private static String formatReward(ItemStack rewardStack) {
        String materialName = rewardStack.getType().name().toLowerCase(Locale.US).replace('_', ' ');
        String[] words = materialName.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
            if (i < words.length - 1) {
                builder.append(' ');
            }
        }
        return rewardStack.getAmount() + " " + builder;
    }

    private static String formatValue(double value) {
        return String.format(Locale.US, "%.1fh", value);
    }
}
