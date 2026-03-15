package com.lastbreath.hc.lastBreathHC.holiday;

import com.lastbreath.hc.lastBreathHC.items.EnhancedGrindstone;
import com.lastbreath.hc.lastBreathHC.items.Gracestone;
import com.lastbreath.hc.lastBreathHC.items.RebirthStone;
import com.lastbreath.hc.lastBreathHC.items.TotemOfLife;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class HolidayRewardItemResolver {

    private static final Map<String, Supplier<ItemStack>> CUSTOM_ITEM_SUPPLIERS = Map.of(
            "rebirth_stone", RebirthStone::create,
            "totem_of_life", TotemOfLife::create,
            "gracestone", Gracestone::create,
            "enhanced_grindstone", EnhancedGrindstone::create
    );

    private HolidayRewardItemResolver() {
    }

    public static Optional<ItemStack> resolve(String customItemId, int amount) {
        if (customItemId == null || customItemId.isBlank()) {
            return Optional.empty();
        }

        Supplier<ItemStack> supplier = CUSTOM_ITEM_SUPPLIERS.get(customItemId.trim().toLowerCase(Locale.ROOT));
        if (supplier == null) {
            return Optional.empty();
        }

        ItemStack item = supplier.get();
        item.setAmount(Math.max(1, amount));
        return Optional.of(item);
    }
}
