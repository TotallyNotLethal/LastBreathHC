package com.lastbreath.hc.lastBreathHC.cosmetics;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class BowTrailListener implements Listener {

    private static final String TRAIL_LORE_PREFIX = ChatColor.DARK_PURPLE + "Bow Trail: ";

    private final LastBreathHC plugin;
    private final NamespacedKey bowTrailKey;
    private final Map<UUID, BowTrailType> trackedArrows = new HashMap<>();

    public BowTrailListener(LastBreathHC plugin) {
        this.plugin = plugin;
        this.bowTrailKey = new NamespacedKey(plugin, "bow-trail-type");
        startTrailTask();
    }

    @EventHandler
    public void onPrepareBowTrailCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        CraftingMatch match = getCraftingMatch(inventory.getMatrix());
        if (match == null) {
            return;
        }
        BowTrailType trailType = BowTrailType.fromInput(CosmeticTokenHelper.getTokenId(match.trailToken()));
        if (trailType == null) {
            return;
        }

        ItemStack result = match.bow().clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(bowTrailKey, PersistentDataType.STRING, trailType.name());
        java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
        lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Bow Trail:"));
        lore.add(TRAIL_LORE_PREFIX + ChatColor.LIGHT_PURPLE + trailType.displayName());
        meta.setLore(lore);
        result.setItemMeta(meta);
        inventory.setResult(result);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onCraftBowTrail(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
        CraftingMatch match = getCraftingMatch(inventory.getMatrix());
        if (match == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() != Material.BOW) {
            event.setCancelled(true);
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Applied bow trail: " + ChatColor.LIGHT_PURPLE
                + getAppliedTrailName(current) + ChatColor.GRAY + ".");
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        ItemStack bow = event.getBow();
        BowTrailType trailType = getTrailFromBow(bow);
        if (trailType == null) {
            return;
        }
        trackedArrows.put(arrow.getUniqueId(), trailType);
    }

    private BowTrailType getTrailFromBow(ItemStack bow) {
        if (bow == null || bow.getType() != Material.BOW || !bow.hasItemMeta()) {
            return null;
        }
        String trailId = bow.getItemMeta().getPersistentDataContainer().get(bowTrailKey, PersistentDataType.STRING);
        return BowTrailType.fromInput(trailId);
    }

    private String getAppliedTrailName(ItemStack bow) {
        BowTrailType type = getTrailFromBow(bow);
        return type == null ? "Unknown" : type.displayName();
    }

    private CraftingMatch getCraftingMatch(ItemStack[] matrix) {
        if (matrix == null || matrix.length == 0) {
            return null;
        }
        ItemStack bow = null;
        ItemStack token = null;
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (item.getType() == Material.BOW) {
                if (bow != null) {
                    return null;
                }
                bow = item;
                continue;
            }
            CosmeticTokenType tokenType = CosmeticTokenHelper.getTokenType(item);
            if (tokenType != CosmeticTokenType.BOW_TRAIL) {
                return null;
            }
            if (token != null) {
                return null;
            }
            token = item;
        }
        if (bow == null || token == null) {
            return null;
        }
        return new CraftingMatch(bow, token);
    }

    private void startTrailTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedArrows.isEmpty()) {
                    return;
                }
                Iterator<Map.Entry<UUID, BowTrailType>> iterator = trackedArrows.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, BowTrailType> entry = iterator.next();
                    Entity entity = plugin.getServer().getEntity(entry.getKey());
                    if (!(entity instanceof AbstractArrow arrow) || arrow.isDead() || arrow.isInBlock() || arrow.isOnGround()) {
                        iterator.remove();
                        continue;
                    }
                    arrow.getWorld().spawnParticle(entry.getValue().particle(), arrow.getLocation(), 4,
                            0.06, 0.06, 0.06, 0.002);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private record CraftingMatch(ItemStack bow, ItemStack trailToken) {
    }
}
