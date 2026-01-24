package com.lastbreath.hc.lastBreathHC.asteroid;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantPage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class AsteroidLoot {

    private static final Random random = new Random();

    public static Inventory createLoot(int tier) {
        int normalizedTier = Math.max(1, Math.min(3, tier));
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text("☄ Asteroid Core"));

        inv.addItem(new ItemStack(Material.IRON_INGOT, 6 + random.nextInt(14) + (normalizedTier - 1) * 4));
        inv.addItem(new ItemStack(Material.GOLD_INGOT, 4 + random.nextInt(6) + (normalizedTier - 1) * 2));
        inv.addItem(new ItemStack(Material.EMERALD, 2 + random.nextInt(6) + (normalizedTier - 1) * 2));
        inv.addItem(new ItemStack(Material.DIAMOND, 1 + random.nextInt(4) + (normalizedTier - 1)));
        inv.addItem(new ItemStack(Material.NETHERITE_SCRAP, 1 + (normalizedTier >= 2 ? random.nextInt(2) : 0) + (normalizedTier == 3 ? 1 : 0)));

        if (random.nextInt(100) < chanceForTier(normalizedTier, 50, 65, 80))
            inv.addItem(new ItemStack(Material.TOTEM_OF_UNDYING));

        if (random.nextInt(100) < chanceForTier(normalizedTier, 20, 35, 50))
            inv.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));

        if (random.nextInt(100) < chanceForTier(normalizedTier, 15, 30, 45))
            inv.addItem(new ItemStack(Material.NETHER_STAR));

        if (random.nextInt(100) < chanceForTier(normalizedTier, 10, 25, 40))
            inv.addItem(new ItemStack(Material.ANCIENT_DEBRIS));

        if (random.nextInt(100) < chanceForTier(normalizedTier, 10, 20, 30))
            inv.addItem(new ItemStack(Material.DRAGON_BREATH));

        if (random.nextInt(100) < chanceForTier(normalizedTier, 5, 12, 20))
            inv.addItem(new ItemStack(Material.HEAVY_CORE));

        if (random.nextInt(100) < chanceForTier(normalizedTier, 1, 3, 5))
            inv.addItem(new ItemStack(Material.DRAGON_EGG));

        addEnchantPageLoot(inv);

        return inv;
    }

    private static void addEnchantPageLoot(Inventory inv) {
        FileConfiguration config = LastBreathHC.getInstance().getConfig();
        double chance = config.getDouble("asteroid.loot.enchantPages.chance", 0.5);
        if (chance <= 0) {
            return;
        }
        if (random.nextDouble() * 100 >= chance) {
            return;
        }
        java.util.List<String> ids = config.getStringList("asteroid.loot.enchantPages.ids");
        if (ids.isEmpty()) {
            return;
        }
        String enchantId = ids.get(random.nextInt(ids.size()));
        inv.addItem(CustomEnchantPage.create(enchantId));
    }

    private static int chanceForTier(int tier, int tier1, int tier2, int tier3) {
        return switch (tier) {
            case 3 -> tier3;
            case 2 -> tier2;
            default -> tier1;
        };
    }
}
