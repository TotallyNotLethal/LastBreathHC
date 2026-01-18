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

        inv.addItem(new ItemStack(Material.DIAMOND, 2 + random.nextInt(3)));
        inv.addItem(new ItemStack(Material.NETHERITE_SCRAP));

        if (random.nextInt(100) < 20)
            inv.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));

        if (random.nextInt(100) < 15)
            inv.addItem(new ItemStack(Material.NETHER_STAR));

        if (random.nextInt(100) < 10)
            inv.addItem(new ItemStack(Material.ANCIENT_DEBRIS));

        if (random.nextInt(100) < 10)
            inv.addItem(new ItemStack(Material.DRAGON_BREATH));

        if (random.nextInt(100) < 5)
            inv.addItem(new ItemStack(Material.DRAGON_EGG));

        if (random.nextInt(100) < 5)
            inv.addItem(new ItemStack(Material.HEAVY_CORE));

        return inv;
    }
}
