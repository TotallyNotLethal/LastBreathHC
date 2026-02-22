package com.lastbreath.hc.lastBreathHC.heads;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.revive.ReviveStateManager;
import com.lastbreath.hc.lastBreathHC.token.ReviveTokenHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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

    private final LastBreathHC plugin;
    private final HeadTrackingLogger headTrackingLogger;

    public HeadListener(LastBreathHC plugin, HeadTrackingLogger headTrackingLogger) {
        this.plugin = plugin;
        this.headTrackingLogger = headTrackingLogger;
    }

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
        UUID recordId = UUID.randomUUID();
        meta.getPersistentDataContainer().set(
                HeadManager.getRecordKey(),
                PersistentDataType.STRING,
                recordId.toString()
        );

        head.setItemMeta(meta);

        Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), head);
        dropped.setGlowing(true);
        headTrackingLogger.recordHeadDropped(
                player.getUniqueId(),
                player.getName(),
                recordId,
                dropped.getUniqueId(),
                dropped.getLocation()
        );
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
        String recordIdRaw = ((SkullMeta) item.getItemMeta())
                .getPersistentDataContainer()
                .get(HeadManager.getRecordKey(), PersistentDataType.STRING);

        skull.getPersistentDataContainer().set(
                HeadManager.getKey(),
                PersistentDataType.STRING,
                uuid
        );
        if (recordIdRaw != null) {
            skull.getPersistentDataContainer().set(
                    HeadManager.getRecordKey(),
                    PersistentDataType.STRING,
                    recordIdRaw
            );
        }

        skull.update();

        try {
            if (uuid != null && recordIdRaw != null) {
                UUID ownerUuid = UUID.fromString(uuid);
                UUID recordId = UUID.fromString(recordIdRaw);
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
                headTrackingLogger.recordHeadPlaced(ownerUuid, owner.getName(), recordId, e.getPlayer(), e.getBlockPlaced().getLocation());
            }
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to parse head owner/record for placement logging.");
        }
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

        String storedUuid = pdc.get(HeadManager.getKey(), PersistentDataType.STRING);
        if (storedUuid == null) {
            e.getPlayer().sendMessage("§cThis head is missing its soul data.");
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(storedUuid);
        } catch (IllegalArgumentException ex) {
            e.getPlayer().sendMessage("§cThis head is corrupted.");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
        String targetName = target.getName();

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (ReviveTokenHelper.hasToken(e.getPlayer())) {
                if (!ReviveTokenHelper.consumeToken(e.getPlayer())) {
                    e.getPlayer().sendMessage("§cYou need a Revival Token to revive this soul.");
                    return;
                }

                if (!HeadManager.has(uuid)) {
                    if (isAdmin(e.getPlayer())) {
                        cleanupGlitchedHead(e.getPlayer(), skull, uuid);
                        return;
                    }
                    e.getPlayer().sendMessage("§cThis soul has already been claimed.");
                    return;
                }

                if (targetName == null || targetName.isBlank()) {
                    e.getPlayer().sendMessage("§cUnable to resolve the soul's name.");
                    return;
                }

                if (!Bukkit.getBanList(BanList.Type.NAME).isBanned(targetName)) {
                    e.getPlayer().sendMessage("§cThis soul is not ban-bound; you cannot revive it.");
                    return;
                }

                Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
                ReviveStateManager.markRevivePending(uuid);
                Bukkit.broadcastMessage("§a✦ " + targetName + " has been revived!");

                Inventory storedInventory = HeadManager.get(uuid);
                Player onlineTarget = Bukkit.getPlayer(uuid);
                if (onlineTarget != null && storedInventory != null) {
                    onlineTarget.getEnderChest().setContents(storedInventory.getContents());
                } else if (storedInventory != null) {
                    HeadManager.storePendingRestore(uuid, storedInventory);
                }

                HeadManager.remove(uuid);
                pdc.remove(HeadManager.getKey());
                skull.update();
                return;
            }
        }

        Inventory inv = HeadManager.get(uuid);
        if (inv == null) {
            if (isAdmin(e.getPlayer())) {
                cleanupGlitchedHead(e.getPlayer(), skull, uuid);
                return;
            }
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

        // Admins can always break
        if (isAdmin(e.getPlayer())) {
            var pdc = skull.getPersistentDataContainer();
            if (pdc.has(HeadManager.getKey(), PersistentDataType.STRING)) {
                String storedOwnerId = pdc.get(HeadManager.getKey(), PersistentDataType.STRING);
                if (storedOwnerId != null) {
                    try {
                        headTrackingLogger.recordHeadUnplaced(UUID.fromString(storedOwnerId), "BROKEN_BY_ADMIN", e.getPlayer(), e.getBlock().getLocation());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return;
        }

        var pdc = skull.getPersistentDataContainer();
        if (!pdc.has(HeadManager.getKey(), PersistentDataType.STRING)) return;

        UUID uuid = UUID.fromString(
                Objects.requireNonNull(pdc.get(HeadManager.getKey(), PersistentDataType.STRING))
        );

        // Block only if loot still exists
        if (HeadManager.has(uuid)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cThis head still contains a soul.");
            return;
        }

        headTrackingLogger.recordHeadUnplaced(uuid, "BROKEN", e.getPlayer(), e.getBlock().getLocation());
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
                        headTrackingLogger.recordHeadUnplaced(target, "SOUL_CLAIMED", null, skull.getLocation());
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

    private boolean isAdmin(Player player) {
        return player.isOp() || player.hasPermission("lastbreathhc.admin");
    }

    private void cleanupGlitchedHead(Player player, Skull skull, UUID uuid) {
        var pdc = skull.getPersistentDataContainer();
        pdc.remove(HeadManager.getKey());
        skull.update();
        player.sendMessage("§eCleared soul data for " + uuid + ". You can now break this head.");
    }
}
