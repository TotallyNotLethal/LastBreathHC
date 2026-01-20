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
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PotionHandler implements Listener {

    private static final int MAX_DURATION_TICKS = 20 * 60 * 20;
    private static final int REDSTONE_BONUS_TICKS = 2 * 60 * 20;
    private static final int MIN_DURATION_TICKS = 10 * 20;
    private static final double CONCENTRATION_REDUCTION = 0.7D;
    private static final int MAX_AMPLIFIER = 2;

    private static final Set<PotionEffectType> NEGATIVE_EFFECTS = Set.of(
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
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
    private final NamespacedKey customEffectsKey;
    private final NamespacedKey scaryToDrinkKey;
    private final PotionDefinitionRegistry definitionRegistry;

    public PotionHandler(LastBreathHC plugin, PotionDefinitionRegistry definitionRegistry) {
        this.redstoneKey = new NamespacedKey(plugin, "potion_redstone_apps");
        this.concentrationKey = new NamespacedKey(plugin, "potion_concentration");
        this.purifiedKey = new NamespacedKey(plugin, "potion_purified");
        this.customIdKey = new NamespacedKey(plugin, "potion_custom_id");
        this.customEffectsKey = new NamespacedKey(plugin, "potion_custom_effects");
        this.scaryToDrinkKey = new NamespacedKey(plugin, "potion_scary_to_drink");
        this.definitionRegistry = definitionRegistry;
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
        HardcorePotionDefinition definition = definitionRegistry.getByIngredient(ingredient);
        if (definition != null) {
            return applyCustomPotion(potion, definition);
        }
        return null;
    }

    public ItemStack applyIngredientForCauldron(ItemStack potion, Material ingredient) {
        return applyIngredient(potion, ingredient);
    }

    public Optional<String> getCustomPotionId(ItemStack potion) {
        if (potion == null || !(potion.getItemMeta() instanceof PotionMeta meta)) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String customId = container.get(customIdKey, PersistentDataType.STRING);
        return Optional.ofNullable(customId);
    }

    private ItemStack applyCustomPotion(ItemStack potion, HardcorePotionDefinition definition) {
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null || !canApplyDefinition(meta, definition)) {
            return null;
        }

        PotionMeta updatedMeta = (PotionMeta) meta.clone();
        updatedMeta.setBasePotionType(PotionType.AWKWARD);
        updatedMeta.clearCustomEffects();
        for (PotionEffect effect : definition.toPotionEffectsForCrafting()) {
            updatedMeta.addCustomEffect(effect, true);
        }
        updatedMeta.setDisplayName(definition.displayName());

        PersistentDataContainer container = updatedMeta.getPersistentDataContainer();
        container.set(customIdKey, PersistentDataType.STRING, definition.id());
        if (!definition.customEffects().isEmpty()) {
            container.set(customEffectsKey, PersistentDataType.INTEGER, definition.customEffects().size());
        } else {
            container.remove(customEffectsKey);
        }
        if (definition.scaryToDrink()) {
            container.set(scaryToDrinkKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            container.remove(scaryToDrinkKey);
        }
        updateLore(updatedMeta);

        potion.setItemMeta(updatedMeta);
        return potion;
    }

    private boolean canApplyDefinition(PotionMeta meta, HardcorePotionDefinition targetDefinition) {
        if (isAwkwardPotion(meta) && meta.getPersistentDataContainer().get(customIdKey, PersistentDataType.STRING) == null) {
            return targetDefinition.allowFromAwkward();
        }
        String currentId = meta.getPersistentDataContainer().get(customIdKey, PersistentDataType.STRING);
        if (currentId == null) {
            return false;
        }
        HardcorePotionDefinition current = definitionRegistry.getById(currentId);
        if (current == null) {
            return false;
        }
        return current.branches().contains(targetDefinition.id())
                || targetDefinition.branchBrews().contains(currentId);
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
        boolean scaryToDrink = container.has(scaryToDrinkKey, PersistentDataType.BYTE);
        int customEffectsCount = container.getOrDefault(customEffectsKey, PersistentDataType.INTEGER, 0);

        List<String> lore = new ArrayList<>();
        if (customId != null) {
            lore.add("§8Custom Brew");
        }
        if (customEffectsCount > 0) {
            lore.add("§8Custom Effects: " + customEffectsCount);
        }
        if (scaryToDrink) {
            lore.add("§4Scary to Drink");
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

    private record CustomCraftingMatch(ItemStack potion, Material ingredient) {
    }
}
