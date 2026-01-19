package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

public class CustomItemRecipes {

    public static void register() {
        registerEnhancedGrindstone();
        registerTotemOfLife();
        registerReaperStone();
        registerGracestone();
    }

    private static void registerEnhancedGrindstone() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "enhanced_grindstone"
        );
        ShapelessRecipe recipe = new ShapelessRecipe(key, EnhancedGrindstone.create());
        recipe.addIngredient(Material.NETHER_STAR);
        recipe.addIngredient(Material.DRAGON_EGG);
        recipe.addIngredient(Material.GRINDSTONE);
        recipe.addIngredient(Material.NETHERITE_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private static void registerTotemOfLife() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "totem_of_life"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, TotemOfLife.create());
        recipe.shape(
                "GHG",
                "HTH",
                "GHG"
        );
        recipe.setIngredient('G', Material.GOLDEN_APPLE);
        recipe.setIngredient('H', Material.HEART_OF_THE_SEA);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        Bukkit.addRecipe(recipe);
    }

    private static void registerReaperStone() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "reaper_stone"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, ReaperStone.create());
        recipe.shape(
                "NSN",
                "SWS",
                "NSN"
        );
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('S', Material.SOUL_SAND);
        recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        Bukkit.addRecipe(recipe);
    }

    private static void registerGracestone() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "gracestone"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, Gracestone.create());
        recipe.shape(
                "EEE",
                "ETE",
                "EEE"
        );
        recipe.setIngredient('E', Material.END_STONE);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        Bukkit.addRecipe(recipe);
    }
}
