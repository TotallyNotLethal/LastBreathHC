package com.lastbreath.hc.lastBreathHC.environment;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;

public class AnvilCrushListener implements Listener {

    private static final String CONFIG_ROOT = "anvilCrush";
    private static final String CONFIG_RADIUS = CONFIG_ROOT + ".radius";
    private static final String CONFIG_MAPPING = CONFIG_ROOT + ".mapping";
    private static final double DEFAULT_RADIUS = 0.75;

    private final LastBreathHC plugin;
    private final NamespacedKey lastCrushTickKey;
    private final Map<Material, Material> crushMap;

    public AnvilCrushListener(LastBreathHC plugin) {
        this.plugin = plugin;
        this.lastCrushTickKey = new NamespacedKey(plugin, "anvil_crush_tick");
        this.crushMap = loadCrushMap();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) {
            return;
        }

        if (!isAnvilMaterial(fallingBlock.getBlockData().getMaterial())) {
            return;
        }

        double radius = plugin.getConfig().getDouble(CONFIG_RADIUS, DEFAULT_RADIUS);
        if (radius <= 0.0 || crushMap.isEmpty()) {
            return;
        }

        Location impactLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        long tick = impactLocation.getWorld().getFullTime();
        double radiusSquared = radius * radius;

        for (Entity entity : impactLocation.getWorld().getNearbyEntities(impactLocation, radius, radius, radius,
                candidate -> candidate instanceof Item)) {
            if (entity.getLocation().distanceSquared(impactLocation) > radiusSquared) {
                continue;
            }

            Item item = (Item) entity;
            if (wasCrushedThisTick(item, tick)) {
                continue;
            }

            ItemStack stack = item.getItemStack();
            Material next = crushMap.get(stack.getType());
            if (next == null) {
                continue;
            }

            ItemStack updated = stack.clone();
            updated.setType(next);
            item.setItemStack(updated);
            markCrushed(item, tick);
        }
    }

    private boolean isAnvilMaterial(Material material) {
        return material == Material.ANVIL
                || material == Material.CHIPPED_ANVIL
                || material == Material.DAMAGED_ANVIL;
    }

    private boolean wasCrushedThisTick(Item item, long tick) {
        PersistentDataContainer container = item.getPersistentDataContainer();
        Long lastTick = container.get(lastCrushTickKey, PersistentDataType.LONG);
        return lastTick != null && lastTick == tick;
    }

    private void markCrushed(Item item, long tick) {
        item.getPersistentDataContainer().set(lastCrushTickKey, PersistentDataType.LONG, tick);
    }

    private Map<Material, Material> loadCrushMap() {
        Map<Material, Material> mapping = new EnumMap<>(Material.class);
        if (!plugin.getConfig().isConfigurationSection(CONFIG_MAPPING)) {
            mapping.put(Material.COBBLESTONE, Material.GRAVEL);
            mapping.put(Material.GRAVEL, Material.SAND);
            return mapping;
        }

        for (String key : plugin.getConfig().getConfigurationSection(CONFIG_MAPPING).getKeys(false)) {
            Material from = Material.matchMaterial(key);
            if (from == null) {
                continue;
            }
            String targetName = plugin.getConfig().getString(CONFIG_MAPPING + "." + key);
            if (targetName == null) {
                continue;
            }
            Material to = Material.matchMaterial(targetName);
            if (to == null) {
                continue;
            }
            mapping.put(from, to);
        }
        return mapping;
    }
}
