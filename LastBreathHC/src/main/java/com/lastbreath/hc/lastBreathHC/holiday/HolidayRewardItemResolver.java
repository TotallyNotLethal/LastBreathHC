package com.lastbreath.hc.lastBreathHC.holiday;

import com.lastbreath.hc.lastBreathHC.cosmetics.BossAura;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossKillMessage;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossPrefix;
import com.lastbreath.hc.lastBreathHC.cosmetics.BowTrailType;
import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticTokenHelper;
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

        Optional<ItemStack> cosmeticToken = resolveCosmeticToken(customItemId.trim().toLowerCase(Locale.ROOT));
        if (cosmeticToken.isPresent()) {
            ItemStack token = cosmeticToken.get();
            token.setAmount(Math.max(1, amount));
            return Optional.of(token);
        }

        Supplier<ItemStack> supplier = CUSTOM_ITEM_SUPPLIERS.get(customItemId.trim().toLowerCase(Locale.ROOT));
        if (supplier == null) {
            return Optional.empty();
        }

        ItemStack item = supplier.get();
        item.setAmount(Math.max(1, amount));
        return Optional.of(item);
    }

    private static Optional<ItemStack> resolveCosmeticToken(String customItemId) {
        if (customItemId.startsWith("cosmetic_prefix_")) {
            BossPrefix prefix = BossPrefix.fromInput(customItemId.substring("cosmetic_prefix_".length()));
            return prefix == null ? Optional.empty() : Optional.of(CosmeticTokenHelper.createPrefixToken(prefix));
        }
        if (customItemId.startsWith("cosmetic_aura_")) {
            BossAura aura = BossAura.fromInput(customItemId.substring("cosmetic_aura_".length()));
            return aura == null ? Optional.empty() : Optional.of(CosmeticTokenHelper.createAuraToken(aura));
        }
        if (customItemId.startsWith("cosmetic_kill_message_")) {
            BossKillMessage killMessage = BossKillMessage.fromInput(customItemId.substring("cosmetic_kill_message_".length()));
            return killMessage == null ? Optional.empty() : Optional.of(CosmeticTokenHelper.createKillMessageToken(killMessage));
        }
        if (customItemId.startsWith("cosmetic_bow_trail_")) {
            BowTrailType bowTrailType = BowTrailType.fromInput(customItemId.substring("cosmetic_bow_trail_".length()));
            return bowTrailType == null ? Optional.empty() : Optional.of(CosmeticTokenHelper.createBowTrailToken(bowTrailType));
        }
        return Optional.empty();
    }
}
