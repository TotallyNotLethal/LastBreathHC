package com.lastbreath.hc.lastBreathHC.spectate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public class SpectateSession {

    private final Location returnLocation;
    private final GameMode returnMode;
    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final ItemStack offhand;
    private final float exp;
    private final int level;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final List<PotionEffect> potionEffects;
    private final boolean adminSpectate;

    public SpectateSession(
            Location returnLocation,
            GameMode returnMode,
            ItemStack[] inventoryContents,
            ItemStack[] armorContents,
            ItemStack offhand,
            float exp,
            int level,
            double health,
            int foodLevel,
            float saturation,
            List<PotionEffect> potionEffects,
            boolean adminSpectate
    ) {
        this.returnLocation = returnLocation;
        this.returnMode = returnMode;
        this.inventoryContents = inventoryContents;
        this.armorContents = armorContents;
        this.offhand = offhand;
        this.exp = exp;
        this.level = level;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.potionEffects = potionEffects;
        this.adminSpectate = adminSpectate;
    }

    public Location getReturnLocation() {
        return returnLocation;
    }

    public GameMode getReturnMode() {
        return returnMode;
    }

    public ItemStack[] getInventoryContents() {
        return inventoryContents;
    }

    public ItemStack[] getArmorContents() {
        return armorContents;
    }

    public ItemStack getOffhand() {
        return offhand;
    }

    public float getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public double getHealth() {
        return health;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public List<PotionEffect> getPotionEffects() {
        return potionEffects;
    }

    public boolean isAdminSpectate() {
        return adminSpectate;
    }
}
