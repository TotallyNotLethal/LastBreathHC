package com.lastbreath.hc.lastBreathHC.items;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class EchoInfuserListener implements Listener {

    private final LastBreathHC plugin;
    private BukkitTask visualTask;

    public EchoInfuserListener(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    public void startVisualTask() {
        if (visualTask != null) {
            visualTask.cancel();
        }
        visualTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickVisuals, 1L, 2L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }
        if (inventory.getMatrix().length < 9) {
            return;
        }

        CraftMatch match = findMatch(inventory.getMatrix());
        if (match == null) {
            return;
        }

        ItemStack result = match.trimmedArmor().clone();
        if (!(result.getItemMeta() instanceof ArmorMeta armorMeta)) {
            return;
        }

        armorMeta.getPersistentDataContainer().set(EchoInfuser.INFUSED_ARMOR_KEY, PersistentDataType.BYTE, (byte) 1);

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        if (armorMeta.lore() != null) {
            lore.addAll(armorMeta.lore());
        }
        lore.add(net.kyori.adventure.text.Component.text("Echo-Infused Trim")
                .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        armorMeta.lore(lore);

        result.setItemMeta(armorMeta);
        inventory.setResult(result);
    }

    private void tickVisuals() {
        Color syncedColor = getSyncedCycleColor();
        TrimMaterial syncedTrimMaterial = getSyncedTrimMaterial();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerInventory inventory = player.getInventory();
            int infusedPieces = 0;
            ItemStack[] armorContents = inventory.getArmorContents();
            for (int slot = 0; slot < armorContents.length; slot++) {
                ItemStack armor = armorContents[slot];
                if (!isInfusedTrimArmor(armor)) {
                    continue;
                }
                cycleTrimMaterial(armor, syncedTrimMaterial);
                inventory.setItem(36 + slot, armor);
                infusedPieces++;
            }
            if (infusedPieces == 0) {
                continue;
            }

            float size = Math.min(1.8F, 0.9F + (infusedPieces * 0.2F));
            Particle.DustOptions dust = new Particle.DustOptions(syncedColor, size);

            for (int i = 0; i < infusedPieces; i++) {
                player.getWorld().spawnParticle(
                        Particle.DUST,
                        player.getLocation().add(0, 1.1, 0),
                        4,
                        0.35,
                        0.55,
                        0.35,
                        0,
                        dust
                );
            }
        }
    }

    private Color getSyncedCycleColor() {
        long now = System.currentTimeMillis();
        float hue = ((now % 2500L) / 2500.0F);
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.95F, 1.0F) & 0x00FFFFFF;
        return Color.fromRGB(rgb);
    }

    private TrimMaterial getSyncedTrimMaterial() {
        List<TrimMaterial> materials = Registry.TRIM_MATERIAL.stream().toList();
        if (materials.isEmpty()) {
            return null;
        }

        long now = System.currentTimeMillis();
        int index = (int) ((now / 800L) % materials.size());
        return materials.get(index);
    }

    private void cycleTrimMaterial(ItemStack armor, TrimMaterial syncedTrimMaterial) {
        if (syncedTrimMaterial == null || !(armor.getItemMeta() instanceof ArmorMeta armorMeta)) {
            return;
        }

        ArmorTrim currentTrim = armorMeta.getTrim();
        if (currentTrim == null || currentTrim.getMaterial().equals(syncedTrimMaterial)) {
            return;
        }

        armorMeta.setTrim(new ArmorTrim(syncedTrimMaterial, currentTrim.getPattern()));
        armor.setItemMeta(armorMeta);
    }

    private CraftMatch findMatch(ItemStack[] matrix) {
        ItemStack infuser = null;
        ItemStack armor = null;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (EchoInfuser.isEchoInfuser(item)) {
                if (infuser != null) {
                    return null;
                }
                infuser = item;
                continue;
            }
            if (isValidTrimmedArmor(item)) {
                if (armor != null) {
                    return null;
                }
                armor = item;
                continue;
            }
            return null;
        }

        if (infuser == null || armor == null) {
            return null;
        }
        return new CraftMatch(infuser, armor);
    }

    private boolean isValidTrimmedArmor(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!(item.getItemMeta() instanceof ArmorMeta armorMeta)) {
            return false;
        }
        if (armorMeta.getTrim() == null) {
            return false;
        }
        return !armorMeta.getPersistentDataContainer().has(EchoInfuser.INFUSED_ARMOR_KEY, PersistentDataType.BYTE);
    }

    private boolean isInfusedTrimArmor(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !(item.getItemMeta() instanceof ArmorMeta armorMeta)) {
            return false;
        }
        return armorMeta.getPersistentDataContainer().has(EchoInfuser.INFUSED_ARMOR_KEY, PersistentDataType.BYTE)
                && armorMeta.getTrim() != null;
    }

    private record CraftMatch(ItemStack infuser, ItemStack trimmedArmor) {
    }
}
