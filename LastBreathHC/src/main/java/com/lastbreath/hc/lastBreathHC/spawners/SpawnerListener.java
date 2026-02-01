package com.lastbreath.hc.lastBreathHC.spawners;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerListener implements Listener {

    private final NamespacedKey playerPlacedSpawnerKey;

    public SpawnerListener(LastBreathHC plugin) {
        this.playerPlacedSpawnerKey = new NamespacedKey(plugin, SpawnerTags.PLAYER_PLACED_SPAWNER_KEY);
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!isSilkTouchPickaxe(tool)) {
            return;
        }

        BlockState state = event.getBlock().getState();
        if (!(state instanceof CreatureSpawner spawner)) {
            return;
        }

        ItemStack spawnerItem = createSpawnerItem(spawner);
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), spawnerItem);
    }

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item == null) {
            return;
        }

        if (!(item.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
            return;
        }

        BlockState metaState = blockStateMeta.getBlockState();
        if (!(metaState instanceof CreatureSpawner spawnerMeta)) {
            return;
        }

        BlockState placedState = event.getBlockPlaced().getState();
        if (!(placedState instanceof CreatureSpawner placedSpawner)) {
            return;
        }

        applySpawnerData(spawnerMeta, placedSpawner);
        placedSpawner.getPersistentDataContainer().set(playerPlacedSpawnerKey, PersistentDataType.BYTE, (byte) 1);
        placedSpawner.update();
    }

    private boolean isSilkTouchPickaxe(ItemStack tool) {
        if (tool == null) {
            return false;
        }

        Material type = tool.getType();
        if (type != Material.DIAMOND_PICKAXE && type != Material.NETHERITE_PICKAXE) {
            return false;
        }

        return tool.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    private ItemStack createSpawnerItem(CreatureSpawner spawner) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        CreatureSpawner spawnerState = (CreatureSpawner) meta.getBlockState();

        applySpawnerData(spawner, spawnerState);
        meta.setBlockState(spawnerState);
        item.setItemMeta(meta);
        return item;
    }

    private void applySpawnerData(CreatureSpawner source, CreatureSpawner target) {
        target.setSpawnedType(source.getSpawnedType());
        target.setDelay(source.getDelay());
        target.setMinSpawnDelay(source.getMinSpawnDelay());
        target.setMaxSpawnDelay(source.getMaxSpawnDelay());
        target.setSpawnCount(source.getSpawnCount());
        target.setMaxNearbyEntities(source.getMaxNearbyEntities());
        target.setRequiredPlayerRange(source.getRequiredPlayerRange());
        target.setSpawnRange(source.getSpawnRange());
    }
}
