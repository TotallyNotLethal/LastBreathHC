package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
    private static final PotionEffectType DISPLAY_EFFECT = PotionEffectType.LUCK;

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§fGracestone");
        meta.setLore(List.of(
                "§7Grants +1 Gracestone Life.",
                "§7Lives stack and persist until used."
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
        int lives = getLives(player);
        if (lives <= 0) {
            return false;
        }
        setLives(player, lives - 1);
        return true;
    }

    public static void refreshDisplay(Player player) {
        int lives = getLives(player);
        if (lives <= 0) {
            clearDisplay(player);
            return;
        }
        applyDisplay(player, lives);
    }

    private static void setLives(Player player, int lives) {
        if (lives <= 0) {
            player.getPersistentDataContainer().remove(LIVES_KEY);
            clearDisplay(player);
            return;
        }
        player.getPersistentDataContainer().set(
                LIVES_KEY,
                PersistentDataType.INTEGER,
                lives
        );
        applyDisplay(player, lives);
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
}
