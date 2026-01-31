package com.lastbreath.hc.lastBreathHC.items;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum CustomEnchant {
    QUARRY("lb:quarry", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    VEIN_MINER("lb:vein_miner", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    AUTO_PICKUP("lb:auto_pickup", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    AUTO_REPLANT("lb:auto_replant", false, EnumSet.of(Material.NETHERITE_HOE)),
    TREE_FELLER("lb:tree_feller", false, EnumSet.of(Material.NETHERITE_AXE)),
    EXCAVATOR("lb:excavator", false, EnumSet.of(Material.NETHERITE_SHOVEL)),
    FERTILE_HARVEST("lb:fertile_harvest", false, EnumSet.of(Material.NETHERITE_HOE)),
    SMELTER_TOUCH("lb:smelter_touch", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    SWIFT_TILLER("lb:swift_tiller", false, EnumSet.of(Material.NETHERITE_HOE)),
    PROSPECTOR("lb:prospector", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    WITHER_GUARD("lb:wither_guard", false, EnumSet.of(
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS
    )),
    STORM_GUARD("lb:storm_guard", false, EnumSet.of(
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS
    )),
    LIFESTEAL_WARD("lb:lifesteal_ward", false, EnumSet.of(
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS
    )),
    TELEGRAPH_NULL("lb:telegraph_null", false, EnumSet.of(Material.ELYTRA));

    private final String id;
    private final boolean pvp;
    private final Set<Material> allowedMaterials;

    CustomEnchant(String id, boolean pvp, Set<Material> allowedMaterials) {
        this.id = id;
        this.pvp = pvp;
        this.allowedMaterials = allowedMaterials;
    }

    public String getId() {
        return id;
    }

    public boolean isPvp() {
        return pvp;
    }

    public boolean isAllowedItem(Material material) {
        return material != null && allowedMaterials.contains(material);
    }

    public static CustomEnchant fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (CustomEnchant enchant : values()) {
            if (enchant.id.equalsIgnoreCase(normalized)) {
                return enchant;
            }
        }
        return null;
    }

    public static boolean isAllowedEnchantId(String id) {
        CustomEnchant enchant = fromId(id);
        return enchant != null && !enchant.isPvp();
    }

    public static boolean isAllowedForItem(String id, Material material) {
        CustomEnchant enchant = fromId(id);
        return enchant != null && !enchant.isPvp() && enchant.isAllowedItem(material);
    }

    public static List<String> filterAllowedIds(Iterable<String> ids) {
        List<String> allowed = new ArrayList<>();
        if (ids == null) {
            return allowed;
        }
        for (String id : ids) {
            if (isAllowedEnchantId(id)) {
                allowed.add(id);
            }
        }
        return allowed;
    }
}
