package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;
import java.util.Locale;

public class CustomItemRecipes {

    public static void register() {
        registerCustomEnchantBook();
        registerEnhancedGrindstone();
        registerEnhancedEnchantingTable();
        registerTotemOfLife();
        registerWorldBossPortalCompass();
        registerRebirthStone();
        registerGracestone();
    }

    private static void registerCustomEnchantBook() {
        NamespacedKey legacyKey = new NamespacedKey(
                LastBreathHC.getInstance(), "custom_enchant_book"
        );
        Bukkit.removeRecipe(legacyKey);

        List<String> ids = CustomEnchant.filterAllowedIds(
                LastBreathHC.getInstance()
                        .getConfig()
                        .getStringList("asteroid.loot.enchantPages.ids")
        );

        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            registerCustomEnchantBookRecipe(id);
        }
    }

    private static void registerCustomEnchantBookRecipe(String enchantId) {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(),
                "custom_enchant_book_" + sanitizeRecipeKey(enchantId)
        );
        Bukkit.removeRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, CustomEnchantBook.create(enchantId));
        recipe.shape(
                "SPP",
                "SPP",
                "SPP"
        );
        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('P', new RecipeChoice.ExactChoice(CustomEnchantPage.create(enchantId)));

        Bukkit.addRecipe(recipe);
    }

    private static String sanitizeRecipeKey(String enchantId) {
        return enchantId
                .toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replaceAll("[^a-z0-9_/.-]", "_");
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


    private static void registerEnhancedEnchantingTable() {
        NamespacedKey key = new NamespacedKey(
                LastBreathHC.getInstance(), "enhanced_enchanting_table"
        );
        ShapedRecipe recipe = new ShapedRecipe(key, EnhancedEnchantingTable.create());
        recipe.shape(
                "SES",
                "ETE",
                "SBS"
        );

        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('E', Material.DRAGON_EGG);
        recipe.setIngredient('T', Material.ENCHANTING_TABLE);
        recipe.setIngredient('B', Material.CHISELED_BOOKSHELF);
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
                "GSC",
                "TDR",
                "AHN"
        );

        recipe.setIngredient('G', new RecipeChoice.ExactChoice(GravewardenCore.create()));
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(StormSigil.create()));
        recipe.setIngredient('C', new RecipeChoice.ExactChoice(ColossusFragment.create()));
        recipe.setIngredient('T', new RecipeChoice.ExactChoice(TotemOfLife.create()));
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('R', new RecipeChoice.ExactChoice(RebirthStone.create()));
        recipe.setIngredient('A', new RecipeChoice.ExactChoice(AshenRelic.create()));
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('N', Material.NETHER_STAR);

        Bukkit.addRecipe(recipe);
    }
}
