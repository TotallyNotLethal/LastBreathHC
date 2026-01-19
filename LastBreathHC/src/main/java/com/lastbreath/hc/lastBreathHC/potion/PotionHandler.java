package com.lastbreath.hc.lastBreathHC.potion;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PotionHandler implements Listener {

    private static final int MAX_DURATION_TICKS = 20 * 60 * 20;
    private static final int REDSTONE_BONUS_TICKS = 2 * 60 * 20;
    private static final int MIN_DURATION_TICKS = 10 * 20;
    private static final double CONCENTRATION_REDUCTION = 0.7D;
    private static final int MAX_AMPLIFIER = 2;

    private static final Set<PotionEffectType> NEGATIVE_EFFECTS = EnumSet.of(
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOW,
            PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.DARKNESS,
            PotionEffectType.NAUSEA,
            PotionEffectType.HUNGER,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.UNLUCK
    );

    private final NamespacedKey redstoneKey;
    private final NamespacedKey concentrationKey;
    private final NamespacedKey purifiedKey;
    private final NamespacedKey customIdKey;
    private final Map<Material, CustomPotionRecipe> customRecipes;

    public PotionHandler(LastBreathHC plugin) {
        this.redstoneKey = new NamespacedKey(plugin, "potion_redstone_apps");
        this.concentrationKey = new NamespacedKey(plugin, "potion_concentration");
        this.purifiedKey = new NamespacedKey(plugin, "potion_purified");
        this.customIdKey = new NamespacedKey(plugin, "potion_custom_id");
        this.customRecipes = buildCustomRecipes();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionCraft(PrepareItemCraftEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }

        Optional<CustomCraftingMatch> match = findPotionCraftingMatch(inventory.getMatrix());
        if (match.isEmpty()) {
            return;
        }

        ItemStack result = applyIngredient(match.get().potion(), match.get().ingredient());
        if (result != null) {
            inventory.setResult(result);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewComplete(BrewEvent event) {
        BrewerInventory inventory = event.getContents();
        ItemStack ingredient = inventory.getIngredient();
        if (ingredient == null || ingredient.getType() == Material.AIR) {
            return;
        }

        for (int slot = 0; slot < 3; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isPotion(item)) {
                continue;
            }

            ItemStack updated = applyIngredient(item, ingredient.getType());
            if (updated != null) {
                inventory.setItem(slot, updated);
            }
        }
    }

    private Optional<CustomCraftingMatch> findPotionCraftingMatch(ItemStack[] matrix) {
        ItemStack potion = null;
        Material ingredient = null;
        int items = 0;
        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            items++;
            if (items > 2) {
                return Optional.empty();
            }
            if (isPotion(item)) {
                potion = item.clone();
            } else {
                ingredient = item.getType();
            }
        }
        if (items == 2 && potion != null && ingredient != null) {
            return Optional.of(new CustomCraftingMatch(potion, ingredient));
        }
        return Optional.empty();
    }

    private ItemStack applyIngredient(ItemStack potion, Material ingredient) {
        if (!isPotion(potion)) {
            return null;
        }
        if (ingredient == Material.REDSTONE) {
            return applyRedstone(potion);
        }
        if (ingredient == Material.GLOWSTONE_DUST) {
            return applyConcentration(potion);
        }
        if (ingredient == Material.NETHER_STAR) {
            return applyPurification(potion);
        }
        CustomPotionRecipe recipe = customRecipes.get(ingredient);
        if (recipe != null) {
            return applyCustomPotion(potion, recipe);
        }
        return null;
    }

    private ItemStack applyCustomPotion(ItemStack potion, CustomPotionRecipe recipe) {
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null || !isAwkwardPotion(meta)) {
            return null;
        }

        PotionMeta updatedMeta = (PotionMeta) meta.clone();
        updatedMeta.setBasePotionType(PotionType.AWKWARD);
        updatedMeta.clearCustomEffects();
        for (PotionEffect effect : recipe.effects()) {
            updatedMeta.addCustomEffect(effect, true);
        }
        updatedMeta.setDisplayName(recipe.displayName());

        PersistentDataContainer container = updatedMeta.getPersistentDataContainer();
        container.set(customIdKey, PersistentDataType.STRING, recipe.id());
        updateLore(updatedMeta);

        potion.setItemMeta(updatedMeta);
        return potion;
    }

    private ItemStack applyRedstone(ItemStack potion) {
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) {
            return null;
        }
        List<PotionEffect> effects = getPotionEffects(meta);
        if (effects.isEmpty()) {
            return null;
        }

        PotionMeta updatedMeta = (PotionMeta) meta.clone();
        updatedMeta.setBasePotionType(PotionType.AWKWARD);
        updatedMeta.clearCustomEffects();
        boolean changed = false;
        for (PotionEffect effect : effects) {
            int newDuration = Math.min(effect.getDuration() + REDSTONE_BONUS_TICKS, MAX_DURATION_TICKS);
            if (newDuration != effect.getDuration()) {
                changed = true;
            }
            updatedMeta.addCustomEffect(new PotionEffect(effect.getType(), newDuration, effect.getAmplifier(),
                    effect.isAmbient(), effect.hasParticles(), effect.hasIcon()), true);
        }
        if (!changed) {
            return null;
        }

        PersistentDataContainer container = updatedMeta.getPersistentDataContainer();
        int currentApps = container.getOrDefault(redstoneKey, PersistentDataType.INTEGER, 0);
        int newApps = currentApps + 1;
        container.set(redstoneKey, PersistentDataType.INTEGER, newApps);
        updateLore(updatedMeta);

        potion.setItemMeta(updatedMeta);
        return potion;
    }

    private ItemStack applyConcentration(ItemStack potion) {
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) {
            return null;
        }
        List<PotionEffect> effects = getPotionEffects(meta);
        if (effects.isEmpty()) {
            return null;
        }

        PotionMeta updatedMeta = (PotionMeta) meta.clone();
        updatedMeta.setBasePotionType(PotionType.AWKWARD);
        updatedMeta.clearCustomEffects();
        boolean changed = false;
        for (PotionEffect effect : effects) {
            int reducedDuration = (int) Math.max(effect.getDuration() * CONCENTRATION_REDUCTION, MIN_DURATION_TICKS);
            int newAmplifier = Math.min(effect.getAmplifier() + 1, MAX_AMPLIFIER);
            if (reducedDuration != effect.getDuration() || newAmplifier != effect.getAmplifier()) {
                changed = true;
            }
            updatedMeta.addCustomEffect(new PotionEffect(effect.getType(), reducedDuration, newAmplifier,
                    effect.isAmbient(), effect.hasParticles(), effect.hasIcon()), true);
        }
        if (!changed) {
            return null;
        }

        PersistentDataContainer container = updatedMeta.getPersistentDataContainer();
        int currentLevel = container.getOrDefault(concentrationKey, PersistentDataType.INTEGER, 0);
        int newLevel = currentLevel + 1;
        container.set(concentrationKey, PersistentDataType.INTEGER, newLevel);
        updateLore(updatedMeta);

        potion.setItemMeta(updatedMeta);
        return potion;
    }

    private ItemStack applyPurification(ItemStack potion) {
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) {
            return null;
        }
        List<PotionEffect> effects = getPotionEffects(meta);
        if (effects.isEmpty()) {
            return null;
        }

        List<PotionEffect> filtered = new ArrayList<>();
        for (PotionEffect effect : effects) {
            if (!NEGATIVE_EFFECTS.contains(effect.getType())) {
                filtered.add(effect);
            }
        }
        if (filtered.size() == effects.size()) {
            return null;
        }

        PotionMeta updatedMeta = (PotionMeta) meta.clone();
        updatedMeta.setBasePotionType(PotionType.AWKWARD);
        updatedMeta.clearCustomEffects();
        for (PotionEffect effect : filtered) {
            updatedMeta.addCustomEffect(effect, true);
        }

        PersistentDataContainer container = updatedMeta.getPersistentDataContainer();
        container.set(purifiedKey, PersistentDataType.BYTE, (byte) 1);
        updateLore(updatedMeta);

        potion.setItemMeta(updatedMeta);
        return potion;
    }

    private void updateLore(PotionMeta meta) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int redstoneApps = container.getOrDefault(redstoneKey, PersistentDataType.INTEGER, 0);
        int concentration = container.getOrDefault(concentrationKey, PersistentDataType.INTEGER, 0);
        boolean purified = container.has(purifiedKey, PersistentDataType.BYTE);
        String customId = container.get(customIdKey, PersistentDataType.STRING);

        List<String> lore = new ArrayList<>();
        if (customId != null) {
            lore.add("§8Custom Brew");
        }
        if (redstoneApps > 0 || concentration > 0 || purified) {
            lore.add("§8Modifiers");
        }
        if (redstoneApps > 0) {
            lore.add("§7Extended: +" + (redstoneApps * 2) + "m");
        }
        if (concentration > 0) {
            lore.add("§7Concentration: +" + concentration + " amp");
        }
        if (purified) {
            lore.add("§7Purified: negatives removed");
        }

        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
    }

    private List<PotionEffect> getPotionEffects(PotionMeta meta) {
        List<PotionEffect> effects = new ArrayList<>();
        if (meta.hasCustomEffects()) {
            effects.addAll(meta.getCustomEffects());
        }
        if (effects.isEmpty() && meta.getBasePotionType() != null) {
            effects.addAll(meta.getBasePotionType().getPotionEffects());
        }
        return effects;
    }

    private boolean isPotion(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION;
    }

    private boolean isAwkwardPotion(PotionMeta meta) {
        return meta.getBasePotionType() == PotionType.AWKWARD;
    }

    private Map<Material, CustomPotionRecipe> buildCustomRecipes() {
        return Map.of(
                Material.AMETHYST_SHARD,
                new CustomPotionRecipe(
                        "clear_mind",
                        "§rPotion of Clear Mind",
                        List.of(
                                new PotionEffect(PotionEffectType.NIGHT_VISION, 3 * 60 * 20, 0),
                                new PotionEffect(PotionEffectType.WEAKNESS, 20 * 20, 0)
                        )
                ),
                Material.COPPER_INGOT,
                new CustomPotionRecipe(
                        "copper_rush",
                        "§rPotion of Copper Rush",
                        List.of(
                                new PotionEffect(PotionEffectType.HASTE, 60 * 20, 0),
                                new PotionEffect(PotionEffectType.SLOW, 10 * 20, 0)
                        )
                ),
                Material.ECHO_SHARD,
                new CustomPotionRecipe(
                        "echoed_veil",
                        "§rPotion of Echoed Veil",
                        List.of(
                                new PotionEffect(PotionEffectType.INVISIBILITY, 45 * 20, 0),
                                new PotionEffect(PotionEffectType.MINING_FATIGUE, 15 * 20, 0)
                        )
                ),
                Material.HONEYCOMB,
                new CustomPotionRecipe(
                        "honeyed_guard",
                        "§rPotion of Honeyed Guard",
                        List.of(
                                new PotionEffect(PotionEffectType.RESISTANCE, 35 * 20, 0),
                                new PotionEffect(PotionEffectType.SLOW, 20 * 20, 0)
                        )
                ),
                Material.GLOW_BERRIES,
                new CustomPotionRecipe(
                        "glow_warmth",
                        "§rPotion of Glow Warmth",
                        List.of(
                                new PotionEffect(PotionEffectType.REGENERATION, 15 * 20, 0),
                                new PotionEffect(PotionEffectType.HUNGER, 15 * 20, 0)
                        )
                ),
                Material.PRISMARINE_CRYSTALS,
                new CustomPotionRecipe(
                        "deep_breath",
                        "§rPotion of Deep Breath",
                        List.of(
                                new PotionEffect(PotionEffectType.WATER_BREATHING, 2 * 60 * 20, 0),
                                new PotionEffect(PotionEffectType.WEAKNESS, 20 * 20, 0)
                        )
                ),
                Material.NAUTILUS_SHELL,
                new CustomPotionRecipe(
                        "tidal_step",
                        "§rPotion of Tidal Step",
                        List.of(
                                new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60 * 20, 0),
                                new PotionEffect(PotionEffectType.SLOW, 10 * 20, 0)
                        )
                ),
                Material.SWEET_BERRIES,
                new CustomPotionRecipe(
                        "forager_edge",
                        "§rPotion of Forager's Edge",
                        List.of(
                                new PotionEffect(PotionEffectType.SPEED, 45 * 20, 0),
                                new PotionEffect(PotionEffectType.HUNGER, 20 * 20, 0)
                        )
                ),
                Material.CRIMSON_FUNGUS,
                new CustomPotionRecipe(
                        "crimson_guard",
                        "§rPotion of Crimson Guard",
                        List.of(
                                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60 * 20, 0),
                                new PotionEffect(PotionEffectType.WEAKNESS, 15 * 20, 0)
                        )
                ),
                Material.WARPED_FUNGUS,
                new CustomPotionRecipe(
                        "warped_focus",
                        "§rPotion of Warped Focus",
                        List.of(
                                new PotionEffect(PotionEffectType.JUMP_BOOST, 60 * 20, 0),
                                new PotionEffect(PotionEffectType.NAUSEA, 10 * 20, 0)
                        )
                )
        );
    }

    private record CustomPotionRecipe(String id, String displayName, List<PotionEffect> effects) {
    }

    private record CustomCraftingMatch(ItemStack potion, Material ingredient) {
    }
}
