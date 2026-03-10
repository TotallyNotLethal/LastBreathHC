package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class Gracestone {

    public static final NamespacedKey KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "gracestone");
    public static final NamespacedKey LIVES_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "gracestone_lives");
    public static final NamespacedKey GRACESTONE_HEALTH_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "gracestone_health");
    public static final NamespacedKey PVP_LAST_HIT_AT_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "gracestone_pvp_last_hit_at");
    public static final long PVP_GRACESTONE_DISABLE_DURATION_MS = 5L * 60L * 1000L;
    private static final PotionEffectType DISPLAY_EFFECT = PotionEffectType.LUCK;

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§fGracestone");
        meta.setLore(List.of(
                "§7Grants +1 Gracestone Life.",
                "§7Lives stack and persist until used.",
                "§7+1 full heart (2 HP) per life, up to +10 hearts."
        ));

        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(
                KEY,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isGracestone(ItemStack item) {
        return item != null &&
                item.getType() == Material.HEART_OF_THE_SEA &&
                item.hasItemMeta() &&
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .has(KEY, PersistentDataType.BYTE);
    }

    public static int getLives(Player player) {
        return player.getPersistentDataContainer()
                .getOrDefault(LIVES_KEY, PersistentDataType.INTEGER, 0);
    }

    public static void addLives(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        int lives = getLives(player) + amount;
        setLives(player, lives);
    }

    public static boolean consumeLife(Player player) {
        if (isSuppressedByPvpCombat(player)) {
            return false;
        }
        int lives = getLives(player);
        if (lives <= 0) {
            return false;
        }
        setLives(player, lives - 1);
        return true;
    }

    public static void refreshDisplay(Player player) {
        int lives = getLives(player);
        if (lives <= 0 || isSuppressedByPvpCombat(player)) {
            clearDisplay(player);
            applyHealthModifier(player, 0);
            return;
        }
        applyDisplay(player, lives);
        applyHealthModifier(player, lives);
    }

    public static void markPvpCombat(Player player) {
        player.getPersistentDataContainer().set(
                PVP_LAST_HIT_AT_KEY,
                PersistentDataType.LONG,
                System.currentTimeMillis()
        );
    }

    public static boolean isSuppressedByPvpCombat(Player player) {
        Long lastHitAt = player.getPersistentDataContainer().get(
                PVP_LAST_HIT_AT_KEY,
                PersistentDataType.LONG
        );
        if (lastHitAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - lastHitAt >= PVP_GRACESTONE_DISABLE_DURATION_MS) {
            player.getPersistentDataContainer().remove(PVP_LAST_HIT_AT_KEY);
            return false;
        }
        return true;
    }

    private static void setLives(Player player, int lives) {
        if (lives <= 0) {
            player.getPersistentDataContainer().remove(LIVES_KEY);
            clearDisplay(player);
            applyHealthModifier(player, lives);
            return;
        }
        player.getPersistentDataContainer().set(
                LIVES_KEY,
                PersistentDataType.INTEGER,
                lives
        );
        applyDisplay(player, lives);
        applyHealthModifier(player, lives);
    }

    private static void applyDisplay(Player player, int lives) {
        int amplifier = Math.max(0, Math.min(255, lives - 1));
        PotionEffect effect = new PotionEffect(
                DISPLAY_EFFECT,
                PotionEffect.INFINITE_DURATION,
                amplifier,
                true,
                false,
                true
        );
        player.addPotionEffect(effect, true);
    }

    private static void clearDisplay(Player player) {
        player.removePotionEffect(DISPLAY_EFFECT);
    }

    private static void applyHealthModifier(Player player, int lives) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        int cappedLives = Math.min(lives, 10);
        double bonus = cappedLives * 2.0;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (GRACESTONE_HEALTH_KEY.equals(modifier.getKey())) {
                attribute.removeModifier(modifier);
            }
        }
        if (bonus > 0) {
            attribute.addModifier(new AttributeModifier(
                    GRACESTONE_HEALTH_KEY,
                    bonus,
                    AttributeModifier.Operation.ADD_NUMBER
            ));
        }
    }
}
