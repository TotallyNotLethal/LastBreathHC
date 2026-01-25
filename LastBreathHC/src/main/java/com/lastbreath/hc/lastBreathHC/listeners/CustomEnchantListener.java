package com.lastbreath.hc.lastBreathHC.listeners;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchant;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantments;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CustomEnchantListener implements Listener {

    private static final int VEIN_LIMIT = 64;
    private static final int TREE_LIMIT = 128;
    private static final int DIRECTIONAL_DISTANCE = 2;
    private static final double PROSPECTOR_CHANCE = 0.2;

    private static final Map<Material, Material> CROP_SEEDS = new EnumMap<>(Material.class);
    private static final Map<Material, Material> CROP_DROPS = new EnumMap<>(Material.class);

    static {
        CROP_SEEDS.put(Material.WHEAT, Material.WHEAT_SEEDS);
        CROP_SEEDS.put(Material.CARROTS, Material.CARROT);
        CROP_SEEDS.put(Material.POTATOES, Material.POTATO);
        CROP_SEEDS.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        CROP_SEEDS.put(Material.NETHER_WART, Material.NETHER_WART);

        CROP_DROPS.put(Material.WHEAT, Material.WHEAT);
        CROP_DROPS.put(Material.CARROTS, Material.CARROT);
        CROP_DROPS.put(Material.POTATOES, Material.POTATO);
        CROP_DROPS.put(Material.BEETROOTS, Material.BEETROOT);
        CROP_DROPS.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    private final LastBreathHC plugin;
    private final Set<Location> processing = ConcurrentHashMap.newKeySet();
    private final Map<Material, ItemStack> smeltCache = new ConcurrentHashMap<>();

    public CustomEnchantListener(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (processing.contains(block.getLocation())) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        List<String> enchantIds = CustomEnchantments.getEnchantIds(meta);
        if (enchantIds.isEmpty()) {
            return;
        }
        Set<String> normalized = normalizeIds(enchantIds);
        boolean autoPickup = normalized.contains(CustomEnchant.AUTO_PICKUP.getId());
        boolean smelter = normalized.contains(CustomEnchant.SMELTER_TOUCH.getId());
        boolean autoReplant = normalized.contains(CustomEnchant.AUTO_REPLANT.getId());
        boolean fertileHarvest = normalized.contains(CustomEnchant.FERTILE_HARVEST.getId());
        boolean prospector = normalized.contains(CustomEnchant.PROSPECTOR.getId());
        boolean veinMiner = normalized.contains(CustomEnchant.VEIN_MINER.getId());
        boolean treeFeller = normalized.contains(CustomEnchant.TREE_FELLER.getId());
        boolean excavator = normalized.contains(CustomEnchant.EXCAVATOR.getId());
        boolean directional = normalized.contains(CustomEnchant.DIRECTIONAL_MINING.getId());

        boolean shouldHandleDrops = autoPickup || smelter || autoReplant || fertileHarvest || prospector;
        if (shouldHandleDrops) {
            event.setDropItems(false);
            List<ItemStack> drops = new ArrayList<>(block.getDrops(tool, player));
            if (smelter) {
                drops = smeltDrops(drops);
            }
            if (prospector && isOre(block.getType()) && Math.random() < PROSPECTOR_CHANCE) {
                drops = addProspectorBonus(drops, block.getType());
            }
            if (fertileHarvest && isMatureCrop(block)) {
                Material extraCrop = CROP_DROPS.get(block.getType());
                if (extraCrop != null) {
                    drops.add(new ItemStack(extraCrop, 1));
                }
            }
            if (autoReplant && isMatureCrop(block)) {
                drops = consumeSeedForReplant(drops, block.getType());
                scheduleReplant(block, block.getBlockData());
            }
            giveDrops(player, block.getLocation(), drops, autoPickup);
        }

        if (veinMiner && isPickaxe(tool.getType()) && isOre(block.getType())) {
            breakExtraBlocks(player, tool, getVeinBlocks(block));
        }
        if (treeFeller && isAxe(tool.getType()) && isLog(block.getType())) {
            breakExtraBlocks(player, tool, getTreeBlocks(block));
        }
        if (excavator && isShovel(tool.getType()) && isShovelMineable(block.getType())) {
            breakExtraBlocks(player, tool, getExcavatorBlocks(block));
        }
        if (directional && isPickaxe(tool.getType())) {
            breakExtraBlocks(player, tool, getDirectionalBlocks(block, player));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }
        if (!isHoe(tool.getType())) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        List<String> enchantIds = CustomEnchantments.getEnchantIds(meta);
        if (enchantIds.isEmpty()) {
            return;
        }
        Set<String> normalized = normalizeIds(enchantIds);
        if (!normalized.contains(CustomEnchant.SWIFT_TILLER.getId())) {
            return;
        }
        Block clicked = Objects.requireNonNull(event.getClickedBlock());
        tillArea(clicked);
    }

    private Set<String> normalizeIds(List<String> ids) {
        Set<String> normalized = new HashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                normalized.add(id.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private boolean isOre(Material type) {
        return type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS;
    }

    private boolean isLog(Material type) {
        return Tag.LOGS.isTagged(type);
    }

    private boolean isShovelMineable(Material type) {
        return Tag.MINEABLE_SHOVEL.isTagged(type);
    }

    private boolean isPickaxe(Material type) {
        return type.name().endsWith("_PICKAXE");
    }

    private boolean isAxe(Material type) {
        return type.name().endsWith("_AXE");
    }

    private boolean isShovel(Material type) {
        return type.name().endsWith("_SHOVEL");
    }

    private boolean isHoe(Material type) {
        return type.name().endsWith("_HOE");
    }

    private boolean isMatureCrop(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    private void scheduleReplant(Block block, BlockData originalData) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            BlockData replanted = originalData.clone();
            if (replanted instanceof Ageable ageable) {
                ageable.setAge(0);
            }
            block.setBlockData(replanted, false);
        });
    }

    private List<ItemStack> consumeSeedForReplant(List<ItemStack> drops, Material cropType) {
        Material seed = CROP_SEEDS.get(cropType);
        if (seed == null) {
            return drops;
        }
        List<ItemStack> updated = new ArrayList<>();
        boolean consumed = false;
        for (ItemStack drop : drops) {
            if (!consumed && drop.getType() == seed) {
                int amount = drop.getAmount();
                if (amount > 1) {
                    ItemStack adjusted = drop.clone();
                    adjusted.setAmount(amount - 1);
                    updated.add(adjusted);
                }
                consumed = true;
                continue;
            }
            updated.add(drop);
        }
        return updated;
    }

    private void giveDrops(Player player, Location location, List<ItemStack> drops, boolean autoPickup) {
        if (drops.isEmpty()) {
            return;
        }
        if (autoPickup) {
            Map<Integer, ItemStack> remaining = player.getInventory().addItem(drops.toArray(new ItemStack[0]));
            if (remaining.isEmpty()) {
                return;
            }
            for (ItemStack leftover : remaining.values()) {
                player.getWorld().dropItemNaturally(location, leftover);
            }
            return;
        }
        for (ItemStack drop : drops) {
            player.getWorld().dropItemNaturally(location, drop);
        }
    }

    private List<ItemStack> smeltDrops(List<ItemStack> drops) {
        List<ItemStack> updated = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack smelted = getSmeltedResult(drop);
            if (smelted == null) {
                updated.add(drop);
            } else {
                smelted.setAmount(drop.getAmount());
                updated.add(smelted);
            }
        }
        return updated;
    }

    private ItemStack getSmeltedResult(ItemStack input) {
        Material type = input.getType();

        if (smeltCache.containsKey(type)) {
            ItemStack cached = smeltCache.get(type);
            return cached == null ? null : cached.clone();
        }

        ItemStack result = null;

        var iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();

            if (recipe instanceof org.bukkit.inventory.FurnaceRecipe furnace) {
                if (furnace.getInputChoice().test(input)) {
                    result = furnace.getResult().clone();
                    break;
                }
            }
        }

        smeltCache.put(type, result);
        return result == null ? null : result.clone();
    }

    private List<ItemStack> addProspectorBonus(List<ItemStack> drops, Material type) {
        List<ItemStack> updated = new ArrayList<>(drops);
        for (ItemStack drop : drops) {
            if (drop.getType() != Material.AIR) {
                ItemStack bonus = drop.clone();
                bonus.setAmount(1);
                updated.add(bonus);
                break;
            }
        }
        return updated;
    }

    private void breakExtraBlocks(Player player, ItemStack tool, Collection<Block> blocks) {
        for (Block target : blocks) {
            if (target.getType() == Material.AIR) {
                continue;
            }
            Location location = target.getLocation();
            if (!processing.add(location)) {
                continue;
            }
            try {
                target.breakNaturally(tool);
            } finally {
                processing.remove(location);
            }
        }
    }

    private Collection<Block> getVeinBlocks(Block origin) {
        Set<Block> matches = new HashSet<>();
        Material type = origin.getType();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        matches.add(origin);
        while (!queue.isEmpty() && matches.size() < VEIN_LIMIT) {
            Block current = queue.poll();
            for (BlockFace face : BlockFace.values()) {
                if (!face.isCartesian()) {
                    continue;
                }
                Block neighbor = current.getRelative(face);
                if (neighbor.getType() != type || matches.contains(neighbor)) {
                    continue;
                }
                matches.add(neighbor);
                queue.add(neighbor);
                if (matches.size() >= VEIN_LIMIT) {
                    break;
                }
            }
        }
        matches.remove(origin);
        return matches;
    }

    private Collection<Block> getTreeBlocks(Block origin) {
        Set<Block> matches = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        matches.add(origin);
        while (!queue.isEmpty() && matches.size() < TREE_LIMIT) {
            Block current = queue.poll();
            for (BlockFace face : BlockFace.values()) {
                if (!face.isCartesian()) {
                    continue;
                }
                Block neighbor = current.getRelative(face);
                if (!isLog(neighbor.getType()) || matches.contains(neighbor)) {
                    continue;
                }
                matches.add(neighbor);
                queue.add(neighbor);
                if (matches.size() >= TREE_LIMIT) {
                    break;
                }
            }
        }
        matches.remove(origin);
        return matches;
    }

    private Collection<Block> getExcavatorBlocks(Block origin) {
        Set<Block> blocks = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                Block candidate = origin.getRelative(x, 0, z);
                if (isShovelMineable(candidate.getType())) {
                    blocks.add(candidate);
                }
            }
        }
        return blocks;
    }

    private Collection<Block> getDirectionalBlocks(Block origin, Player player) {
        Set<Block> blocks = new HashSet<>();
        BlockFace face = resolveDirectionalFace(player);
        for (int i = 1; i <= DIRECTIONAL_DISTANCE; i++) {
            Block candidate = origin.getRelative(face, i);
            if (candidate.getType() != Material.AIR) {
                blocks.add(candidate);
            }
        }
        return blocks;
    }

    private BlockFace resolveDirectionalFace(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch > 60) {
            return BlockFace.DOWN;
        }
        if (pitch < -60) {
            return BlockFace.UP;
        }
        return player.getFacing();
    }

    private void tillArea(Block origin) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block target = origin.getRelative(x, 0, z);
                Block above = target.getRelative(BlockFace.UP);
                if (!isTillable(target.getType()) || above.getType() != Material.AIR) {
                    continue;
                }
                target.setType(Material.FARMLAND, false);
            }
        }
    }

    private boolean isTillable(Material type) {
        return type == Material.DIRT
                || type == Material.GRASS_BLOCK
                || type == Material.COARSE_DIRT
                || type == Material.ROOTED_DIRT
                || type == Material.PODZOL;
    }
}
