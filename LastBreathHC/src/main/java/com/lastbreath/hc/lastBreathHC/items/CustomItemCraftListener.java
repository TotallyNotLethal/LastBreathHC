package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.token.ReviveGuiToken;
import com.lastbreath.hc.lastBreathHC.token.ReviveToken;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

public class CustomItemCraftListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPrepareCustomItemCraft(PrepareItemCraftEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }

        ItemStack[] matrix = inventory.getMatrix();
        if (matrix.length < 9) {
            return;
        }

        ItemStack result = resolveCustomResult(matrix);
        if (result != null) {
            inventory.setResult(result);
        }
    }

    private ItemStack resolveCustomResult(ItemStack[] matrix) {
        if (matchesPattern(matrix, new Predicate[]{
                is(Material.PAPER), is(Material.NETHER_STAR), is(Material.PAPER),
                is(Material.NETHER_STAR), is(Material.DRAGON_EGG), is(Material.NETHER_STAR),
                is(Material.PAPER), is(Material.NETHER_STAR), is(Material.PAPER)
        })) {
            return ReviveToken.create();
        }

        if (matchesPattern(matrix, new Predicate[]{
                is(Material.HEAVY_CORE), is(Material.NETHER_STAR), is(Material.HEAVY_CORE),
                is(Material.NETHER_STAR), is(Material.DRAGON_EGG), is(Material.NETHER_STAR),
                is(Material.NETHER_STAR), is(Material.NETHERITE_BLOCK), is(Material.NETHER_STAR)
        })) {
            return ReviveGuiToken.create();
        }

        if (matchesPattern(matrix, new Predicate[]{
                is(Material.NETHER_STAR), is(Material.TOTEM_OF_UNDYING), is(Material.NETHER_STAR),
                is(Material.TOTEM_OF_UNDYING), is(Material.TOTEM_OF_UNDYING), is(Material.TOTEM_OF_UNDYING),
                is(Material.DRAGON_BREATH), is(Material.TOTEM_OF_UNDYING), is(Material.DRAGON_BREATH)
        })) {
            return TotemOfLife.create();
        }

        if (matchesPattern(matrix, new Predicate[]{
                is(Material.END_CRYSTAL), is(Material.NETHER_STAR), is(Material.END_CRYSTAL),
                is(Material.NETHER_STAR), is(Material.DRAGON_EGG), is(Material.NETHER_STAR),
                is(Material.END_CRYSTAL), is(Material.NETHER_STAR), is(Material.END_CRYSTAL)
        })) {
            return RebirthStone.create();
        }

        if (matchesPattern(matrix, new Predicate[]{
                is(Material.NETHER_STAR), is(Material.DRAGON_EGG), is(Material.NETHER_STAR),
                is(Material.DRAGON_EGG), is(Material.GRINDSTONE), is(Material.DRAGON_EGG),
                is(Material.NETHER_STAR), is(Material.NETHERITE_BLOCK), is(Material.NETHER_STAR)
        })) {
            return EnhancedGrindstone.create();
        }

        if (matchesPattern(matrix, new Predicate[]{
                is(Material.HEAVY_CORE), is(Material.ELYTRA), is(Material.HEAVY_CORE),
                is(Material.HEAVY_CORE), isTotemOfLife(), is(Material.HEAVY_CORE),
                is(Material.NETHER_STAR), isRebirthStone(), is(Material.NETHER_STAR)
        })) {
            return Gracestone.create();
        }

        return null;
    }

    private boolean matchesPattern(ItemStack[] matrix, Predicate<ItemStack>[] slots) {
        if (slots.length != 9) {
            throw new IllegalArgumentException("Recipe pattern must define 9 slots.");
        }
        for (int slot = 0; slot < slots.length; slot++) {
            Predicate<ItemStack> matcher = slots[slot];
            ItemStack item = matrix[slot];
            if (!matcher.test(item)) {
                return false;
            }
        }
        return true;
    }

    private Predicate<ItemStack> is(Material material) {
        return item -> item != null && item.getType() == material;
    }

    private Predicate<ItemStack> isTotemOfLife() {
        return TotemOfLife::isTotemOfLife;
    }

    private Predicate<ItemStack> isRebirthStone() {
        return RebirthStone::isRebirthStone;
    }
}
