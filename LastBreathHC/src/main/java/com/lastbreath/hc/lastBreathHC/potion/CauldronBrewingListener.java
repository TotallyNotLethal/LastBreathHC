package com.lastbreath.hc.lastBreathHC.potion;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CauldronBrewingListener implements Listener {

    private static final double CAULDRON_RADIUS_XZ = 1.2;
    private static final double CAULDRON_RADIUS_Y = 1.0;
    private static final int BREW_PARTICLE_COUNT = 20;
    private static final int BREW_CHECK_DURATION_TICKS = 40;

    private final JavaPlugin plugin;
    private final PotionHandler potionHandler;
    private final PotionDefinitionRegistry definitionRegistry;

    public CauldronBrewingListener(JavaPlugin plugin, PotionHandler potionHandler, PotionDefinitionRegistry definitionRegistry) {
        this.plugin = plugin;
        this.potionHandler = potionHandler;
        this.definitionRegistry = definitionRegistry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (isPotion(item.getItemStack()) || isIngredient(item.getItemStack())) {
            sendDebug(item, "Brew candidate spawned. Scheduling cauldron checks.");
        }
        scheduleBrewCheck(item);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCauldronPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.CAULDRON && block.getType() != Material.WATER_CAULDRON) {
            return;
        }
        sendDebug(event.getPlayer(), "Cauldron placement detected at " + describeLocation(block.getLocation()) + ".");
        Block below = block.getRelative(BlockFace.DOWN);
        if (below.getType() != Material.SOUL_CAMPFIRE) {
            sendDebug(event.getPlayer(), "Cauldron placed, but the block below is not a soul campfire.");
            return;
        }
        if (!(below.getBlockData() instanceof Campfire campfire)) {
            sendDebug(event.getPlayer(), "Cauldron placed on a soul campfire, but campfire data is missing.");
            return;
        }
        if (!campfire.isLit()) {
            sendDebug(event.getPlayer(), "Cauldron placed on a soul campfire, but it is not lit.");
            return;
        }
        if (block.getType() == Material.WATER_CAULDRON) {
            sendDebug(event.getPlayer(), "Valid brewing setup detected: water cauldron on a lit soul campfire.");
        } else {
            sendDebug(event.getPlayer(), "Cauldron placed on a lit soul campfire. Fill it with water to brew.");
        }
        sendDebug(event.getPlayer(), "Valid ingredients: " + listValidIngredients());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        if (isPotion(item.getItemStack()) || isIngredient(item.getItemStack())) {
            sendDebug(item, "Brew candidate dropped. Scheduling cauldron checks.");
        }
        scheduleBrewCheck(item);
    }

    private void scheduleBrewCheck(Item item) {
        new BukkitRunnable() {
            private int ticks;
            private boolean loggedFailures;

            @Override
            public void run() {
                if (!item.isValid()) {
                    cancel();
                    return;
                }
                boolean logFailures = ticks >= BREW_CHECK_DURATION_TICKS && !loggedFailures;
                if (tryCauldronBrew(item, logFailures)) {
                    cancel();
                    return;
                }
                if (logFailures) {
                    loggedFailures = true;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean tryCauldronBrew(Item item, boolean logFailures) {
        if (!item.isValid()) {
            return false;
        }
        Block cauldron = getBrewingCauldron(item.getLocation());
        if (cauldron == null) {
            if (logFailures) {
                debugMissingCauldron(item);
            }
            return false;
        }

        sendDebug(item, "Detected valid brewing cauldron (water cauldron on lit soul campfire).");

        List<Item> contents = item.getWorld().getNearbyEntities(
                cauldron.getLocation().add(0.5, 0.5, 0.5),
                CAULDRON_RADIUS_XZ, CAULDRON_RADIUS_Y, CAULDRON_RADIUS_XZ,
                entity -> entity instanceof Item
        ).stream().map(entity -> (Item) entity).toList();
        Optional<Item> potionEntity = contents.stream()
                .filter(entity -> isPotion(entity.getItemStack()))
                .filter(entity -> potionHandler.getCustomPotionId(entity.getItemStack()).isPresent())
                .findFirst();
        if (potionEntity.isEmpty()) {
            potionEntity = contents.stream()
                    .filter(entity -> isPotion(entity.getItemStack()))
                    .findFirst();
        }
        Optional<Item> ingredientEntity = contents.stream()
                .filter(entity -> isIngredient(entity.getItemStack()))
                .findFirst();
        if (potionEntity.isEmpty() || ingredientEntity.isEmpty()) {
            if (logFailures) {
                if (isPotion(item.getItemStack())) {
                    sendDebug(item, "Water bottle/potion detected in cauldron. Waiting for ingredient.");
                } else if (isIngredient(item.getItemStack())) {
                    sendDebug(item, "Ingredient detected in cauldron. Waiting for water bottle/potion.");
                }
                sendDebug(item, "Valid ingredients: " + listValidIngredients());
            }
            return false;
        }

        brewOne(cauldron, potionEntity.get(), ingredientEntity.get());
        return true;
    }

    private void brewOne(Block cauldron, Item potionEntity, Item ingredientEntity) {
        ItemStack potionStack = potionEntity.getItemStack();
        ItemStack ingredientStack = ingredientEntity.getItemStack();
        Material ingredient = ingredientStack.getType();

        sendDebug(cauldron.getLocation(), "Brewing attempt: potion=" + describePotion(potionStack)
                + ", ingredient=" + ingredient);

        ItemStack potionUnit = potionStack.clone();
        potionUnit.setAmount(1);
        ItemStack adjustedPotion = preparePotionForIngredient(potionUnit, ingredient);
        if (adjustedPotion == null) {
            sendDebug(cauldron.getLocation(), "Failed to prepare potion for ingredient. Is this a valid potion item?");
            return;
        }

        ItemStack previewPotion = adjustedPotion.clone();
        ItemStack result = potionHandler.applyIngredientForCauldron(previewPotion, ingredient);
        if (result == null) {
            sendDebug(cauldron.getLocation(),
                    "No valid recipe for potion=" + describePotion(adjustedPotion) + " with ingredient=" + ingredient
                            + ". Expected result: " + expectedResultDescription(ingredient));
            return;
        }

        sendDebug(cauldron.getLocation(), "Recipe matched. Result will be: " + describePotion(result));

        consumeSingle(potionEntity);
        consumeSingle(ingredientEntity);

        ItemStack output = result.clone();
        output.setAmount(1);
        cauldron.getWorld().dropItemNaturally(cauldron.getLocation().add(0.5, 1.0, 0.5), output);
        playBrewEffects(cauldron);
    }

    private Block getBrewingCauldron(Location location) {
        Block block = location.getBlock();
        if (isBrewingCauldron(block)) {
            return block;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (isBrewingCauldron(below)) {
            return below;
        }
        return null;
    }

    private boolean isBrewingCauldron(Block block) {
        if (block.getType() != Material.WATER_CAULDRON) {
            return false;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (below.getType() != Material.SOUL_CAMPFIRE) {
            return false;
        }
        if (!(below.getBlockData() instanceof Campfire campfire)) {
            return false;
        }
        return campfire.isLit();
    }

    private boolean isPotion(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION;
    }

    private boolean isIngredient(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material material = item.getType();
        return material == Material.REDSTONE
                || material == Material.GLOWSTONE_DUST
                || material == Material.GHAST_TEAR
                || definitionRegistry.getByIngredient(material) != null;
    }

    private ItemStack preparePotionForIngredient(ItemStack potion, Material ingredient) {
        if (!isPotion(potion)) {
            return null;
        }
        if (!isWaterBottle(potion)) {
            return potion;
        }
        if (definitionRegistry.getByIngredient(ingredient) == null) {
            return potion;
        }
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.setBasePotionType(PotionType.AWKWARD);
        potion.setItemMeta(meta);
        return potion;
    }

    private boolean isWaterBottle(ItemStack item) {
        if (!isPotion(item)) {
            return false;
        }
        if (!(item.getItemMeta() instanceof PotionMeta meta)) {
            return false;
        }
        return meta.getBasePotionType() == PotionType.WATER;
    }

    private void playBrewEffects(Block cauldron) {
        World world = cauldron.getWorld();
        Location center = cauldron.getLocation().add(0.5, 0.85, 0.5);
        world.spawnParticle(Particle.WITCH, center, BREW_PARTICLE_COUNT, 0.35, 0.25, 0.35, 0.01);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, BREW_PARTICLE_COUNT, 0.35, 0.2, 0.35, 0.01);
        world.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 0.9f, 1.1f);
    }

    private void consumeSingle(Item item) {
        ItemStack stack = item.getItemStack();
        int amount = stack.getAmount();
        if (amount <= 1) {
            item.remove();
            return;
        }
        stack.setAmount(amount - 1);
        item.setItemStack(stack);
    }

    private void debugMissingCauldron(Item item) {
        if (!isPotion(item.getItemStack()) && !isIngredient(item.getItemStack())) {
            return;
        }
        Location location = item.getLocation();
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        if (block.getType() == Material.CAULDRON || below.getType() == Material.CAULDRON) {
            sendDebug(item, "Cauldron detected but it is not a water cauldron. Fill it with water to brew.");
            return;
        }
        if (block.getType() == Material.WATER_CAULDRON || below.getType() == Material.WATER_CAULDRON) {
            Block cauldron = block.getType() == Material.WATER_CAULDRON ? block : below;
            Block campfire = cauldron.getRelative(BlockFace.DOWN);
            if (campfire.getType() != Material.SOUL_CAMPFIRE) {
                sendDebug(item, "Water cauldron detected but it is not on a soul campfire.");
                return;
            }
            if (!(campfire.getBlockData() instanceof Campfire campfireData)) {
                sendDebug(item, "Water cauldron on soul campfire, but campfire data is missing.");
                return;
            }
            if (!campfireData.isLit()) {
                sendDebug(item, "Water cauldron on soul campfire, but the campfire is not lit.");
            }
        }
    }

    private String listValidIngredients() {
        String custom = definitionRegistry.getAll().stream()
                .sorted(Comparator.comparing(def -> def.craftingIngredient().name()))
                .map(def -> def.craftingIngredient().name() + " -> " + def.displayName())
                .collect(Collectors.joining(", "));

        String defaults = "REDSTONE -> extend duration, GLOWSTONE_DUST -> concentrate, GHAST_TEAR -> purify";

        if (custom.isBlank()) {
            return defaults;
        }

        return defaults + ", " + custom;
    }

    private String describePotion(ItemStack potion) {
        if (potion == null) {
            return "none";
        }

        if (!(potion.getItemMeta() instanceof PotionMeta meta)) {
            return potion.getType().name();
        }

        String name = meta.hasDisplayName()
                ? PlainTextComponentSerializer.plainText().serialize(meta.displayName())
                : potion.getType().name();

        PotionType base = meta.getBasePotionType();
        String baseName = base != null ? base.name() : "UNKNOWN";

        return name + " (base=" + baseName + ", type=" + potion.getType().name() + ")";
    }

    private String describeLocation(Location location) {
        return "x=" + location.getBlockX()
                + ", y=" + location.getBlockY()
                + ", z=" + location.getBlockZ()
                + ", world=" + location.getWorld().getName();
    }

    private String expectedResultDescription(Material ingredient) {
        if (ingredient == Material.REDSTONE) {
            return "extends existing potion effects";
        }
        if (ingredient == Material.GLOWSTONE_DUST) {
            return "concentrates existing potion effects";
        }
        if (ingredient == Material.GHAST_TEAR) {
            return "purifies negative effects";
        }
        HardcorePotionDefinition definition = definitionRegistry.getByIngredient(ingredient);
        if (definition == null) {
            return "no custom potion mapping for ingredient";
        }
        return "custom potion: " + definition.displayName();
    }

    private void sendDebug(Item item, String message) {
        UUID throwerId = item.getThrower();
        if (throwerId != null) {
            Player thrower = Bukkit.getPlayer(throwerId);
            if (thrower != null) {
                sendDebug(thrower, message);
                return;
            }
        }
        sendDebug(item.getLocation(), message);
    }

    private void sendDebug(Player player, String message) {
        plugin.getLogger().info("[Cauldron] " + message);
    }

    private void sendDebug(Location location, String message) {
        plugin.getLogger().info("[Cauldron] " + message);
    }
}
