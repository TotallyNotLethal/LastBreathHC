package com.lastbreath.hc.lastBreathHC.asteroid;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossAura;
import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticTokenHelper;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchant;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantPage;
import com.lastbreath.hc.lastBreathHC.items.EnhancedGrindstone;
import com.lastbreath.hc.lastBreathHC.items.Gracestone;
import com.lastbreath.hc.lastBreathHC.items.RebirthStone;
import com.lastbreath.hc.lastBreathHC.items.TotemOfLife;
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
        FileConfiguration config = LastBreathHC.getInstance().getConfig();
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text("☄ Asteroid Core"));

        inv.addItem(new ItemStack(Material.IRON_INGOT, getTieredAmount(config, normalizedTier, "ironIngot",
                switch (normalizedTier) {
                    case 2 -> 10;
                    case 3 -> 14;
                    default -> 6;
                },
                switch (normalizedTier) {
                    case 2 -> 23;
                    case 3 -> 27;
                    default -> 19;
                })));
        inv.addItem(new ItemStack(Material.GOLD_INGOT, getTieredAmount(config, normalizedTier, "goldIngot",
                switch (normalizedTier) {
                    case 2 -> 6;
                    case 3 -> 8;
                    default -> 4;
                },
                switch (normalizedTier) {
                    case 2 -> 11;
                    case 3 -> 13;
                    default -> 9;
                })));
        inv.addItem(new ItemStack(Material.EMERALD, getTieredAmount(config, normalizedTier, "emerald",
                switch (normalizedTier) {
                    case 2 -> 4;
                    case 3 -> 6;
                    default -> 2;
                },
                switch (normalizedTier) {
                    case 2 -> 9;
                    case 3 -> 11;
                    default -> 7;
                })));
        inv.addItem(new ItemStack(Material.DIAMOND, getTieredAmount(config, normalizedTier, "diamond",
                switch (normalizedTier) {
                    case 2 -> 2;
                    case 3 -> 3;
                    default -> 1;
                },
                switch (normalizedTier) {
                    case 2 -> 5;
                    case 3 -> 6;
                    default -> 4;
                })));
        inv.addItem(new ItemStack(Material.NETHERITE_SCRAP, getTieredAmount(config, normalizedTier, "netheriteScrap",
                switch (normalizedTier) {
                    case 2 -> 1;
                    case 3 -> 2;
                    default -> 1;
                },
                switch (normalizedTier) {
                    case 2 -> 2;
                    case 3 -> 3;
                    default -> 1;
                })));

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

        addCustomItemLoot(inv, normalizedTier, config);
        addEnchantPageLoot(inv, normalizedTier);
        addCosmeticAuraLoot(inv, normalizedTier, config);

        return inv;
    }

    private static void addCustomItemLoot(Inventory inv, int tier, FileConfiguration config) {
        if (tier < 2) {
            return;
        }
        if (random.nextInt(100) < getTierChance(config, tier, "customItems.rebirthStoneChance", 5, 10)) {
            inv.addItem(RebirthStone.create());
        }
        if (random.nextInt(100) < getTierChance(config, tier, "customItems.totemOfLifeChance", 5, 10)) {
            inv.addItem(TotemOfLife.create());
        }
        if (random.nextInt(100) < getTierChance(config, tier, "customItems.gracestoneChance", 5, 10)) {
            inv.addItem(Gracestone.create());
        }
        if (random.nextInt(100) < getTierChance(config, tier, "customItems.enhancedGrindstoneChance", 5, 10)) {
            inv.addItem(EnhancedGrindstone.create());
        }
    }

    private static void addEnchantPageLoot(Inventory inv, int tier) {
        FileConfiguration config = LastBreathHC.getInstance().getConfig();
        java.util.List<String> ids = CustomEnchant.filterAllowedIds(
                config.getStringList("asteroid.loot.enchantPages.ids")
        );
        if (ids.isEmpty()) {
            return;
        }
        int pagesAdded = 0;
        double baseChance = getEnchantPageChance(config, tier, 25, 50, 75);
        if (random.nextDouble() * 100 < baseChance) {
            String enchantId = ids.get(random.nextInt(ids.size()));
            inv.addItem(CustomEnchantPage.create(enchantId));
            pagesAdded++;
        }
        java.util.List<Integer> extraRolls = config.getIntegerList(
                "asteroid.loot.enchantPages.extraRolls.tier" + tier
        );
        for (Integer chance : extraRolls) {
            if (chance == null || chance <= 0) {
                continue;
            }
            if (random.nextDouble() * 100 < chance) {
                String enchantId = ids.get(random.nextInt(ids.size()));
                inv.addItem(CustomEnchantPage.create(enchantId));
                pagesAdded++;
            }
        }
        if (tier == 3 && pagesAdded == 0) {
            String enchantId = ids.get(random.nextInt(ids.size()));
            inv.addItem(CustomEnchantPage.create(enchantId));
        }
    }

    private static int getTieredAmount(FileConfiguration config, int tier, String key, int defaultMin, int defaultMax) {
        String basePath = "asteroid.loot.tier" + tier + "." + key;
        int min = config.getInt(basePath + ".min", defaultMin);
        int max = config.getInt(basePath + ".max", defaultMax);
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }
        return min + random.nextInt(max - min + 1);
    }

    private static int chanceForTier(int tier, int tier1, int tier2, int tier3) {
        return switch (tier) {
            case 3 -> tier3;
            case 2 -> tier2;
            default -> tier1;
        };
    }

    private static int getTierChance(FileConfiguration config, int tier, String key, int tier2Default, int tier3Default) {
        String basePath = "asteroid.loot.tier" + tier + "." + key;
        if (tier == 2) {
            return config.getInt(basePath, tier2Default);
        }
        if (tier == 3) {
            return config.getInt(basePath, tier3Default);
        }
        return config.getInt(basePath, 0);
    }

    private static int getEnchantPageChance(FileConfiguration config, int tier, int tier1Default, int tier2Default, int tier3Default) {
        String basePath = "asteroid.loot.enchantPages.tier" + tier + "Chance";
        return switch (tier) {
            case 3 -> config.getInt(basePath, tier3Default);
            case 2 -> config.getInt(basePath, tier2Default);
            default -> config.getInt(basePath, tier1Default);
        };
    }

    private static void addCosmeticAuraLoot(Inventory inv, int tier, FileConfiguration config) {
        if (tier != 3) {
            return;
        }
        String basePath = "asteroid.loot.tier3.cosmetics";
        double chance = config.getDouble(basePath + ".chance", 2.0);
        if (chance <= 0 || random.nextDouble() * 100 >= chance) {
            return;
        }
        java.util.List<String> auraIds = config.getStringList(basePath + ".auras");
        if (auraIds.isEmpty()) {
            return;
        }
        java.util.List<BossAura> auras = auraIds.stream()
                .map(BossAura::fromInput)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (auras.isEmpty()) {
            return;
        }
        BossAura aura = auras.get(random.nextInt(auras.size()));
        inv.addItem(CosmeticTokenHelper.createAuraToken(aura));
    }
}
