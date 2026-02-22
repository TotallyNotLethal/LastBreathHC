package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class NemesisCaptainListGUI implements Listener {
    private static final String TITLE_PREFIX = "Nemesis Captains ";
    private static final String DETAIL_TITLE_PREFIX = "Captain Detail ";
    private static final int INVENTORY_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 36;
    private static final double MAX_DISTANCE = 5000.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    private static final String CAPTAIN_MARKER = "captain:";
    private static final String PAGE_MARKER = "page:";

    private final CaptainRegistry registry;
    private final CaptainEntityBinder captainEntityBinder;
    private final CaptainTraitRegistry traitRegistry;

    public NemesisCaptainListGUI(CaptainRegistry registry, CaptainEntityBinder captainEntityBinder, CaptainTraitRegistry traitRegistry) {
        this.registry = registry;
        this.captainEntityBinder = captainEntityBinder;
        this.traitRegistry = traitRegistry;
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
        if (title.startsWith(TITLE_PREFIX)) {
            onListClick(event, player, title);
            return;
        }

        if (title.startsWith(DETAIL_TITLE_PREFIX)) {
            onDetailClick(event, player);
        }
    }

    private void onListClick(InventoryClickEvent event, Player player, String title) {
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
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
            return;
        }

        UUID captainId = readCaptainUuid(meta);
        if (captainId == null) {
            return;
        }
        CaptainRecord record = registry.getByCaptainUuid(captainId);
        if (record == null) {
            player.sendMessage(ChatColor.RED + "Captain data is no longer available.");
            open(player, page);
            return;
        }

        NearbyCaptain nearbyCaptain = resolveNearby(record, player.getLocation());
        if (nearbyCaptain == null) {
            nearbyCaptain = fallbackNearby(record, player.getLocation());
        }
        int totalPages = Math.max(1, (int) Math.ceil(nearbyCaptains(player).size() / (double) ENTRIES_PER_PAGE));
        openCaptainDetail(player, nearbyCaptain, page, totalPages);
    }

    private void onDetailClick(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
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
        if ("Back to Nearby List".equalsIgnoreCase(clickedName)) {
            int page = readMarkerInteger(meta, PAGE_MARKER, 1);
            open(player, page);
            return;
        }

        if ("Close".equalsIgnoreCase(clickedName)) {
            player.closeInventory();
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
        return new NearbyCaptain(record, origin, resolveEntityType(record), false, origin.distanceSquared(playerLocation));
    }

    private NearbyCaptain fallbackNearby(CaptainRecord record, Location playerLocation) {
        EntityType type = resolveEntityType(record);
        CaptainRecord.Origin originData = record.origin();
        Location fallback = playerLocation;
        if (originData != null && originData.world() != null && playerLocation.getWorld().getName().equals(originData.world())) {
            fallback = new Location(playerLocation.getWorld(), originData.spawnX(), originData.spawnY(), originData.spawnZ());
        }
        return new NearbyCaptain(record, fallback, type, false, fallback.distanceSquared(playerLocation));
    }

    private EntityType resolveEntityType(CaptainRecord record) {
        if (record == null || record.naming() == null || record.naming().aliasSeed() == null) {
            return EntityType.ZOMBIE;
        }
        try {
            return EntityType.valueOf(record.naming().aliasSeed().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return EntityType.ZOMBIE;
        }
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
        List<String> strengths = record.traits() == null ? List.of() : record.traits().traits();
        List<String> weaknesses = record.traits() == null ? List.of() : record.traits().weaknesses();

        meta.setDisplayName(ChatColor.GOLD + name + ChatColor.GRAY + " (" + toRoman(level) + ")");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Distance: " + ChatColor.WHITE + (int) Math.sqrt(nearbyCaptain.distanceSquared()));
        lore.add(ChatColor.GRAY + "Strengths: " + ChatColor.WHITE + localize(strengths));
        lore.add(ChatColor.GRAY + "Weaknesses: " + ChatColor.RED + localize(weaknesses));
        lore.add(ChatColor.GRAY + "Status: " + (nearbyCaptain.active() ? ChatColor.GREEN + "Active" : ChatColor.YELLOW + "Dormant"));
        lore.add(ChatColor.DARK_GRAY + "Mob: " + prettyEntityName(nearbyCaptain.entityType()));
        if (record.identity() != null && record.identity().captainId() != null) {
            lore.add(ChatColor.BLACK + CAPTAIN_MARKER + record.identity().captainId());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openCaptainDetail(Player player, NearbyCaptain nearbyCaptain, int sourcePage, int totalPages) {
        CaptainRecord record = nearbyCaptain.record();
        String name = record.naming() == null ? "Nemesis Captain" : record.naming().displayName();
        Inventory detail = Bukkit.createInventory(null, INVENTORY_SIZE, DETAIL_TITLE_PREFIX + name);
        decorate(detail);

        detail.setItem(4, infoItem(iconFor(nearbyCaptain.entityType()), ChatColor.GOLD + name, List.of(
                ChatColor.GRAY + "Type: " + ChatColor.WHITE + prettyEntityName(nearbyCaptain.entityType()),
                ChatColor.GRAY + "Status: " + (nearbyCaptain.active() ? ChatColor.GREEN + "Active" : ChatColor.YELLOW + "Dormant"),
                ChatColor.GRAY + "Distance: " + ChatColor.WHITE + (int) Math.sqrt(nearbyCaptain.distanceSquared()) + " blocks"
        )));

        int level = record.progression() == null ? 1 : record.progression().level();
        String tier = record.progression() == null || record.progression().tier() == null ? "Unknown" : capitalize(record.progression().tier());
        String owner = ownerName(record.identity() == null ? null : record.identity().nemesisOf());
        detail.setItem(10, infoItem(Material.NETHER_STAR, ChatColor.AQUA + "Progression", List.of(
                ChatColor.GRAY + "Level: " + ChatColor.WHITE + level + ChatColor.DARK_GRAY + " (" + toRoman(level) + ")",
                ChatColor.GRAY + "Tier: " + ChatColor.WHITE + tier,
                ChatColor.GRAY + "Nemesis Owner: " + ChatColor.WHITE + owner
        )));

        detail.setItem(11, infoItem(Material.BOOK, ChatColor.GREEN + "Traits", List.of(
                ChatColor.GRAY + "Full Traits:",
                ChatColor.WHITE + listLines(record.traits() == null ? List.of() : record.traits().traits())
        )));
        detail.setItem(12, infoItem(Material.FERMENTED_SPIDER_EYE, ChatColor.RED + "Weaknesses", List.of(
                ChatColor.GRAY + "Known Weaknesses:",
                ChatColor.RED + listLines(record.traits() == null ? List.of() : record.traits().weaknesses())
        )));
        detail.setItem(13, infoItem(Material.SHIELD, ChatColor.YELLOW + "Immunities", List.of(
                ChatColor.GRAY + "Known Immunities:",
                ChatColor.YELLOW + listLines(record.traits() == null ? List.of() : record.traits().immunities())
        )));

        LivingEntity liveEntity = captainEntityBinder.resolveLiveKillerEntity(record);
        if (liveEntity != null && liveEntity.isValid()) {
            detail.setItem(19, infoItem(Material.IRON_CHESTPLATE, ChatColor.BLUE + "Armor", equipmentLines(
                    "Helmet", liveEntity.getEquipment() == null ? null : liveEntity.getEquipment().getHelmet(),
                    "Chestplate", liveEntity.getEquipment() == null ? null : liveEntity.getEquipment().getChestplate(),
                    "Leggings", liveEntity.getEquipment() == null ? null : liveEntity.getEquipment().getLeggings(),
                    "Boots", liveEntity.getEquipment() == null ? null : liveEntity.getEquipment().getBoots()
            )));
            detail.setItem(20, infoItem(Material.DIAMOND_SWORD, ChatColor.BLUE + "Hands / Tools", equipmentLines(
                    "Main Hand", liveEntity.getEquipment() == null ? null : liveEntity.getEquipment().getItemInMainHand(),
                    "Off Hand", liveEntity.getEquipment() == null ? null : liveEntity.getEquipment().getItemInOffHand()
            )));
        } else {
            detail.setItem(19, infoItem(Material.IRON_CHESTPLATE, ChatColor.BLUE + "Armor", List.of(
                    ChatColor.GRAY + "No live entity available.",
                    ChatColor.DARK_GRAY + "Helmet: Unknown",
                    ChatColor.DARK_GRAY + "Chestplate: Unknown",
                    ChatColor.DARK_GRAY + "Leggings: Unknown",
                    ChatColor.DARK_GRAY + "Boots: Unknown"
            )));
            detail.setItem(20, infoItem(Material.DIAMOND_SWORD, ChatColor.BLUE + "Hands / Tools", List.of(
                    ChatColor.GRAY + "No live entity available.",
                    ChatColor.DARK_GRAY + "Main Hand: Unknown",
                    ChatColor.DARK_GRAY + "Off Hand: Unknown"
            )));
        }

        detail.setItem(21, dormantOriginItem(record, nearbyCaptain));

        detail.setItem(45, infoItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu")));
        detail.setItem(48, infoItem(Material.ARROW, ChatColor.YELLOW + "Back to Nearby List", List.of(
                ChatColor.GRAY + "Return to page " + sourcePage + " of " + totalPages,
                ChatColor.BLACK + PAGE_MARKER + sourcePage
        )));

        player.openInventory(detail);
    }

    private ItemStack dormantOriginItem(CaptainRecord record, NearbyCaptain nearbyCaptain) {
        CaptainRecord.Origin origin = record.origin();
        boolean dormant = !nearbyCaptain.active();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + (origin == null || origin.world() == null ? "Unknown" : origin.world()));
        lore.add(ChatColor.GRAY + "Spawn: " + ChatColor.WHITE + formatOrigin(origin));
        lore.add(ChatColor.GRAY + "Biome: " + ChatColor.WHITE + (origin == null || origin.biome() == null ? "Unknown" : capitalize(origin.biome())));
        if (record.state() != null && record.state().lastSeenEpochMs() > 0) {
            lore.add(ChatColor.GRAY + "Last Seen (ms): " + ChatColor.WHITE + record.state().lastSeenEpochMs());
        }
        if (dormant) {
            lore.add(ChatColor.YELLOW + "Dormant captain: showing stored record snapshot.");
        }
        return infoItem(Material.MAP, ChatColor.GOLD + "Origin / Last Known Info", lore);
    }

    private List<String> equipmentLines(String... keyValuePairs) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            String key = keyValuePairs[i];
            String value = keyValuePairs[i + 1];
            lines.add(ChatColor.GRAY + key + ": " + ChatColor.WHITE + value);
        }
        return lines;
    }

    private List<String> equipmentLines(String firstLabel, ItemStack first, String secondLabel, ItemStack second) {
        return equipmentLines(
                firstLabel, describeItem(first),
                secondLabel, describeItem(second)
        );
    }

    private List<String> equipmentLines(String firstLabel, ItemStack first, String secondLabel, ItemStack second,
                                        String thirdLabel, ItemStack third, String fourthLabel, ItemStack fourth) {
        return equipmentLines(
                firstLabel, describeItem(first),
                secondLabel, describeItem(second),
                thirdLabel, describeItem(third),
                fourthLabel, describeItem(fourth)
        );
    }

    private String describeItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return "None";
        }
        ItemMeta meta = itemStack.getItemMeta();
        String itemName = (meta != null && meta.hasDisplayName()) ? ChatColor.stripColor(meta.getDisplayName()) : capitalize(itemStack.getType().name());
        if (meta instanceof Damageable damageable && itemStack.getType().getMaxDurability() > 0) {
            int remaining = itemStack.getType().getMaxDurability() - damageable.getDamage();
            return itemName + " (" + Math.max(0, remaining) + " durability)";
        }
        return itemName;
    }

    private String formatOrigin(CaptainRecord.Origin origin) {
        if (origin == null) {
            return "Unknown";
        }
        return "x=" + (int) origin.spawnX() + ", y=" + (int) origin.spawnY() + ", z=" + (int) origin.spawnZ();
    }

    private String listLines(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "None";
        }
        return ids.stream().map(traitRegistry::displayName).collect(java.util.stream.Collectors.joining(", "));
    }

    private String ownerName(UUID ownerId) {
        if (ownerId == null) {
            return "Unknown";
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerId);
        if (offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        }
        return ownerId.toString();
    }

    private UUID readCaptainUuid(ItemMeta meta) {
        String value = readMarker(meta, CAPTAIN_MARKER);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int readMarkerInteger(ItemMeta meta, String marker, int fallback) {
        String value = readMarker(meta, marker);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String readMarker(ItemMeta meta, String marker) {
        if (meta == null || meta.getLore() == null) {
            return null;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.startsWith(marker)) {
                return stripped.substring(marker.length());
            }
        }
        return null;
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


    private String localize(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "None";
        }
        return ids.stream().map(traitRegistry::displayName).limit(2).collect(java.util.stream.Collectors.joining(", "));
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
