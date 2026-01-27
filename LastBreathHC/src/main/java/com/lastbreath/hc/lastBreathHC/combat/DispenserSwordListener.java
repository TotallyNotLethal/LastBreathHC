package com.lastbreath.hc.lastBreathHC.combat;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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
        if (!isSword(item.getType())) return;

        Block block = event.getBlock();
        if (!(block.getState() instanceof Dispenser dispenser)) return;
        if (!(block.getBlockData() instanceof Directional directional)) return;

        long now = System.currentTimeMillis();
        Long last = dispenserCooldowns.get(block);
        if (last != null && now - last < COOLDOWN_MS) {
            event.setCancelled(true);
            return;
        }
        dispenserCooldowns.put(block, now);

        event.setCancelled(true);

        Block targetBlock = block.getRelative(directional.getFacing());

        List<LivingEntity> targets = targetBlock.getWorld()
                .getNearbyLivingEntities(
                        targetBlock.getLocation().add(0.5, 0.5, 0.5),
                        0.6, 0.6, 0.6
                )
                .stream()
                .filter(e -> !e.isDead())
                .toList();

        if (targets.isEmpty()) return;

        LivingEntity target = targets.get(0);
        double damage = getSwordDamage(item.getType());

        // DAMAGE
        target.damage(damage);

        // MARK FOR XP DROP
        markDispenserSwordHit(target);

        // DAMAGE SWORD
        damageSword(dispenser, item);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        PersistentDataContainer data = entity.getPersistentDataContainer();

        if (!data.has(dispenserSwordKillKey, PersistentDataType.BYTE)) return;

        if (isMarkerStale(data)) {
            clearMarker(data);
            return;
        }

        // FORCE XP DROP
        int xp = event.getDroppedExp();
        if (xp <= 0) xp = 5;

        entity.getWorld().spawn(entity.getLocation(), ExperienceOrb.class)
                .setExperience(xp);

        event.setDroppedExp(0); // prevent double-drop
        clearMarker(data);
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

    private void damageSword(Dispenser dispenser, ItemStack dispensed) {
        Inventory inv = dispenser.getInventory();

        Optional<Integer> slot = findSlot(inv, dispensed);
        if (slot.isPresent() && applyDamage(inv, slot.get())) {
            return;
        }

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);

            if (stack == null) continue;
            if (!isSword(stack.getType())) continue;

            if (!(stack.getItemMeta() instanceof Damageable dmg)) continue;

            int newDamage = dmg.getDamage() + 1;

            if (newDamage >= stack.getType().getMaxDurability()) {
                inv.setItem(i, null); // break sword
            } else {
                dmg.setDamage(newDamage);
                stack.setItemMeta(dmg);
                inv.setItem(i, stack);
            }
            return; // only damage ONE sword
        }
    }

    private boolean applyDamage(Inventory inv, int slot) {
        ItemStack stack = inv.getItem(slot);
        if (stack == null) return false;
        if (!isSword(stack.getType())) return false;
        if (!(stack.getItemMeta() instanceof Damageable dmg)) return false;

        int newDamage = dmg.getDamage() + 1;

        if (newDamage >= stack.getType().getMaxDurability()) {
            inv.setItem(slot, null); // break sword
        } else {
            dmg.setDamage(newDamage);
            stack.setItemMeta(dmg);
            inv.setItem(slot, stack);
        }
        return true;
    }

    private Optional<Integer> findSlot(Inventory inv, ItemStack match) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.isSimilar(match)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private void markDispenserSwordHit(LivingEntity entity) {
        PersistentDataContainer data = entity.getPersistentDataContainer();
        data.set(dispenserSwordKillKey, PersistentDataType.BYTE, (byte) 1);
        data.set(dispenserSwordKillTimestampKey, PersistentDataType.LONG, System.currentTimeMillis());
    }

    private boolean isMarkerStale(PersistentDataContainer data) {
        Long ts = data.get(dispenserSwordKillTimestampKey, PersistentDataType.LONG);
        return ts != null && System.currentTimeMillis() - ts > KILL_MARKER_TTL_MS;
    }

    private void clearMarker(PersistentDataContainer data) {
        data.remove(dispenserSwordKillKey);
        data.remove(dispenserSwordKillTimestampKey);
    }
}
