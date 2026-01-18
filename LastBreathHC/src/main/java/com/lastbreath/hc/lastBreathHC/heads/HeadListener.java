package com.lastbreath.hc.lastBreathHC.heads;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;

public class HeadListener implements Listener {

    /* =======================
       PLAYER DEATH
       ======================= */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        var player = e.getEntity();

        // COPY & CLEAR ender chest
        Inventory inv = Bukkit.createInventory(
                null, 27,
                Component.text("☠ " + player.getName() + "'s Ender Chest")
        );
        inv.setContents(player.getEnderChest().getContents());
        player.getEnderChest().clear(); // IMPORTANT

        HeadManager.store(player.getUniqueId(), inv);

        // Create head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(player);
        meta.displayName(Component.text("☠ " + player.getName()));
        meta.getPersistentDataContainer().set(
                HeadManager.getKey(),
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );

        head.setItemMeta(meta);

        Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), head);
        dropped.setGlowing(true);
    }

    /* =======================
       PREVENT DESPAWN / DAMAGE
       ======================= */

    @EventHandler
    public void onDespawn(ItemDespawnEvent e) {
        if (isHead(e.getEntity().getItemStack())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e) {
        if (e.getBlock().getState() instanceof Skull skull &&
                skull.getPersistentDataContainer().has(
                        HeadManager.getKey(),
                        PersistentDataType.STRING)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(block ->
                block.getState() instanceof Skull skull &&
                        skull.getPersistentDataContainer().has(
                                HeadManager.getKey(),
                                PersistentDataType.STRING));
    }

    /* =======================
       PLACE HEAD
       ======================= */
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (!isHead(item)) return;

        if (!(e.getBlockPlaced().getState() instanceof Skull skull)) return;

        String uuid = ((SkullMeta) item.getItemMeta())
                .getPersistentDataContainer()
                .get(HeadManager.getKey(), PersistentDataType.STRING);

        skull.getPersistentDataContainer().set(
                HeadManager.getKey(),
                PersistentDataType.STRING,
                uuid
        );

        skull.update();
    }

    /* =======================
       OPEN HEAD
       ======================= */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (!(e.getClickedBlock().getState() instanceof Skull skull)) return;

        var pdc = skull.getPersistentDataContainer();
        if (!pdc.has(HeadManager.getKey(), PersistentDataType.STRING)) return;

        e.setCancelled(true);

        UUID uuid = UUID.fromString(
                pdc.get(HeadManager.getKey(), PersistentDataType.STRING)
        );

        Inventory inv = HeadManager.get(uuid);
        if (inv == null) {
            e.getPlayer().sendMessage("§cThis soul has already been claimed.");
            return;
        }

        e.getPlayer().openInventory(inv);
    }

    /* =======================
       BREAK LOGIC
       ======================= */
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!(e.getBlock().getState() instanceof Skull skull)) return;

        // OPs can always break
        if (e.getPlayer().isOp()) return;

        var pdc = skull.getPersistentDataContainer();
        if (!pdc.has(HeadManager.getKey(), PersistentDataType.STRING)) return;

        UUID uuid = UUID.fromString(
                Objects.requireNonNull(pdc.get(HeadManager.getKey(), PersistentDataType.STRING))
        );

        // Block only if loot still exists
        if (HeadManager.has(uuid)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cThis head still contains a soul.");
        }
    }


    /* =======================
       CLEANUP WHEN LOOTED
       ======================= */
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();

        UUID target = null;
        for (var entry : HeadManager.getAll().entrySet()) {
            if (entry.getValue().equals(inv)) {
                target = entry.getKey();
                break;
            }
        }

        if (target == null) return;

        boolean empty = true;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                empty = false;
                break;
            }
        }

        if (!empty) return;

        // Remove from manager
        HeadManager.remove(target);

        // Remove protection from placed skull
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof Skull skull)) continue;

                    var pdc = skull.getPersistentDataContainer();
                    if (!pdc.has(HeadManager.getKey(), PersistentDataType.STRING)) continue;

                    String stored = pdc.get(HeadManager.getKey(), PersistentDataType.STRING);
                    if (stored != null && stored.equals(target.toString())) {
                        pdc.remove(HeadManager.getKey());
                        skull.update();
                    }
                }
            }
        }
    }


    /* =======================
       UTIL
       ======================= */
    private boolean isHead(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return false;
        if (!(item.getItemMeta() instanceof SkullMeta meta)) return false;

        return meta.getPersistentDataContainer().has(
                HeadManager.getKey(),
                PersistentDataType.STRING
        );
    }
}
