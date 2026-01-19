package com.lastbreath.hc.lastBreathHC.token;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
    }
}
