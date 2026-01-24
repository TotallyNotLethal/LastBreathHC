package com.lastbreath.hc.lastBreathHC.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum CustomEnchant {
    DIRECTIONAL_MINING("lb:directional_mining", false),
    VEIN_MINER("lb:vein_miner", false),
    AUTO_PICKUP("lb:auto_pickup", false),
    AUTO_REPLANT("lb:auto_replant", false),
    TREE_FELLER("lb:tree_feller", false),
    EXCAVATOR("lb:excavator", false),
    FERTILE_HARVEST("lb:fertile_harvest", false),
    SMELTER_TOUCH("lb:smelter_touch", false),
    SWIFT_TILLER("lb:swift_tiller", false),
    PROSPECTOR("lb:prospector", false);

    private final String id;
    private final boolean pvp;

    CustomEnchant(String id, boolean pvp) {
        this.id = id;
        this.pvp = pvp;
    }

    public String getId() {
        return id;
    }

    public boolean isPvp() {
        return pvp;
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
