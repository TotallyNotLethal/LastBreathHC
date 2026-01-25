package com.lastbreath.hc.lastBreathHC.items;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnhancedGrindstoneListener implements Listener {

    private final Set<UUID> activeUsers = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (!isEnhancedGrindstoneBlock(clicked)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        activeUsers.add(player.getUniqueId());
        player.openAnvil(null, true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!EnhancedGrindstone.isEnhancedGrindstone(item)) {
            return;
        }

        BlockState state = event.getBlockPlaced().getState();
        if (!(state instanceof TileState tileState)) {
            return;
        }

        PersistentDataContainer container = tileState.getPersistentDataContainer();
        container.set(EnhancedGrindstone.KEY, PersistentDataType.BYTE, (byte) 1);
        tileState.update();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isEnhancedGrindstoneBlock(block)) {
            return;
        }

        event.setDropItems(false);
        block.getWorld().dropItemNaturally(
                block.getLocation().add(0.5, 0.5, 0.5),
                EnhancedGrindstone.create()
        );
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (!activeUsers.contains(player.getUniqueId())) return;

        AnvilInventory inventory = event.getInventory();
        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        if (left == null || right == null || right.getType() != Material.BOOK) return;

        Map<Enchantment, Integer> enchants = extractEnchantments(left);
        if (enchants.isEmpty()) return;

        ItemStack result = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = result.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta storageMeta)) return;

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            storageMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
        }
        result.setItemMeta(storageMeta);
        event.setResult(result);

        // ✅ NEW API (replaces inventory.setRepairCost)
        AnvilView view = (AnvilView) event.getView();
        view.setRepairCost(1);

        // Optional: if you also want to avoid "Too Expensive" behavior, set max cost higher
        // view.setMaximumRepairCost(40); // or something large
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        activeUsers.remove(event.getPlayer().getUniqueId());
    }

    private Map<Enchantment, Integer> extractEnchantments(ItemStack item) {
        if (item == null) {
            return Map.of();
        }

        if (item.getType() == Material.ENCHANTED_BOOK
                && item.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            return new HashMap<>(storageMeta.getStoredEnchants());
        }

        return new HashMap<>(item.getEnchantments());
    }

    private boolean isEnhancedGrindstoneBlock(Block block) {
        if (block == null || block.getType() != Material.GRINDSTONE) {
            return false;
        }

        BlockState state = block.getState();
        if (!(state instanceof TileState tileState)) {
            return false;
        }

        return tileState.getPersistentDataContainer()
                .has(EnhancedGrindstone.KEY, PersistentDataType.BYTE);
    }
}
