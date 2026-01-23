package com.lastbreath.hc.lastBreathHC.combat;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DispenserSwordListener implements Listener {

    private static final long COOLDOWN_MS = 100L;
    private static final long KILL_MARKER_TTL_MS = 10_000L;
    private final Map<Block, Long> dispenserCooldowns = new ConcurrentHashMap<>();
    private final NamespacedKey dispenserSwordKillKey;
    private final NamespacedKey dispenserSwordKillTimestampKey;

    public DispenserSwordListener(LastBreathHC plugin) {
        this.dispenserSwordKillKey = new NamespacedKey(plugin, "dispenser_sword_kill");
        this.dispenserSwordKillTimestampKey = new NamespacedKey(plugin, "dispenser_sword_kill_ts");
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (!isSword(item.getType())) {
            return;
        }

        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Directional directional)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastFire = dispenserCooldowns.get(block);
        if (lastFire != null && now - lastFire < COOLDOWN_MS) {
            event.setCancelled(true);
            return;
        }
        dispenserCooldowns.put(block, now);

        event.setCancelled(true);

        Block targetBlock = block.getRelative(directional.getFacing());
        BoundingBox searchBox = targetBlock.getBoundingBox().expand(0.1);
        List<LivingEntity> targets = targetBlock.getWorld().getNearbyLivingEntities(searchBox).stream()
                .filter(entity -> !entity.isDead())
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        LivingEntity target = targets.get(0);
        double damage = getSwordDamage(item.getType());
        target.damage(damage);
        markDispenserSwordHit(target);

        if (block.getState() instanceof Dispenser dispenser) {
            reduceDurability(dispenser.getInventory(), item);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (!container.has(dispenserSwordKillKey, PersistentDataType.BYTE)) {
            return;
        }

        if (isMarkerStale(container)) {
            clearMarker(container);
            return;
        }

        int expToDrop = entity.getExpToDrop();
        if (expToDrop <= 0) {
            expToDrop = event.getDroppedExp();
        }
        event.setDroppedExp(expToDrop);
        clearMarker(container);
    }

    private boolean isSword(Material material) {
        return material.name().endsWith("_SWORD");
    }

    private double getSwordDamage(Material material) {
        return switch (material) {
            case WOODEN_SWORD, GOLDEN_SWORD -> 4.0;
            case STONE_SWORD -> 5.0;
            case IRON_SWORD -> 6.0;
            case DIAMOND_SWORD -> 7.0;
            case NETHERITE_SWORD -> 8.0;
            default -> 1.0;
        };
    }

    private void reduceDurability(Inventory inventory, ItemStack template) {
        Optional<Integer> slot = findMatchingSlot(inventory, template);
        if (slot.isEmpty()) {
            return;
        }

        ItemStack stored = inventory.getItem(slot.get());
        if (stored == null) {
            return;
        }

        ItemMeta meta = stored.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int newDamage = damageable.getDamage() + 1;
        if (newDamage >= stored.getType().getMaxDurability()) {
            inventory.setItem(slot.get(), null);
            return;
        }

        damageable.setDamage(newDamage);
        stored.setItemMeta(meta);
        inventory.setItem(slot.get(), stored);
    }

    private Optional<Integer> findMatchingSlot(Inventory inventory, ItemStack template) {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.isSimilar(template)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private void markDispenserSwordHit(LivingEntity target) {
        PersistentDataContainer container = target.getPersistentDataContainer();
        container.set(dispenserSwordKillKey, PersistentDataType.BYTE, (byte) 1);
        container.set(dispenserSwordKillTimestampKey, PersistentDataType.LONG, System.currentTimeMillis());
    }

    private boolean isMarkerStale(PersistentDataContainer container) {
        Long timestamp = container.get(dispenserSwordKillTimestampKey, PersistentDataType.LONG);
        if (timestamp == null) {
            return false;
        }
        return System.currentTimeMillis() - timestamp > KILL_MARKER_TTL_MS;
    }

    private void clearMarker(PersistentDataContainer container) {
        container.remove(dispenserSwordKillKey);
        container.remove(dispenserSwordKillTimestampKey);
    }
}
