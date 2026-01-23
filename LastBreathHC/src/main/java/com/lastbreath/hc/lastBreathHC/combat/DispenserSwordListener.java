package com.lastbreath.hc.lastBreathHC.combat;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DispenserSwordListener implements Listener {

    private static final long COOLDOWN_MS = 100L;
    private final Map<Block, Long> dispenserCooldowns = new ConcurrentHashMap<>();

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

        if (block.getState() instanceof Dispenser dispenser) {
            reduceDurability(dispenser.getInventory(), item);
        }
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
}
