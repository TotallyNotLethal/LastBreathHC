package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

public class CustomItemRecipes {

    public static void register() {
        registerCustomEnchantBook();
        registerEnhancedGrindstone();
        registerTotemOfLife();
        registerWorldBossPortalCompass();
        registerRebirthStone();
        registerGracestone();
    }

    private static void registerCustomEnchantBook() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "custom_enchant_book"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, CustomEnchantBook.create("unknown"));
        recipe.shape(
                "SPP",
                "SPP",
                "SPP"
        );

        recipe.setIngredient('S', Material.NETHER_STAR);

        java.util.List<String> ids = CustomEnchant.filterAllowedIds(
                LastBreathHC.getInstance()
                        .getConfig()
                        .getStringList("asteroid.loot.enchantPages.ids")
        );
        java.util.List<org.bukkit.inventory.ItemStack> choices = new java.util.ArrayList<>();
        if (ids.isEmpty()) {
            choices.add(CustomEnchantPage.create("unknown"));
        } else {
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    choices.add(CustomEnchantPage.create(id));
                }
            }
        }
        recipe.setIngredient('P', new RecipeChoice.ExactChoice(choices));

        Bukkit.addRecipe(recipe);
    }

    private static void registerEnhancedGrindstone() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "enhanced_grindstone"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, EnhancedGrindstone.create());
        recipe.shape(
                "SES",
                "EGE",
                "SBS"
        );

        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('E', Material.DRAGON_EGG);
        recipe.setIngredient('G', Material.GRINDSTONE);
        recipe.setIngredient('B', Material.NETHERITE_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private static void registerTotemOfLife() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "totem_of_life"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, TotemOfLife.create());
        recipe.shape(
                "GTG",
                "TTT",
                "HTH"
        );
        recipe.setIngredient('G', Material.NETHER_STAR);
        recipe.setIngredient('H', Material.DRAGON_BREATH);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        Bukkit.addRecipe(recipe);
    }

    private static void registerWorldBossPortalCompass() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "world_boss_portal_compass"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, WorldBossPortalCompass.create());
        recipe.shape(
                "EEE",
                "ECE",
                "EEE"
        );
        recipe.setIngredient('E', Material.ENDER_EYE);
        recipe.setIngredient('C', Material.COMPASS);
        Bukkit.addRecipe(recipe);
    }

    private static void registerRebirthStone() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "rebirth_stone"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, RebirthStone.create());
        recipe.shape(
                "NSN",
                "SWS",
                "NSN"
        );
        recipe.setIngredient('N', Material.END_CRYSTAL);
        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('W', Material.DRAGON_EGG);
        Bukkit.addRecipe(recipe);
    }

    private static void registerGracestone() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "gracestone"
        );

        ShapedRecipe recipe = new ShapedRecipe(key, Gracestone.create());

        recipe.shape(
                "ABA",
                "ACA",
                "DED"
        );

        recipe.setIngredient('A', Material.HEAVY_CORE);
        recipe.setIngredient('B', Material.ELYTRA);

        // ✅ REQUIRE Totem of Life
        recipe.setIngredient('C',
                new RecipeChoice.ExactChoice(TotemOfLife.create()));

        recipe.setIngredient('D', Material.NETHER_STAR);

        // ✅ REQUIRE Rebirth Stone
        recipe.setIngredient('E',
                new RecipeChoice.ExactChoice(RebirthStone.create()));

        Bukkit.addRecipe(recipe);
    }
}
