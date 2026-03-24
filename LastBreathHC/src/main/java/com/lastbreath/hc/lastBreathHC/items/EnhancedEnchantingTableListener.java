package com.lastbreath.hc.lastBreathHC.items;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class EnhancedEnchantingTableListener implements Listener {

    private static final int[][] BOOKSHELF_OFFSETS = {
            {-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1},
            {2, 0, -1}, {2, 0, 0}, {2, 0, 1},
            {-1, 0, -2}, {0, 0, -2}, {1, 0, -2},
            {-1, 0, 2}, {0, 0, 2}, {1, 0, 2},
            {-2, 1, -1}, {-2, 1, 0}, {-2, 1, 1},
            {2, 1, -1}, {2, 1, 0}, {2, 1, 1},
            {-1, 1, -2}, {0, 1, -2}, {1, 1, -2},
            {-1, 1, 2}, {0, 1, 2}, {1, 1, 2}
    };

    private final Random random = new Random();

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!EnhancedEnchantingTable.isEnhancedEnchantingTable(item)) {
            return;
        }

        Block placed = event.getBlockPlaced();
        Chunk chunk = placed.getChunk();
        Set<String> blocks = getEnhancedBlocks(chunk);
        blocks.add(encodeBlockKey(placed));
        saveEnhancedBlocks(chunk, blocks);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isEnhancedEnchantingTableBlock(block)) {
            return;
        }

        event.setDropItems(false);
        removeEnhancedBlock(block);
        block.getWorld().dropItemNaturally(
                block.getLocation().add(0.5, 0.5, 0.5),
                EnhancedEnchantingTable.create()
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (!isEnhancedEnchantingTableBlock(event.getEnchantBlock())) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        Map<Enchantment, Integer> enchantsToApply = buildResolvedEnchantMap(event.getEnchantBlock(), item);
        if (enchantsToApply.isEmpty()) {
            return;
        }

        EnchantmentOffer[] offers = event.getOffers();
        if (offers == null || offers.length < 3) {
            return;
        }

        List<Map.Entry<Enchantment, Integer>> entries = new ArrayList<>(enchantsToApply.entrySet());
        Map.Entry<Enchantment, Integer> hint = entries.get(random.nextInt(entries.size()));
        offers[2] = new EnchantmentOffer(
                hint.getKey(),
                hint.getValue(),
                EnhancedEnchantingTable.GUARANTEED_ENCHANT_COST
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEnchant(EnchantItemEvent event) {
        if (!isEnhancedEnchantingTableBlock(event.getEnchantBlock())) {
            return;
        }

        if (event.getExpLevelCost() != EnhancedEnchantingTable.GUARANTEED_ENCHANT_COST) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            event.setCancelled(true);
            return;
        }

        Map<Enchantment, Integer> enchantsToApply = buildResolvedEnchantMap(event.getEnchantBlock(), item);
        if (enchantsToApply.isEmpty()) {
            event.setCancelled(true);
            Player player = event.getEnchanter();
            player.sendMessage("§cNo valid chiseled bookshelf enchantments were found.");
            return;
        }

        Map<Enchantment, Integer> enchantsToAdd = event.getEnchantsToAdd();
        enchantsToAdd.clear();
        enchantsToAdd.putAll(enchantsToApply);
    }

    private Map<Enchantment, Integer> buildResolvedEnchantMap(Block enchantBlock, ItemStack item) {
        Map<Enchantment, Integer> candidateLevels = collectCandidateEnchants(enchantBlock, item);
        if (candidateLevels.isEmpty()) {
            return Map.of();
        }

        Set<Enchantment> unresolved = new HashSet<>(candidateLevels.keySet());
        Set<Enchantment> selected = new HashSet<>();

        while (!unresolved.isEmpty()) {
            Enchantment start = unresolved.iterator().next();
            Set<Enchantment> component = collectConflictComponent(start, unresolved);
            unresolved.removeAll(component);

            if (component.size() == 1) {
                selected.add(start);
                continue;
            }

            List<Enchantment> choices = new ArrayList<>(component);
            selected.add(choices.get(random.nextInt(choices.size())));
        }

        Map<Enchantment, Integer> resolved = new HashMap<>();
        for (Enchantment enchantment : selected) {
            resolved.put(enchantment, candidateLevels.get(enchantment));
        }
        return resolved;
    }

    private Set<Enchantment> collectConflictComponent(Enchantment start, Set<Enchantment> pool) {
        Set<Enchantment> component = new HashSet<>();
        ArrayDeque<Enchantment> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            Enchantment current = queue.removeFirst();
            if (!component.add(current)) {
                continue;
            }

            for (Enchantment other : pool) {
                if (!component.contains(other) && areConflicting(current, other)) {
                    queue.addLast(other);
                }
            }
        }

        return component;
    }

    private Map<Enchantment, Integer> collectCandidateEnchants(Block enchantBlock, ItemStack item) {
        Map<Enchantment, Integer> candidates = new HashMap<>();

        for (int[] offset : BOOKSHELF_OFFSETS) {
            int pathX = Integer.signum(offset[0]);
            int pathY = offset[1];
            int pathZ = Integer.signum(offset[2]);
            if (!isPathClear(enchantBlock, pathX, pathY, pathZ)) {
                continue;
            }

            Block shelfBlock = enchantBlock.getRelative(offset[0], offset[1], offset[2]);
            if (shelfBlock.getType() != Material.CHISELED_BOOKSHELF) {
                continue;
            }

            if (!(shelfBlock.getState() instanceof ChiseledBookshelf chiseledBookshelf)) {
                continue;
            }

            ItemStack[] shelfContents = chiseledBookshelf.getInventory().getStorageContents();
            for (ItemStack shelfItem : shelfContents) {
                if (shelfItem == null || shelfItem.getType() != Material.ENCHANTED_BOOK) {
                    continue;
                }
                if (!(shelfItem.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
                    continue;
                }

                for (Map.Entry<Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()) {
                    Enchantment enchantment = entry.getKey();
                    int level = Math.max(1, Math.min(entry.getValue(), enchantment.getMaxLevel()));
                    if (!isAllowedForItem(enchantment, item)) {
                        continue;
                    }
                    candidates.merge(enchantment, level, Math::max);
                }
            }
        }

        return candidates;
    }

    private boolean isAllowedForItem(Enchantment enchantment, ItemStack item) {
        if (!enchantment.canEnchantItem(item)) {
            return false;
        }

        for (Enchantment existing : item.getEnchantments().keySet()) {
            if (areConflicting(existing, enchantment)) {
                return false;
            }
        }

        return true;
    }

    private boolean areConflicting(Enchantment first, Enchantment second) {
        return first.conflictsWith(second) || second.conflictsWith(first);
    }

    private boolean isPathClear(Block enchantBlock, int xOffset, int yOffset, int zOffset) {
        if (xOffset == 0 && zOffset == 0) {
            return true;
        }

        Block path = enchantBlock.getRelative(xOffset, yOffset, zOffset);
        return path.isEmpty();
    }

    private boolean isEnhancedEnchantingTableBlock(Block block) {
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) {
            return false;
        }

        return getEnhancedBlocks(block.getChunk()).contains(encodeBlockKey(block));
    }

    private void removeEnhancedBlock(Block block) {
        Chunk chunk = block.getChunk();
        Set<String> blocks = getEnhancedBlocks(chunk);
        if (blocks.remove(encodeBlockKey(block))) {
            saveEnhancedBlocks(chunk, blocks);
        }
    }

    private Set<String> getEnhancedBlocks(Chunk chunk) {
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        String stored = container.get(EnhancedEnchantingTable.BLOCKS_KEY, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(stored.split(";"))
                .filter(entry -> !entry.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void saveEnhancedBlocks(Chunk chunk, Set<String> blocks) {
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (blocks.isEmpty()) {
            container.remove(EnhancedEnchantingTable.BLOCKS_KEY);
            return;
        }
        container.set(
                EnhancedEnchantingTable.BLOCKS_KEY,
                PersistentDataType.STRING,
                String.join(";", blocks)
        );
    }

    private String encodeBlockKey(Block block) {
        int localX = block.getX() & 0xF;
        int localZ = block.getZ() & 0xF;
        return localX + "," + block.getY() + "," + localZ;
    }
}
