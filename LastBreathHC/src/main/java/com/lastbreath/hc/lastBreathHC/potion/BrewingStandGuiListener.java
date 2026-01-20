package com.lastbreath.hc.lastBreathHC.potion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class BrewingStandGuiListener implements Listener {

    private static final String BREWING_TITLE = "Brewing Stand";
    private static final int BREWING_SLOT_COUNT = 5;
    private static final int INGREDIENT_SLOT = 3;

    private final JavaPlugin plugin;
    private final Map<UUID, BrewingSession> sessions = new HashMap<>();
    private final Map<Location, Set<UUID>> viewersByLocation = new HashMap<>();

    public BrewingStandGuiListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingStandInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BREWING_STAND) {
            return;
        }
        if (!(block.getState() instanceof BrewingStand brewingStand)) {
            return;
        }

        event.setCancelled(true);
        openBrewingStandGui(event.getPlayer(), brewingStand);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        BrewingSession session = getSession(event.getWhoClicked().getUniqueId());
        if (session == null) {
            return;
        }
        if (!isBrewingGui(event.getView().getTopInventory())) {
            return;
        }
        if (event.getClickedInventory() == event.getView().getTopInventory() && event.getSlot() == INGREDIENT_SLOT) {
            event.setCancelled(true);
            swapIngredientSlot(event);
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncToBrewer(session));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        BrewingSession session = getSession(event.getWhoClicked().getUniqueId());
        if (session == null) {
            return;
        }
        if (!isBrewingGui(event.getView().getTopInventory())) {
            return;
        }
        if (event.getRawSlots().contains(INGREDIENT_SLOT)) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncToBrewer(session));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BrewingSession session = sessions.remove(uuid);
        if (session == null) {
            return;
        }
        syncToBrewer(session);
        removeViewer(session.location(), uuid);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewComplete(BrewEvent event) {
        if (!(event.getContents().getHolder() instanceof BrewingStand brewingStand)) {
            return;
        }
        Location location = brewingStand.getLocation();
        Set<UUID> viewers = viewersByLocation.get(location);
        if (viewers == null || viewers.isEmpty()) {
            return;
        }
        for (UUID uuid : viewers) {
            BrewingSession session = sessions.get(uuid);
            if (session == null) {
                continue;
            }
            syncFromBrewer(session);
        }
    }

    private void openBrewingStandGui(Player player, BrewingStand brewingStand) {
        Location location = brewingStand.getLocation();
        BrewingSession existing = sessions.remove(player.getUniqueId());
        if (existing != null) {
            removeViewer(existing.location(), player.getUniqueId());
        }

        Inventory gui = Bukkit.createInventory(new BrewingStandGuiHolder(location), InventoryType.BREWING, BREWING_TITLE);
        BrewingSession session = new BrewingSession(location, gui);
        sessions.put(player.getUniqueId(), session);
        viewersByLocation.computeIfAbsent(location, key -> new HashSet<>()).add(player.getUniqueId());

        syncFromBrewer(session);
        player.openInventory(gui);
    }

    private void syncFromBrewer(BrewingSession session) {
        BrewerInventory brewerInventory = getBrewerInventory(session.location());
        if (brewerInventory == null) {
            return;
        }
        for (int slot = 0; slot < BREWING_SLOT_COUNT; slot++) {
            session.inventory().setItem(slot, cloneOrNull(brewerInventory.getItem(slot)));
        }
    }

    private void syncToBrewer(BrewingSession session) {
        BrewerInventory brewerInventory = getBrewerInventory(session.location());
        if (brewerInventory == null) {
            return;
        }
        for (int slot = 0; slot < BREWING_SLOT_COUNT; slot++) {
            brewerInventory.setItem(slot, cloneOrNull(session.inventory().getItem(slot)));
        }
    }

    private BrewerInventory getBrewerInventory(Location location) {
        Block block = location.getBlock();
        if (block.getType() != Material.BREWING_STAND) {
            return null;
        }
        if (!(block.getState() instanceof BrewingStand brewingStand)) {
            return null;
        }
        return brewingStand.getInventory();
    }

    private boolean isBrewingGui(Inventory inventory) {
        return inventory.getHolder() instanceof BrewingStandGuiHolder;
    }

    private BrewingSession getSession(UUID uuid) {
        BrewingSession session = sessions.get(uuid);
        if (session == null) {
            return null;
        }
        if (!isBrewingGui(session.inventory())) {
            sessions.remove(uuid);
            return null;
        }
        return session;
    }

    private ItemStack cloneOrNull(ItemStack item) {
        if (item == null) {
            return null;
        }
        return item.clone();
    }

    private void removeViewer(Location location, UUID uuid) {
        Set<UUID> viewers = viewersByLocation.get(location);
        if (viewers == null) {
            return;
        }
        viewers.remove(uuid);
        if (viewers.isEmpty()) {
            viewersByLocation.remove(location);
        }
    }

    private record BrewingSession(Location location, Inventory inventory) {
        private BrewingSession {
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(inventory, "inventory");
        }
    }

    private void swapIngredientSlot(InventoryClickEvent event) {
        ItemStack cursor = cloneOrNull(event.getCursor());
        ItemStack current = cloneOrNull(event.getCurrentItem());
        event.setCursor(current);
        event.getClickedInventory().setItem(INGREDIENT_SLOT, cursor);
    }

    private static final class BrewingStandGuiHolder implements InventoryHolder {
        private final Location location;

        private BrewingStandGuiHolder(Location location) {
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
