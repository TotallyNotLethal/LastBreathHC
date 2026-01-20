package com.lastbreath.hc.lastBreathHC.potion;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HardcorePotionDefinition {

    private final String id;
    private final String displayName;
    private final Material craftingIngredient;
    private final boolean scaryToDrink;
    private final boolean allowFromAwkward;
    private final List<EffectDefinition> baseEffects;
    private final List<EffectDefinition> drawbacks;
    private final List<EffectDefinition> afterEffects;
    private final List<CustomEffectDefinition> customEffects;
    private final List<String> branches;

    public HardcorePotionDefinition(String id,
                                    String displayName,
                                    Material craftingIngredient,
                                    boolean scaryToDrink,
                                    boolean allowFromAwkward,
                                    List<EffectDefinition> baseEffects,
                                    List<EffectDefinition> drawbacks,
                                    List<EffectDefinition> afterEffects,
                                    List<CustomEffectDefinition> customEffects,
                                    List<String> branches) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.craftingIngredient = Objects.requireNonNull(craftingIngredient, "craftingIngredient");
        this.scaryToDrink = scaryToDrink;
        this.allowFromAwkward = allowFromAwkward;
        this.baseEffects = List.copyOf(baseEffects);
        this.drawbacks = List.copyOf(drawbacks);
        this.afterEffects = List.copyOf(afterEffects);
        this.customEffects = List.copyOf(customEffects);
        this.branches = List.copyOf(branches);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material craftingIngredient() {
        return craftingIngredient;
    }

    public boolean scaryToDrink() {
        return scaryToDrink;
    }

    public boolean allowFromAwkward() {
        return allowFromAwkward;
    }

    public List<EffectDefinition> baseEffects() {
        return baseEffects;
    }

    public List<EffectDefinition> drawbacks() {
        return drawbacks;
    }

    public List<EffectDefinition> afterEffects() {
        return afterEffects;
    }

    public List<CustomEffectDefinition> customEffects() {
        return customEffects;
    }

    public List<String> branches() {
        return branches;
    }

    public List<PotionEffect> toPotionEffectsForCrafting() {
        if (baseEffects.isEmpty() && drawbacks.isEmpty()) {
            return Collections.emptyList();
        }
        List<PotionEffect> effects = new ArrayList<>();
        for (EffectDefinition definition : baseEffects) {
            effects.add(definition.toPotionEffect());
        }
        for (EffectDefinition definition : drawbacks) {
            effects.add(definition.toPotionEffect());
        }
        return effects;
    }

    public record EffectDefinition(PotionEffectType type,
                                   int durationTicks,
                                   int amplifier,
                                   EffectTrigger trigger,
                                   int cooldownTicks) {
        public PotionEffect toPotionEffect() {
            return new PotionEffect(type, durationTicks, amplifier);
        }
    }

    public record CustomEffectDefinition(String id,
                                         int durationTicks,
                                         EffectTrigger trigger,
                                         int cooldownTicks) {
    }
}
