package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class InfluenceItemHandler implements Listener {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;
    private final CaptainEntityBinder binder;
    private final TerritoryPressureService pressureService;
    private final LoyaltyService loyaltyService;
    private final boolean enabled;

    public InfluenceItemHandler(LastBreathHC plugin, CaptainRegistry registry, CaptainEntityBinder binder, TerritoryPressureService pressureService, LoyaltyService loyaltyService) {
        this.plugin = plugin;
        this.registry = registry;
        this.binder = binder;
        this.pressureService = pressureService;
        this.loyaltyService = loyaltyService;
        this.enabled = plugin.getConfig().getBoolean("nemesis.influenceItems.enabled", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!enabled) {
            return;
        }
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        Optional<CaptainRecord> recordOpt = binder.resolveCaptainRecord(event.getRightClicked());
        if (recordOpt.isEmpty() || item.getType() == Material.AIR) {
            return;
        }
        CaptainRecord record = recordOpt.get();
        String marker = itemMarker(item);
        if (marker == null) {
            return;
        }
        event.setCancelled(true);
        switch (marker) {
            case "IntelToken" -> applyIntelToken(event.getPlayer(), record);
            case "BribeToken" -> applyBribeToken(event.getPlayer(), record);
            case "ProvocationSigil" -> applyProvocation(event.getPlayer(), record, event.getRightClicked());
            default -> {
                return;
            }
        }
        consumeOne(item, event.getPlayer());
    }

    private void applyIntelToken(Player player, CaptainRecord record) {
        player.sendMessage("§bIntel: §f" + record.naming().displayName() + " §7lvl §f" + record.progression().level());
    }

    private void applyBribeToken(Player player, CaptainRecord record) {
        CaptainRecord.Social social = record.social().orElse(new CaptainRecord.Social(0.4, 0.0, 0.4, 0.5));
        CaptainRecord.Social shifted = new CaptainRecord.Social(Math.min(1.0, social.loyalty() + 0.15), social.fear(), social.ambition(), social.confidence());
        CaptainRecord updated = NemesisTelemetry.withSocial(record, shifted);
        updated = NemesisTelemetry.incrementCounter(updated, "loyaltyShifts", 1);
        registry.upsert(updated);
        String region = updated.political().map(CaptainRecord.Political::region).orElse("unassigned");
        pressureService.applyChange(region, "bribe_token", -8.0);
        updated = NemesisTelemetry.incrementCounter(updated, "pressureChanges", 1);
        registry.upsert(updated);
        player.sendMessage("§aBribe accepted. Territorial pressure lowered.");
    }

    private void applyProvocation(Player player, CaptainRecord record, org.bukkit.entity.Entity clicked) {
        String region = record.political().map(CaptainRecord.Political::region).orElse("unassigned");
        pressureService.applyChange(region, "provocation_sigil", 12.0);
        registry.upsert(NemesisTelemetry.incrementCounter(record, "pressureChanges", 1));
        loyaltyService.evaluateBetrayal(clicked, clicked);
        player.sendMessage("§cProvocation planted. Rivalry pressure increased.");
    }

    private String itemMarker(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        String stripped = meta.getDisplayName().replace("§", "");
        if (stripped.contains("Intel Token")) {
            return "IntelToken";
        }
        if (stripped.contains("Bribe Token")) {
            return "BribeToken";
        }
        if (stripped.contains("Provocation Sigil")) {
            return "ProvocationSigil";
        }
        return null;
    }

    private void consumeOne(ItemStack stack, Player player) {
        stack.setAmount(Math.max(0, stack.getAmount() - 1));
        player.getInventory().setItemInMainHand(stack.getAmount() <= 0 ? null : stack);
    }
}
