package com.lastbreath.hc.lastBreathHC.items;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum CustomEnchant {
    DIRECTIONAL_MINING("lb:directional_mining", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    VEIN_MINER("lb:vein_miner", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    AUTO_PICKUP("lb:auto_pickup", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    AUTO_REPLANT("lb:auto_replant", false, EnumSet.of(Material.NETHERITE_HOE)),
    TREE_FELLER("lb:tree_feller", false, EnumSet.of(Material.NETHERITE_AXE)),
    EXCAVATOR("lb:excavator", false, EnumSet.of(Material.NETHERITE_SHOVEL)),
    FERTILE_HARVEST("lb:fertile_harvest", false, EnumSet.of(Material.NETHERITE_HOE)),
    SMELTER_TOUCH("lb:smelter_touch", false, EnumSet.of(Material.NETHERITE_PICKAXE)),
    SWIFT_TILLER("lb:swift_tiller", false, EnumSet.of(Material.NETHERITE_HOE)),
    PROSPECTOR("lb:prospector", false, EnumSet.of(Material.NETHERITE_PICKAXE));

    private final String id;
    private final boolean pvp;
    private final Set<Material> allowedTools;

    CustomEnchant(String id, boolean pvp, Set<Material> allowedTools) {
        this.id = id;
        this.pvp = pvp;
        this.allowedTools = allowedTools;
    }

    public String getId() {
        return id;
    }

    public boolean isPvp() {
        return pvp;
    }

    public boolean isAllowedTool(Material material) {
        return material != null && allowedTools.contains(material);
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

    public static boolean isAllowedForTool(String id, Material material) {
        CustomEnchant enchant = fromId(id);
        return enchant != null && !enchant.isPvp() && enchant.isAllowedTool(material);
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
