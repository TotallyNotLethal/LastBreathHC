package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.Title.TitleCategory;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TitlesGUI implements Listener {

    private static final String TITLE_PREFIX = "Titles";
    private static final int INVENTORY_SIZE = 54;
    private static final List<Integer> TITLE_SLOTS = Arrays.asList(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    );

    private final NamespacedKey titleKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey payloadKey;
    private final Map<UUID, GuiState> states = new HashMap<>();

    public TitlesGUI() {
        this.titleKey = new NamespacedKey(LastBreathHC.getInstance(), "title-id");
        this.actionKey = new NamespacedKey(LastBreathHC.getInstance(), "title-gui-action");
        this.payloadKey = new NamespacedKey(LastBreathHC.getInstance(), "title-gui-payload");
    }

    public void open(Player player) {
        GuiState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new GuiState());
        render(player, state);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || !current.hasItemMeta()) {
            return;
        }

        ItemMeta meta = current.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String action = container.get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            handleActionClick(player, action, container.get(payloadKey, PersistentDataType.STRING));
            return;
        }

        String titleId = container.get(titleKey, PersistentDataType.STRING);
        if (titleId == null) {
            return;
        }

        Title title = Title.fromInput(titleId);
        if (title == null) {
            return;
        }

        List<Title> unlockedTitles = TitleManager.getUnlockedTitles(player);
        boolean acquired = unlockedTitles.contains(title);
        if (!acquired) {
            player.sendMessage("§cMissing title: §7" + title.displayName() + "§c. How to earn: §e" + title.requirementDescription());
            return;
        }

        if (TitleManager.getEquippedTitle(player) == title) {
            player.sendMessage("§e" + title.displayName() + " is already equipped.");
            return;
        }

        if (!TitleManager.equipTitle(player, title)) {
            player.sendMessage("§cYou have not unlocked that title yet.");
            return;
        }

        player.sendMessage("§aEquipped title: " + title.displayName());
        open(player);
    }

    private void handleActionClick(Player player, String action, String payload) {
        GuiState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new GuiState());
        switch (action) {
            case "filter" -> {
                TitleCategory category = parseCategory(payload);
                state.category = category;
                state.page = 0;
            }
            case "next_page" -> state.page++;
            case "prev_page" -> state.page = Math.max(0, state.page - 1);
            default -> {
                return;
            }
        }
        render(player, state);
    }

    private void render(Player player, GuiState state) {
        List<Title> filteredTitles = getFilteredTitles(state.category);
        int maxPage = Math.max(0, (filteredTitles.size() - 1) / TITLE_SLOTS.size());
        if (state.page > maxPage) {
            state.page = maxPage;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                TITLE_PREFIX + " - " + state.category.label() + " (Page " + (state.page + 1) + "/" + (maxPage + 1) + ")");

        renderFilterButtons(inventory, state.category);

        Set<Title> unlockedTitles = new HashSet<>(TitleManager.getUnlockedTitles(player));
        Title equippedTitle = TitleManager.getEquippedTitle(player);

        int start = state.page * TITLE_SLOTS.size();
        for (int i = 0; i < TITLE_SLOTS.size(); i++) {
            int titleIndex = start + i;
            if (titleIndex >= filteredTitles.size()) {
                continue;
            }
            Title title = filteredTitles.get(titleIndex);
            boolean acquired = unlockedTitles.contains(title);
            inventory.setItem(TITLE_SLOTS.get(i), buildTitleItem(title, acquired, equippedTitle == title));
        }

        if (state.page > 0) {
            inventory.setItem(45, buildActionButton(Material.ARROW, ChatColor.GOLD + "Previous Page", "prev_page", ""));
        }
        if (state.page < maxPage) {
            inventory.setItem(53, buildActionButton(Material.ARROW, ChatColor.GOLD + "Next Page", "next_page", ""));
        }
        inventory.setItem(49, buildInfoItem(unlockedTitles.size(), filteredTitles.size(), state.category));

        player.openInventory(inventory);
    }

    private void renderFilterButtons(Inventory inventory, TitleCategory activeCategory) {
        addFilterButton(inventory, 1, TitleCategory.ALL, Material.COMPASS, activeCategory);
        addFilterButton(inventory, 2, TitleCategory.COMBAT, Material.IRON_SWORD, activeCategory);
        addFilterButton(inventory, 3, TitleCategory.PROGRESSION, Material.CLOCK, activeCategory);
        addFilterButton(inventory, 4, TitleCategory.BOSSES, Material.NETHER_STAR, activeCategory);
    }

    private void addFilterButton(Inventory inventory, int slot, TitleCategory category, Material material, TitleCategory activeCategory) {
        String displayName = (category == activeCategory ? ChatColor.GREEN : ChatColor.YELLOW) + category.label();
        ItemStack item = buildActionButton(material, displayName, "filter", category.name());
        inventory.setItem(slot, item);
    }

    private List<Title> getFilteredTitles(TitleCategory category) {
        return Arrays.stream(Title.values())
                .filter(title -> category == TitleCategory.ALL || title.category() == category)
                .sorted(Comparator.comparing(Title::displayName))
                .toList();
    }

    private ItemStack buildTitleItem(Title title, boolean acquired, boolean equipped) {
        Material material = acquired ? Material.NAME_TAG : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String titleColor = acquired ? ChatColor.AQUA.toString() : ChatColor.GRAY.toString();
        String displayName = titleColor + title.displayName();
        if (equipped) {
            displayName = ChatColor.GREEN + title.displayName() + ChatColor.GRAY + " (Equipped)";
        }
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add((acquired ? ChatColor.GREEN : ChatColor.RED) + "Status: " + (acquired ? "Acquired" : "Missing"));
        lore.add(ChatColor.GRAY + "Category: " + ChatColor.WHITE + title.category().label());
        lore.add(ChatColor.GOLD + "How to earn:");
        lore.add(ChatColor.YELLOW + title.requirementDescription());
        lore.add(" ");

        List<String> effects = TitleManager.getTitleEffects(title);
        if (effects.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "No title effects configured yet.");
        } else {
            lore.add(ChatColor.GRAY + "Current effects:");
            effects.forEach(effect -> lore.add(ChatColor.GREEN + "- " + effect));
        }

        lore.add(" ");
        if (!acquired) {
            lore.add(ChatColor.RED + "You have not unlocked this title yet.");
            lore.add(ChatColor.YELLOW + "Click for requirement reminder.");
        } else if (equipped) {
            lore.add(ChatColor.YELLOW + "Currently equipped.");
        } else {
            lore.add(ChatColor.GOLD + "Click to equip.");
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(titleKey, PersistentDataType.STRING, title.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildActionButton(Material material, String displayName, String action, String payload) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(payloadKey, PersistentDataType.STRING, payload == null ? "" : payload);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem(int unlockedCount, int visibleCount, TitleCategory category) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Title Overview");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Filter: " + ChatColor.WHITE + category.label());
        lore.add(ChatColor.GRAY + "Unlocked (total): " + ChatColor.GREEN + unlockedCount);
        lore.add(ChatColor.GRAY + "Titles in filter: " + ChatColor.YELLOW + visibleCount);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private TitleCategory parseCategory(String payload) {
        if (payload == null || payload.isBlank()) {
            return TitleCategory.ALL;
        }
        try {
            return TitleCategory.valueOf(payload);
        } catch (IllegalArgumentException ignored) {
            return TitleCategory.ALL;
        }
    }

    private static class GuiState {
        private TitleCategory category = TitleCategory.ALL;
        private int page = 0;
    }
}
