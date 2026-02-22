package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NemesisCaptainListGUI implements Listener {
    private static final String TITLE_PREFIX = "Nemesis Captains ";
    private static final int INVENTORY_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 36;
    private static final double MAX_DISTANCE = 5000.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;

    private final CaptainRegistry registry;
    private final CaptainEntityBinder captainEntityBinder;

    public NemesisCaptainListGUI(CaptainRegistry registry, CaptainEntityBinder captainEntityBinder) {
        this.registry = registry;
        this.captainEntityBinder = captainEntityBinder;
    }

    public void open(Player player) {
        open(player, 1);
    }

    private void open(Player player, int requestedPage) {
        List<NearbyCaptain> captains = nearbyCaptains(player);
        int totalPages = Math.max(1, (int) Math.ceil(captains.size() / (double) ENTRIES_PER_PAGE));
        int page = Math.max(1, Math.min(totalPages, requestedPage));

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title(page, totalPages));
        decorate(inventory);

        int start = (page - 1) * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, captains.size());
        int slot = 0;
        for (int index = start; index < end; index++) {
            inventory.setItem(slot++, buildCaptainItem(captains.get(index)));
        }

        if (captains.isEmpty()) {
            inventory.setItem(22, infoItem(Material.BARRIER, ChatColor.YELLOW + "No captains in range", List.of(
                    ChatColor.GRAY + "No nemesis captain found within 5000 blocks."
            )));
        }

        inventory.setItem(45, infoItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu")));
        if (page > 1) {
            inventory.setItem(48, infoItem(Material.ARROW, ChatColor.YELLOW + "Previous", List.of(ChatColor.GRAY + "Page " + (page - 1))));
        }
        if (page < totalPages) {
            inventory.setItem(50, infoItem(Material.ARROW, ChatColor.YELLOW + "Next", List.of(ChatColor.GRAY + "Page " + (page + 1))));
        }
        inventory.setItem(53, infoItem(Material.COMPASS, ChatColor.GOLD + "Nearby Captains", List.of(
                ChatColor.GRAY + "Within: 5000 blocks",
                ChatColor.GRAY + "Showing: " + captains.size(),
                ChatColor.GRAY + "Page " + page + "/" + totalPages
        )));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) {
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

        String clickedName = ChatColor.stripColor(meta.getDisplayName());
        int page = parsePage(title);
        if ("Close".equalsIgnoreCase(clickedName)) {
            player.closeInventory();
            return;
        }
        if ("Previous".equalsIgnoreCase(clickedName)) {
            open(player, Math.max(1, page - 1));
            return;
        }
        if ("Next".equalsIgnoreCase(clickedName)) {
            open(player, page + 1);
        }
    }

    private List<NearbyCaptain> nearbyCaptains(Player player) {
        Location playerLocation = player.getLocation();
        return registry.getAll().stream()
                .map(record -> resolveNearby(record, playerLocation))
                .filter(captain -> captain != null && captain.distanceSquared() <= MAX_DISTANCE_SQUARED)
                .sorted(Comparator.comparingDouble(NearbyCaptain::distanceSquared))
                .toList();
    }

    private NearbyCaptain resolveNearby(CaptainRecord record, Location playerLocation) {
        LivingEntity liveEntity = captainEntityBinder.resolveLiveKillerEntity(record);
        if (liveEntity != null && liveEntity.isValid() && liveEntity.getWorld().equals(playerLocation.getWorld())) {
            return new NearbyCaptain(record, liveEntity.getLocation(), liveEntity.getType(), true,
                    liveEntity.getLocation().distanceSquared(playerLocation));
        }

        if (record.origin() == null || record.origin().world() == null) {
            return null;
        }
        if (!record.origin().world().equals(playerLocation.getWorld().getName())) {
            return null;
        }

        Location origin = new Location(playerLocation.getWorld(), record.origin().spawnX(), record.origin().spawnY(), record.origin().spawnZ());
        return new NearbyCaptain(record, origin, EntityType.ZOMBIE, false, origin.distanceSquared(playerLocation));
    }

    private ItemStack buildCaptainItem(NearbyCaptain nearbyCaptain) {
        Material icon = iconFor(nearbyCaptain.entityType());
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        CaptainRecord record = nearbyCaptain.record();
        String name = record.naming() == null ? "Nemesis Captain" : record.naming().displayName();
        int level = record.progression() == null ? 1 : record.progression().level();
        List<String> traits = record.traits() == null ? List.of() : record.traits().traits();
        String primaryTrait = traits.isEmpty() ? "Unknown" : capitalize(traits.get(0));

        meta.setDisplayName(ChatColor.GOLD + name + ChatColor.GRAY + " (" + toRoman(level) + ")");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Distance: " + ChatColor.WHITE + (int) Math.sqrt(nearbyCaptain.distanceSquared()));
        lore.add(ChatColor.GRAY + "Trait: " + ChatColor.WHITE + primaryTrait + ChatColor.GRAY + " (" + toRoman(level) + ")");
        lore.add(ChatColor.GRAY + "Status: " + (nearbyCaptain.active() ? ChatColor.GREEN + "Active" : ChatColor.YELLOW + "Dormant"));
        lore.add(ChatColor.DARK_GRAY + "Mob: " + prettyEntityName(nearbyCaptain.entityType()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material iconFor(EntityType entityType) {
        if (entityType == null) {
            return Material.ZOMBIE_HEAD;
        }
        Material spawnEgg = Material.matchMaterial(entityType.name() + "_SPAWN_EGG");
        return spawnEgg == null ? Material.ZOMBIE_HEAD : spawnEgg;
    }

    private static ItemStack infoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void decorate(Inventory inventory) {
        ItemStack filler = infoItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + " ", List.of());
        int[] borderSlots = {
                36, 37, 38, 39, 40, 41, 42, 43, 44,
                46, 47, 49, 51, 52
        };
        for (int slot : borderSlots) {
            inventory.setItem(slot, filler);
        }
    }

    private String title(int page, int totalPages) {
        return TITLE_PREFIX + "(" + page + "/" + totalPages + ")";
    }

    private int parsePage(String title) {
        int open = title.indexOf('(');
        int slash = title.indexOf('/');
        int close = title.indexOf(')');
        if (open < 0 || slash < 0 || close < 0 || slash <= open) {
            return 1;
        }
        try {
            return Integer.parseInt(title.substring(open + 1, slash));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private String prettyEntityName(EntityType entityType) {
        return capitalize(entityType == null ? "UNKNOWN" : entityType.name().replace('_', ' ').toLowerCase(Locale.ROOT));
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        String[] words = value.replace('_', ' ').trim().split("\\s+");
        StringBuilder output = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!output.isEmpty()) {
                output.append(' ');
            }
            output.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return output.toString();
    }

    private String toRoman(int number) {
        int value = Math.max(1, Math.min(3999, number));
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (value >= values[i]) {
                value -= values[i];
                output.append(numerals[i]);
            }
        }
        return output.toString();
    }

    private record NearbyCaptain(CaptainRecord record, Location location, EntityType entityType, boolean active,
                                 double distanceSquared) {
    }
}
