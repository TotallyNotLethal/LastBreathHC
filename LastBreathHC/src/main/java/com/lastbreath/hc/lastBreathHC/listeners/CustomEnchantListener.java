package com.lastbreath.hc.lastBreathHC.listeners;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchant;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CustomEnchantListener implements Listener {

    private static final int VEIN_LIMIT = 64;
    private static final int TREE_LIMIT = 128;
    private static final int CONNECTION_RANGE = 2;
    private static final int LEAF_DECAY_TOTAL_TICKS = 200;
    private static final int LEAF_DECAY_INTERVAL_TICKS = 10;
    private static final double PROSPECTOR_CHANCE = 0.2;

    private static final Map<Material, Material> CROP_SEEDS = new EnumMap<>(Material.class);
    private static final Map<Material, Material> CROP_DROPS = new EnumMap<>(Material.class);
    private static final Map<Material, Material> ORE_FALLBACK_DROPS = new EnumMap<>(Material.class);

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

        ORE_FALLBACK_DROPS.put(Material.COAL_ORE, Material.COAL);
        ORE_FALLBACK_DROPS.put(Material.DEEPSLATE_COAL_ORE, Material.COAL);
        ORE_FALLBACK_DROPS.put(Material.COPPER_ORE, Material.RAW_COPPER);
        ORE_FALLBACK_DROPS.put(Material.DEEPSLATE_COPPER_ORE, Material.RAW_COPPER);
        ORE_FALLBACK_DROPS.put(Material.IRON_ORE, Material.RAW_IRON);
        ORE_FALLBACK_DROPS.put(Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON);
        ORE_FALLBACK_DROPS.put(Material.GOLD_ORE, Material.RAW_GOLD);
        ORE_FALLBACK_DROPS.put(Material.DEEPSLATE_GOLD_ORE, Material.RAW_GOLD);
        ORE_FALLBACK_DROPS.put(Material.REDSTONE_ORE, Material.REDSTONE);
        ORE_FALLBACK_DROPS.put(Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE);
        ORE_FALLBACK_DROPS.put(Material.LAPIS_ORE, Material.LAPIS_LAZULI);
        ORE_FALLBACK_DROPS.put(Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_LAZULI);
        ORE_FALLBACK_DROPS.put(Material.DIAMOND_ORE, Material.DIAMOND);
        ORE_FALLBACK_DROPS.put(Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND);
        ORE_FALLBACK_DROPS.put(Material.EMERALD_ORE, Material.EMERALD);
        ORE_FALLBACK_DROPS.put(Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD);
        ORE_FALLBACK_DROPS.put(Material.NETHER_GOLD_ORE, Material.GOLD_NUGGET);
        ORE_FALLBACK_DROPS.put(Material.NETHER_QUARTZ_ORE, Material.QUARTZ);
        ORE_FALLBACK_DROPS.put(Material.ANCIENT_DEBRIS, Material.ANCIENT_DEBRIS);
    }

    private final LastBreathHC plugin;
    private final Set<Location> processing = ConcurrentHashMap.newKeySet();
    private final Map<Material, ItemStack> smeltCache = new ConcurrentHashMap<>();
    private final Set<Material> smeltMisses = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();

    public CustomEnchantListener(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        var worldBossManager = plugin.getWorldBossManager();
        if (worldBossManager != null && worldBossManager.isArenaBlockProtected(block)) {
            return;
        }
        if (processing.contains(block.getLocation())) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        if (!CustomEnchantments.areEnchantsEnabled(meta)) {
            return;
        }
        List<String> enchantIds = CustomEnchantments.getEnchantIds(meta);
        if (enchantIds.isEmpty()) {
            return;
        }
        Set<String> normalized = normalizeIds(enchantIds);
        boolean veinMiner = normalized.contains(CustomEnchant.VEIN_MINER.getId());
        boolean treeFeller = normalized.contains(CustomEnchant.TREE_FELLER.getId());
        boolean excavator = normalized.contains(CustomEnchant.EXCAVATOR.getId());
        boolean quarry = normalized.contains(CustomEnchant.QUARRY.getId());
        boolean swiftTiller = normalized.contains(CustomEnchant.SWIFT_TILLER.getId());
        boolean autoReplant = normalized.contains(CustomEnchant.AUTO_REPLANT.getId());

        if (isHoe(tool.getType()) && autoReplant && isReplantableCrop(block) && !isMatureCrop(block)) {
            event.setCancelled(true);
            return;
        }

        boolean forceOreDrops = veinMiner && isOre(block.getType());
        if (handleCustomDrops(player, tool, block, normalized, block.getLocation(), forceOreDrops)) {
            event.setDropItems(false);
        }

        if (veinMiner && isPickaxe(tool.getType()) && isOre(block.getType())) {
            breakExtraBlocks(player, tool, getVeinBlocks(block), normalized, block.getLocation());
        }
        if (treeFeller && isAxe(tool.getType()) && isLog(block.getType())) {
            Collection<Block> treeBlocks = getTreeBlocks(block);
            breakExtraBlocks(player, tool, treeBlocks, normalized, block.getLocation());
            accelerateLeafDecay(block, treeBlocks);
        }
        if (excavator && isShovel(tool.getType()) && isShovelMineable(block.getType())) {
            breakExtraBlocks(player, tool, getExcavatorBlocks(block, player), normalized, block.getLocation());
        }
        if (quarry && isPickaxe(tool.getType()) && isPickaxeMineable(block.getType())) {
            breakExtraBlocks(player, tool, getQuarryBlocks(block, player), normalized, block.getLocation());
        }
        if (swiftTiller && isHoe(tool.getType()) && isReplantableCrop(block) && isMatureCrop(block)) {
            breakExtraBlocks(player, tool, getSwiftTillerHarvestBlocks(block, autoReplant), normalized, block.getLocation());
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onToggleEnchants(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if ((action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
                || !event.getPlayer().isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        if (CustomEnchantments.getEnchantIds(meta).isEmpty()) {
            return;
        }
        boolean enabled = CustomEnchantments.toggleEnchants(tool);
        player.sendActionBar(Component.text(
                enabled ? "Custom enchants enabled." : "Custom enchants disabled."
        ).color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
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
        if (!CustomEnchantments.areEnchantsEnabled(meta)) {
            return;
        }
        List<String> enchantIds = CustomEnchantments.getEnchantIds(meta);
        if (enchantIds.isEmpty()) {
            return;
        }
        Set<String> normalized = normalizeIds(enchantIds);
        if (!normalized.contains(CustomEnchant.SWIFT_TILLER.getId())) {
            return;
        }
        Block clicked = Objects.requireNonNull(event.getClickedBlock());
        tillArea(clicked, player, tool);
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

    private boolean isCoalOre(Material type) {
        return type == Material.COAL_ORE || type == Material.DEEPSLATE_COAL_ORE;
    }

    private boolean isLog(Material type) {
        return Tag.LOGS.isTagged(type);
    }

    private boolean isShovelMineable(Material type) {
        return Tag.MINEABLE_SHOVEL.isTagged(type);
    }

    private boolean isPickaxeMineable(Material type) {
        return Tag.MINEABLE_PICKAXE.isTagged(type);
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

    private boolean isReplantableCrop(Block block) {
        return CROP_SEEDS.containsKey(block.getType());
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
                player.getWorld().dropItem(location, leftover);
            }
            return;
        }
        for (ItemStack drop : drops) {
            player.getWorld().dropItem(location, drop);
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
            return cached.clone();
        }
        if (smeltMisses.contains(type)) {
            return null;
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

        if (result == null) {
            smeltMisses.add(type);
            return null;
        }
        smeltCache.put(type, result);
        return result.clone();
    }

    private List<ItemStack> addProspectorBonus(List<ItemStack> drops) {
        List<ItemStack> updated = new ArrayList<>(drops);
        for (ItemStack drop : drops) {
            if (drop.getType() != Material.AIR) {
                ItemStack bonus = drop.clone();
                bonus.setAmount(1 + random.nextInt(2));
                updated.add(bonus);
                break;
            }
        }
        return updated;
    }

    private List<ItemStack> ensureOreMinimumDrop(List<ItemStack> drops, Material blockType, boolean smelter) {
        int total = drops.stream()
            .filter(drop -> drop.getType() != Material.AIR)
            .mapToInt(ItemStack::getAmount)
            .sum();
        if (total > 0) {
            return drops;
        }

        Material fallback = ORE_FALLBACK_DROPS.getOrDefault(blockType, blockType);
        ItemStack fallbackDrop = new ItemStack(fallback, 1);
        if (smelter && !isCoalOre(blockType)) {
            ItemStack smelted = getSmeltedResult(fallbackDrop);
            if (smelted != null) {
                smelted.setAmount(1);
                return List.of(smelted);
            }
        }
        return List.of(fallbackDrop);
    }

    private void breakExtraBlocks(Player player, ItemStack tool, Collection<Block> blocks, Set<String> normalizedEnchantIds, Location dropLocation) {
        var worldBossManager = plugin.getWorldBossManager();
        for (Block target : blocks) {
            if (target.getType() == Material.AIR) {
                continue;
            }
            if (worldBossManager != null && worldBossManager.isArenaBlockProtected(target)) {
                continue;
            }
            Location location = target.getLocation();
            if (!processing.add(location)) {
                continue;
            }
            try {
                int expToDrop = resolveExpToDrop(player, tool, target);
                if (expToDrop < 0) {
                    continue;
                }
                boolean forceOreDrops = isOre(target.getType()) && isPickaxe(tool.getType());
                if (handleCustomDrops(player, tool, target, normalizedEnchantIds, dropLocation, forceOreDrops)) {
                    breakBlockWithPhysics(target);
                    dropExperience(target.getLocation(), expToDrop);
                    if (applyDurabilityDamage(player, tool)) {
                        return;
                    }
                } else {
                    target.breakNaturally(tool);
                    dropExperience(target.getLocation(), expToDrop);
                }
            } finally {
                processing.remove(location);
            }
        }
    }

    private int resolveExpToDrop(Player player, ItemStack tool, Block block) {
        // Compute what vanilla would do for XP for this block + tool
        int vanillaExp = computeVanillaExpToDrop(block.getType(), tool);

        // Fire a synthetic break event so protections/other plugins can cancel or modify XP
        BlockBreakEvent synthetic = new BlockBreakEvent(block, player);

        // We handle item drops ourselves in many cases, but XP is separate
        synthetic.setDropItems(false);

        // Seed XP so event consumers see a realistic value and you can drop it per block
        synthetic.setExpToDrop(vanillaExp);

        Bukkit.getPluginManager().callEvent(synthetic);

        if (synthetic.isCancelled()) {
            return -1;
        }

        return Math.max(0, synthetic.getExpToDrop());
    }

    private int computeVanillaExpToDrop(Material type, ItemStack tool) {
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            // Silk Touch prevents ore XP in vanilla
            return 0;
        }

        // Fortune does NOT change XP in vanilla (only drops), so we ignore it intentionally.

        // Vanilla XP ranges (inclusive) for common ores
        return switch (type) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> random.nextInt(3);          // 0–2
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> 3 + random.nextInt(8); // 3–7
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> 3 + random.nextInt(8); // 3–7
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> 2 + random.nextInt(6);     // 2–5
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> 1 + random.nextInt(6); // 1–5
            case NETHER_QUARTZ_ORE -> 2 + random.nextInt(6);                 // 2–5
            case NETHER_GOLD_ORE -> random.nextInt(2);                       // 0–1

            // These ores give small XP when mined (raw ore era)
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> random.nextInt(3);      // 0–2
            case IRON_ORE, DEEPSLATE_IRON_ORE -> random.nextInt(3);          // 0–2
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> random.nextInt(3);          // 0–2

            // No XP in vanilla
            case ANCIENT_DEBRIS -> 0;

            default -> 0;
        };
    }


    private void dropExperience(Location location, int amount) {
        if (amount <= 0) {
            return;
        }
        ExperienceOrb orb = location.getWorld().spawn(location.clone().add(0.5, 0.5, 0.5), ExperienceOrb.class);
        orb.setExperience(amount);
    }

    private boolean applyDurabilityDamage(Player player, ItemStack tool) {
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }
        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreakingLevel > 0 && random.nextInt(unbreakingLevel + 1) != 0) {
            return false;
        }
        int newDamage = damageable.getDamage() + 1;
        damageable.setDamage(newDamage);
        tool.setItemMeta(meta);
        if (newDamage >= tool.getType().getMaxDurability()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            return true;
        }
        return false;
    }

    private boolean handleCustomDrops(Player player, ItemStack tool, Block block, Set<String> normalizedEnchantIds, Location dropLocation, boolean forceOreDrops) {
        boolean autoPickup = normalizedEnchantIds.contains(CustomEnchant.AUTO_PICKUP.getId());
        boolean smelter = normalizedEnchantIds.contains(CustomEnchant.SMELTER_TOUCH.getId());
        boolean autoReplant = normalizedEnchantIds.contains(CustomEnchant.AUTO_REPLANT.getId());
        boolean fertileHarvest = normalizedEnchantIds.contains(CustomEnchant.FERTILE_HARVEST.getId());
        boolean prospector = normalizedEnchantIds.contains(CustomEnchant.PROSPECTOR.getId());
        boolean prospectorOre = prospector && isOre(block.getType());
        boolean shouldHandleDrops = autoPickup || smelter || autoReplant || fertileHarvest || prospectorOre || forceOreDrops;
        if (!shouldHandleDrops) {
            return false;
        }
        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool, player));
        if (smelter && isOre(block.getType()) && !isCoalOre(block.getType())) {
            drops = smeltDrops(drops);
        }
        if (isOre(block.getType())) {
            drops = ensureOreMinimumDrop(drops, block.getType(), smelter);
        }
        if (prospectorOre && random.nextDouble() < PROSPECTOR_CHANCE) {
            drops = addProspectorBonus(drops);
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
        giveDrops(player, dropLocation, drops, autoPickup);
        return true;
    }

    private void breakBlockWithPhysics(Block block) {
        block.setType(Material.AIR, true);
    }

    private Collection<Block> getVeinBlocks(Block origin) {
        Set<Block> matches = new HashSet<>();
        Material type = origin.getType();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        matches.add(origin);
        while (!queue.isEmpty() && matches.size() < VEIN_LIMIT) {
            Block current = queue.poll();
            for (int dx = -CONNECTION_RANGE; dx <= CONNECTION_RANGE; dx++) {
                for (int dy = -CONNECTION_RANGE; dy <= CONNECTION_RANGE; dy++) {
                    for (int dz = -CONNECTION_RANGE; dz <= CONNECTION_RANGE; dz++) {
                        if (!isWithinConnectionRange(dx, dy, dz)) {
                            continue;
                        }
                        if (tryAddVeinNeighbor(current.getRelative(dx, dy, dz), type, matches, queue)
                            && matches.size() >= VEIN_LIMIT) {
                            break;
                        }
                    }
                    if (matches.size() >= VEIN_LIMIT) {
                        break;
                    }
                }
                if (matches.size() >= VEIN_LIMIT) {
                    break;
                }
            }
        }
        matches.remove(origin);
        return matches;
    }

    private boolean isWithinConnectionRange(int dx, int dy, int dz) {
        if (dx == 0 && dy == 0 && dz == 0) {
            return false;
        }
        return Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) <= CONNECTION_RANGE;
    }

    private boolean tryAddVeinNeighbor(Block neighbor, Material type, Set<Block> matches, Deque<Block> queue) {
        if (neighbor.getType() != type || matches.contains(neighbor)) {
            return false;
        }
        matches.add(neighbor);
        queue.add(neighbor);
        return true;
    }

    private Collection<Block> getTreeBlocks(Block origin) {
        Set<Block> matches = new HashSet<>();
        Material type = origin.getType();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        matches.add(origin);
        while (!queue.isEmpty() && matches.size() < TREE_LIMIT) {
            Block current = queue.poll();
            for (int dx = -CONNECTION_RANGE; dx <= CONNECTION_RANGE; dx++) {
                for (int dy = -CONNECTION_RANGE; dy <= CONNECTION_RANGE; dy++) {
                    for (int dz = -CONNECTION_RANGE; dz <= CONNECTION_RANGE; dz++) {
                        if (!isWithinConnectionRange(dx, dy, dz)) {
                            continue;
                        }
                        Block neighbor = current.getRelative(dx, dy, dz);
                        if (neighbor.getType() != type || matches.contains(neighbor)) {
                            continue;
                        }
                        matches.add(neighbor);
                        queue.add(neighbor);
                        if (matches.size() >= TREE_LIMIT) {
                            break;
                        }
                    }
                    if (matches.size() >= TREE_LIMIT) {
                        break;
                    }
                }
                if (matches.size() >= TREE_LIMIT) {
                    break;
                }
            }
        }
        matches.remove(origin);
        return matches;
    }

    private void accelerateLeafDecay(Block origin, Collection<Block> treeBlocks) {
        List<Block> logs = new ArrayList<>(treeBlocks.size() + 1);
        logs.add(origin);
        logs.addAll(treeBlocks);

        Set<Block> leavesToDecay = new HashSet<>();
        for (Block log : logs) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        Block candidate = log.getRelative(dx, dy, dz);
                        if (isNaturallyDecayingLeaf(candidate)) {
                            leavesToDecay.add(candidate);
                        }
                    }
                }
            }
        }

        if (leavesToDecay.isEmpty()) {
            return;
        }

        List<Block> orderedLeaves = new ArrayList<>(leavesToDecay);
        int maxRuns = Math.max(1, LEAF_DECAY_TOTAL_TICKS / LEAF_DECAY_INTERVAL_TICKS);
        int leavesPerRun = Math.max(1, (int) Math.ceil((double) orderedLeaves.size() / maxRuns));

        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                int removedThisRun = 0;
                while (index < orderedLeaves.size() && removedThisRun < leavesPerRun) {
                    Block leaf = orderedLeaves.get(index++);
                    if (isNaturallyDecayingLeaf(leaf)) {
                        leaf.setType(Material.AIR, true);
                    }
                    removedThisRun++;
                }
                if (index >= orderedLeaves.size()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, LEAF_DECAY_INTERVAL_TICKS, LEAF_DECAY_INTERVAL_TICKS);
    }

    private boolean isNaturallyDecayingLeaf(Block block) {
        if (!Tag.LEAVES.isTagged(block.getType())) {
            return false;
        }
        BlockData data = block.getBlockData();
        if (!(data instanceof Leaves leaves)) {
            return true;
        }
        return !leaves.isPersistent();
    }

    private Collection<Block> getExcavatorBlocks(Block origin, Player player) {
        Set<Block> blocks = new HashSet<>();
        BlockFace face = resolveMiningFace(player, origin);
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
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

        if (face == BlockFace.EAST || face == BlockFace.WEST) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 0 && z == 0) {
                        continue;
                    }
                    Block candidate = origin.getRelative(0, y, z);
                    if (isShovelMineable(candidate.getType())) {
                        blocks.add(candidate);
                    }
                }
            }
            return blocks;
        }

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                Block candidate = origin.getRelative(x, y, 0);
                if (isShovelMineable(candidate.getType())) {
                    blocks.add(candidate);
                }
            }
        }
        return blocks;
    }

    private Collection<Block> getQuarryBlocks(Block origin, Player player) {
        Set<Block> blocks = new HashSet<>();
        BlockFace face = resolveMiningFace(player, origin);
        int primaryMin;
        int primaryMax;
        int secondaryMin;
        int secondaryMax;

        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            primaryMin = -1;
            primaryMax = 1;
            secondaryMin = -1;
            secondaryMax = 1;
            for (int x = primaryMin; x <= primaryMax; x++) {
                for (int z = secondaryMin; z <= secondaryMax; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    addQuarryCandidate(blocks, origin.getRelative(x, 0, z));
                }
            }
            return blocks;
        }

        if (face == BlockFace.EAST || face == BlockFace.WEST) {
            primaryMin = -1;
            primaryMax = 1;
            secondaryMin = -1;
            secondaryMax = 1;
            for (int y = primaryMin; y <= primaryMax; y++) {
                for (int z = secondaryMin; z <= secondaryMax; z++) {
                    if (y == 0 && z == 0) {
                        continue;
                    }
                    addQuarryCandidate(blocks, origin.getRelative(0, y, z));
                }
            }
            return blocks;
        }

        primaryMin = -1;
        primaryMax = 1;
        secondaryMin = -1;
        secondaryMax = 1;
        for (int x = primaryMin; x <= primaryMax; x++) {
            for (int y = secondaryMin; y <= secondaryMax; y++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                addQuarryCandidate(blocks, origin.getRelative(x, y, 0));
            }
        }
        return blocks;
    }

    private void addQuarryCandidate(Set<Block> blocks, Block candidate) {
        if (isPickaxeMineable(candidate.getType())) {
            blocks.add(candidate);
        }
    }

    private Collection<Block> getSwiftTillerHarvestBlocks(Block origin, boolean requireMature) {
        Set<Block> blocks = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                Block candidate = origin.getRelative(x, 0, z);
                if (!isReplantableCrop(candidate)) {
                    continue;
                }
                if (requireMature && !isMatureCrop(candidate)) {
                    continue;
                }
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

    private BlockFace resolveMiningFace(Player player, Block origin) {
        Block target = player.getTargetBlockExact(6);
        BlockFace face = player.getTargetBlockFace(6);
        if (target != null && target.equals(origin) && face != null) {
            return face;
        }
        return resolveDirectionalFace(player);
    }

    private void tillArea(Block origin, Player player, ItemStack tool) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block target = origin.getRelative(x, 0, z);
                Block above = target.getRelative(BlockFace.UP);
                if (!isTillable(target.getType()) || above.getType() != Material.AIR) {
                    continue;
                }
                target.setType(Material.FARMLAND, false);
                if (applyDurabilityDamage(player, tool)) {
                    return;
                }
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
