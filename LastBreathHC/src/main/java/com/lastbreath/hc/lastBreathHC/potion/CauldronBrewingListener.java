package com.lastbreath.hc.lastBreathHC.potion;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Optional;

public class CauldronBrewingListener implements Listener {

    private static final double CAULDRON_RADIUS_XZ = 1.2;
    private static final double CAULDRON_RADIUS_Y = 1.0;
    private static final int BREW_PARTICLE_COUNT = 20;

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
        Bukkit.getScheduler().runTask(plugin, () -> tryCauldronBrew(item));
    }

    private void tryCauldronBrew(Item item) {
        if (!item.isValid()) {
            return;
        }
        Block cauldron = getBrewingCauldron(item.getLocation());
        if (cauldron == null) {
            return;
        }

        List<Item> contents = item.getWorld().getNearbyEntities(
                cauldron.getLocation().add(0.5, 0.5, 0.5),
                CAULDRON_RADIUS_XZ, CAULDRON_RADIUS_Y, CAULDRON_RADIUS_XZ,
                entity -> entity instanceof Item
        ).stream().map(entity -> (Item) entity).toList();
        Optional<Item> potionEntity = contents.stream()
                .filter(entity -> isPotion(entity.getItemStack()))
                .findFirst();
        Optional<Item> ingredientEntity = contents.stream()
                .filter(entity -> isIngredient(entity.getItemStack()))
                .findFirst();
        if (potionEntity.isEmpty() || ingredientEntity.isEmpty()) {
            return;
        }

        brewOne(cauldron, potionEntity.get(), ingredientEntity.get());
    }

    private void brewOne(Block cauldron, Item potionEntity, Item ingredientEntity) {
        ItemStack potionStack = potionEntity.getItemStack();
        ItemStack ingredientStack = ingredientEntity.getItemStack();
        Material ingredient = ingredientStack.getType();

        ItemStack potionUnit = potionStack.clone();
        potionUnit.setAmount(1);
        ItemStack adjustedPotion = preparePotionForIngredient(potionUnit, ingredient);
        if (adjustedPotion == null) {
            return;
        }

        ItemStack result = potionHandler.applyIngredientForCauldron(adjustedPotion, ingredient);
        if (result == null) {
            return;
        }

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
                || material == Material.NETHER_STAR
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
        world.spawnParticle(Particle.SPELL_WITCH, center, BREW_PARTICLE_COUNT, 0.35, 0.25, 0.35, 0.01);
        world.spawnParticle(Particle.SPELL, center, BREW_PARTICLE_COUNT, 0.35, 0.2, 0.35, 0.01);
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
}
