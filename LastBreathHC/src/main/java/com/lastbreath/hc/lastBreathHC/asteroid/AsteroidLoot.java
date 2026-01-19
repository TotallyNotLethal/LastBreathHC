package com.lastbreath.hc.lastBreathHC.asteroid;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class AsteroidLoot {

    private static final Random random = new Random();

    public static Inventory createLoot() {
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text("☄ Asteroid Core"));

        inv.addItem(new ItemStack(Material.IRON_INGOT, 6 + random.nextInt(14)));
        inv.addItem(new ItemStack(Material.GOLD_INGOT, 4 + random.nextInt(6)));
        inv.addItem(new ItemStack(Material.EMERALD, 2 + random.nextInt(6)));
        inv.addItem(new ItemStack(Material.DIAMOND, 1 + random.nextInt(4)));
        inv.addItem(new ItemStack(Material.NETHERITE_SCRAP));

        if (random.nextInt(100) < 50)
            inv.addItem(new ItemStack(Material.TOTEM_OF_UNDYING));

        if (random.nextInt(100) < 20)
            inv.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));

        if (random.nextInt(100) < 15)
            inv.addItem(new ItemStack(Material.NETHER_STAR));

        if (random.nextInt(100) < 10)
            inv.addItem(new ItemStack(Material.ANCIENT_DEBRIS));

        if (random.nextInt(100) < 10)
            inv.addItem(new ItemStack(Material.DRAGON_BREATH));

        if (random.nextInt(100) < 5)
            inv.addItem(new ItemStack(Material.HEAVY_CORE));

        if (random.nextInt(100) <= 1)
            inv.addItem(new ItemStack(Material.DRAGON_EGG));

        return inv;
    }
}
