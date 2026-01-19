package com.lastbreath.hc.lastBreathHC.token;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public class TokenRecipe {

    public static void register() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "revive_token"
        );

        ShapedRecipe recipe = new ShapedRecipe(key, ReviveToken.create());

        recipe.shape(
                "PSP",
                "SOS",
                "PSP"
        );

        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('O', Material.DRAGON_EGG);

        Bukkit.addRecipe(recipe);

        NamespacedKey dragonEggKey = new NamespacedKey(
                LastBreathHC.getInstance(), "dragon_egg_craft"
        );
        ShapedRecipe dragonEggRecipe = new ShapedRecipe(
                dragonEggKey,
                new ItemStack(Material.DRAGON_EGG)
        );
        dragonEggRecipe.shape(
                "SSS",
                "SBS",
                "SCS"
        );
        dragonEggRecipe.setIngredient('S', Material.NETHER_STAR);
        dragonEggRecipe.setIngredient('B', Material.DRAGON_BREATH);
        dragonEggRecipe.setIngredient('C', Material.HEAVY_CORE);

        Bukkit.addRecipe(dragonEggRecipe);
    }
}
