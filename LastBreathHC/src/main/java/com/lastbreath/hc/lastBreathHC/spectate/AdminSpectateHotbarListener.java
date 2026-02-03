package com.lastbreath.hc.lastBreathHC.spectate;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.commands.SpectateCommand;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminSpectateHotbarListener implements Listener {

    private final LastBreathHC plugin;
    private final SpectateCommand spectateCommand;
    private final NamespacedKey toolKey;
    private final Map<Tool, ItemStack> toolItems = new EnumMap<>(Tool.class);

    public AdminSpectateHotbarListener(LastBreathHC plugin, SpectateCommand spectateCommand) {
        this.plugin = plugin;
        this.spectateCommand = spectateCommand;
        this.toolKey = new NamespacedKey(plugin, "admin_spectate_tool");
        for (Tool tool : Tool.values()) {
            toolItems.put(tool, tool.createItem(toolKey));
        }
    }

    public void applyHotbar(Player player) {
        int slot = 0;
        for (Tool tool : Tool.values()) {
            player.getInventory().setItem(slot, toolItems.get(tool).clone());
            slot++;
        }
        player.updateInventory();
    }

    @EventHandler
    public void onToolUse(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String toolId = container.get(toolKey, PersistentDataType.STRING);
        if (toolId == null) {
            return;
        }

        Player player = event.getPlayer();
        SpectateSession session = spectateCommand.getSession(player.getUniqueId());
        if (session == null || !session.isAdminSpectate()) {
            event.setCancelled(true);
            player.getInventory().remove(item);
            player.updateInventory();
            return;
        }

        Tool tool = Tool.fromId(toolId);
        if (tool == null) {
            return;
        }

        event.setCancelled(true);
        handleTool(player, tool);
    }

    private void handleTool(Player player, Tool tool) {
        switch (tool) {
            case PREVIOUS_TARGET -> cycleTarget(player, -1);
            case NEXT_TARGET -> cycleTarget(player, 1);
            case TOGGLE_POV -> toggleSpectatorTarget(player);
            case TELEPORT_TARGET -> teleportToTarget(player);
            case OPEN_ENDERCHEST -> openEnderChest(player);
            case VIEW_INVENTORY -> viewInventory(player);
            case SHOW_STATS -> showStats(player);
            case SHOW_LOCATION -> showLocation(player);
            case EXIT_SPECTATE -> player.performCommand("spectate leave");
        }
    }

    private void cycleTarget(Player viewer, int step) {
        List<Player> targets = getOnlineTargets(viewer);
        if (targets.isEmpty()) {
            viewer.sendMessage(ChatColor.RED + "No available players to spectate.");
            return;
        }

        Player current = getTargetPlayer(viewer);
        int index = current == null ? 0 : targets.indexOf(current);
        if (index < 0) {
            index = 0;
        }
        int nextIndex = Math.floorMod(index + step, targets.size());
        Player nextTarget = targets.get(nextIndex);
        setSpectatorTarget(viewer, nextTarget, "Now spectating " + nextTarget.getName() + ".");
    }

    private void toggleSpectatorTarget(Player viewer) {
        Entity current = viewer.getSpectatorTarget();
        if (current != null) {
            viewer.setSpectatorTarget(null);
            viewer.sendMessage(ChatColor.YELLOW + "Spectator POV cleared.");
            return;
        }

        List<Player> targets = getOnlineTargets(viewer);
        if (targets.isEmpty()) {
            viewer.sendMessage(ChatColor.RED + "No available players to spectate.");
            return;
        }
        Player target = targets.get(0);
        setSpectatorTarget(viewer, target, "Spectator POV set to " + target.getName() + ".");
    }

    private void teleportToTarget(Player viewer) {
        Player target = getTargetPlayer(viewer);
        if (target == null) {
            viewer.sendMessage(ChatColor.RED + "No spectate target selected.");
            return;
        }
        viewer.teleport(target.getLocation());
        viewer.sendMessage(ChatColor.AQUA + "Teleported to " + target.getName() + ".");
    }

    private void openEnderChest(Player viewer) {
        Player target = getTargetPlayer(viewer);
        if (target == null) {
            viewer.sendMessage(ChatColor.RED + "No spectate target selected.");
            return;
        }
        viewer.openInventory(target.getEnderChest());
        viewer.sendMessage(ChatColor.AQUA + "Viewing " + target.getName() + "'s ender chest.");
    }

    private void viewInventory(Player viewer) {
        Player target = getTargetPlayer(viewer);
        if (target == null) {
            viewer.sendMessage(ChatColor.RED + "No spectate target selected.");
            return;
        }
        viewer.openInventory(target.getInventory());
        viewer.sendMessage(ChatColor.AQUA + "Viewing " + target.getName() + "'s inventory.");
    }

    private void showStats(Player viewer) {
        Player target = getTargetPlayer(viewer);
        if (target == null) {
            viewer.sendMessage(ChatColor.RED + "No spectate target selected.");
            return;
        }
        PlayerStats stats = StatsManager.get(target.getUniqueId());
        viewer.sendMessage(ChatColor.GOLD + "Stats for " + target.getName() + ":");
        viewer.sendMessage(ChatColor.YELLOW + "Deaths: " + stats.deaths + " | Revives: " + stats.revives);
        viewer.sendMessage(ChatColor.YELLOW + "Blocks mined: " + stats.blocksMined + " | Rare ores: " + stats.rareOresMined);
        viewer.sendMessage(ChatColor.YELLOW + "Crops harvested: " + stats.cropsHarvested);
        viewer.sendMessage(ChatColor.YELLOW + "Time alive: " + formatDuration(stats.timeAlive));
    }

    private void showLocation(Player viewer) {
        Player target = getTargetPlayer(viewer);
        if (target == null) {
            viewer.sendMessage(ChatColor.RED + "No spectate target selected.");
            return;
        }
        Location location = target.getLocation();
        viewer.sendMessage(ChatColor.AQUA + "Target location: "
                + location.getWorld().getName()
                + " (" + location.getBlockX()
                + ", " + location.getBlockY()
                + ", " + location.getBlockZ() + ")");
    }

    private Player getTargetPlayer(Player viewer) {
        Entity target = viewer.getSpectatorTarget();
        if (target instanceof Player player) {
            return player;
        }
        return null;
    }

    private void setSpectatorTarget(Player viewer, Player target, String message) {
        viewer.setSpectatorTarget(target);
        viewer.teleport(target.getLocation());
        viewer.sendMessage(ChatColor.AQUA + message);
    }

    private List<Player> getOnlineTargets(Player viewer) {
        List<Player> targets = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getUniqueId().equals(viewer.getUniqueId())) {
                targets.add(player);
            }
        }
        targets.sort(Comparator.comparing(player -> player.getName().toLowerCase(Locale.ROOT)));
        return targets;
    }

    private String formatDuration(long timeAliveTicks) {
        long seconds = timeAliveTicks / 20L;
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long secs = duration.toSecondsPart();
        return String.format("%dh %dm %ds", hours, minutes, secs);
    }

    private enum Tool {
        PREVIOUS_TARGET("previous_target", Material.SPECTRAL_ARROW, "Previous Target", List.of("Cycle to the previous player.")),
        NEXT_TARGET("next_target", Material.ARROW, "Next Target", List.of("Cycle to the next player.")),
        TOGGLE_POV("toggle_pov", Material.ENDER_EYE, "Toggle POV", List.of("Toggle spectator POV on/off.")),
        TELEPORT_TARGET("teleport_target", Material.COMPASS, "Teleport to Target", List.of("Teleport to the selected target.")),
        OPEN_ENDERCHEST("open_enderchest", Material.ENDER_CHEST, "Open Ender Chest", List.of("View target ender chest.")),
        VIEW_INVENTORY("view_inventory", Material.CHEST, "View Inventory", List.of("View target inventory snapshot.")),
        SHOW_STATS("show_stats", Material.BOOK, "Show Stats", List.of("Display target stats.")),
        SHOW_LOCATION("show_location", Material.MAP, "Show Location", List.of("Display target coordinates.")),
        EXIT_SPECTATE("exit_spectate", Material.BARRIER, "Exit Spectate", List.of("Leave admin spectate."));

        private final String id;
        private final Material material;
        private final String displayName;
        private final List<String> lore;

        Tool(String id, Material material, String displayName, List<String> lore) {
            this.id = id;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }

        public ItemStack createItem(NamespacedKey toolKey) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + displayName);
            List<String> formattedLore = new ArrayList<>();
            for (String line : lore) {
                formattedLore.add(ChatColor.GRAY + line);
            }
            meta.setLore(formattedLore);
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
            return item;
        }

        public static Tool fromId(String id) {
            for (Tool tool : values()) {
                if (tool.id.equals(id)) {
                    return tool;
                }
            }
            return null;
        }
    }
}
