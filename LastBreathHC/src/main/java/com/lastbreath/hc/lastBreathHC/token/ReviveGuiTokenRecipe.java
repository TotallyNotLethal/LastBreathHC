package com.lastbreath.hc.lastBreathHC.token;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public class ReviveGuiTokenRecipe {

    public static void register() {
        NamespacedKey guiKey = new NamespacedKey(
                LastBreathHC.getInstance(), "revive_gui_token"
        );

        ShapedRecipe guiRecipe = new ShapedRecipe(guiKey, ReviveGuiToken.create());

        guiRecipe.shape(
                "CSC",
                "SOS",
                "SBS"
        );

        guiRecipe.setIngredient('C', Material.HEAVY_CORE);
        guiRecipe.setIngredient('O', Material.DRAGON_EGG);
        guiRecipe.setIngredient('S', Material.NETHER_STAR);
        guiRecipe.setIngredient('B', Material.NETHERITE_BLOCK);

        Bukkit.addRecipe(guiRecipe);
    }
}
