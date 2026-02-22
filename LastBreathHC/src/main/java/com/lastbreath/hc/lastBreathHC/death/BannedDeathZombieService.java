package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.heads.HeadManager;
import com.lastbreath.hc.lastBreathHC.heads.HeadTrackingLogger;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BannedDeathZombieService implements Listener {

    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 30L; // every 30 minutes
    private static final long ACTIVE_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final String STORAGE_PATH = "deathZombieLastSpawn";
    private static final String REMNANT_TAG = "lastbreathhc.remnantZombie";

    private final LastBreathHC plugin;
    private final HeadTrackingLogger headTrackingLogger;
    private final NamespacedKey remnantOwnerKey;
    private final Map<UUID, LocalDate> lastSpawnDateByPlayer = new HashMap<>();
    private BukkitTask task;

    public BannedDeathZombieService(LastBreathHC plugin, HeadTrackingLogger headTrackingLogger) {
        this.plugin = plugin;
        this.headTrackingLogger = headTrackingLogger;
        this.remnantOwnerKey = new NamespacedKey(plugin, "remnant-owner");
    }

    public void start() {
        loadState();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runSweep, 100L, CHECK_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
        saveState();
    }

    @EventHandler
    public void onRemnantDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) {
            return;
        }
        if (!zombie.getScoreboardTags().contains(REMNANT_TAG)) {
            return;
        }

        String ownerUuidRaw = zombie.getPersistentDataContainer().get(remnantOwnerKey, PersistentDataType.STRING);
        if (ownerUuidRaw == null || ownerUuidRaw.isBlank()) {
            return;
        }

        UUID ownerUuid;
        try {
            ownerUuid = UUID.fromString(ownerUuidRaw);
        } catch (IllegalArgumentException ex) {
            return;
        }

        ItemStack ownerHead = createOwnedHead(Bukkit.getOfflinePlayer(ownerUuid));
        event.getDrops().removeIf(this::isStoredPlayerHead);
        event.getDrops().add(ownerHead);
    }

    private void runSweep() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Set<UUID> protectedHeadOwners = resolveProtectedHeadOwners();

        for (BanEntry<?> entry : Bukkit.getBanList(BanList.Type.NAME).getBanEntries()) {
            String playerName = entry.getTarget();
            if (playerName == null || playerName.isBlank()) {
                continue;
            }

            OfflinePlayer bannedPlayer = Bukkit.getOfflinePlayer(playerName);
            if (!bannedPlayer.isBanned()) {
                continue;
            }

            UUID playerId = bannedPlayer.getUniqueId();
            if (playerId == null) {
                continue;
            }

            if (today.equals(lastSpawnDateByPlayer.get(playerId))) {
                continue;
            }

            if (protectedHeadOwners.contains(playerId)) {
                continue;
            }

            if (headTrackingLogger.hasPlacedHead(playerId)) {
                continue;
            }

            Location deathLocation = bannedPlayer.getLastDeathLocation();
            if (deathLocation == null || deathLocation.getWorld() == null) {
                continue;
            }

            spawnZombieWithHead(bannedPlayer, deathLocation);
            lastSpawnDateByPlayer.put(playerId, today);
        }

        saveState();
    }

    private void spawnZombieWithHead(OfflinePlayer player, Location location) {
        Zombie zombie = location.getWorld().spawn(location, Zombie.class);
        UUID ownerUuid = player.getUniqueId();

        zombie.getEquipment().setHelmet(createOwnedHead(player));
        zombie.getEquipment().setHelmetDropChance(1.0f);
        zombie.setRemoveWhenFarAway(false);
        zombie.setPersistent(true);
        zombie.setAdult();
        zombie.setCanBreakDoors(true);
        zombie.setShouldBurnInDay(false);
        zombie.setCustomName("§8Remnant of " + (player.getName() == null ? "Unknown" : player.getName()));
        zombie.setCustomNameVisible(true);
        zombie.addScoreboardTag(REMNANT_TAG);

        if (ownerUuid != null) {
            zombie.getPersistentDataContainer().set(remnantOwnerKey, PersistentDataType.STRING, ownerUuid.toString());
        }
    }

    private ItemStack createOwnedHead(OfflinePlayer player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.getPersistentDataContainer().set(
                HeadManager.getKey(),
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );
        head.setItemMeta(meta);
        return head;
    }

    private Set<UUID> resolveProtectedHeadOwners() {
        Set<UUID> protectedOwners = new HashSet<>();
        long now = System.currentTimeMillis();

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (!hasLoggedInWithinWindow(offlinePlayer, now)) {
                continue;
            }

            if (offlinePlayer.getPlayer() == null) {
                continue;
            }

            scanInventoryForHeads(offlinePlayer.getPlayer().getInventory(), protectedOwners);
            scanInventoryForHeads(offlinePlayer.getPlayer().getEnderChest(), protectedOwners);
        }

        return protectedOwners;
    }

    private boolean hasLoggedInWithinWindow(OfflinePlayer player, long nowMs) {
        long lastPlayed = player.getLastPlayed();
        return lastPlayed > 0L && (nowMs - lastPlayed) <= ACTIVE_WINDOW_MS;
    }

    private void scanInventoryForHeads(Inventory inventory, Set<UUID> protectedOwners) {
        if (inventory == null) {
            return;
        }

        for (ItemStack item : inventory.getContents()) {
            collectHeadOwners(item, protectedOwners);
        }
    }

    private void collectHeadOwners(ItemStack rootItem, Set<UUID> protectedOwners) {
        if (rootItem == null || rootItem.getType() == Material.AIR) {
            return;
        }

        Deque<ItemStack> stack = new ArrayDeque<>();
        stack.push(rootItem);

        while (!stack.isEmpty()) {
            ItemStack current = stack.pop();
            if (current == null || current.getType() == Material.AIR) {
                continue;
            }

            if (isStoredPlayerHead(current)) {
                String ownerId = ((SkullMeta) current.getItemMeta())
                        .getPersistentDataContainer()
                        .get(HeadManager.getKey(), PersistentDataType.STRING);
                if (ownerId != null) {
                    try {
                        protectedOwners.add(UUID.fromString(ownerId));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            ItemMeta meta = current.getItemMeta();
            if (meta == null) {
                continue;
            }

            if (meta instanceof BundleMeta bundleMeta) {
                for (ItemStack bundledItem : bundleMeta.getItems()) {
                    if (bundledItem != null && bundledItem.getType() != Material.AIR) {
                        stack.push(bundledItem.clone());
                    }
                }
            }

            if (meta instanceof BlockStateMeta blockStateMeta
                    && blockStateMeta.getBlockState() instanceof org.bukkit.block.Container container) {
                for (ItemStack contained : container.getInventory().getContents()) {
                    if (contained != null && contained.getType() != Material.AIR) {
                        stack.push(contained.clone());
                    }
                }
            }
        }
    }

    private boolean isStoredPlayerHead(ItemStack item) {
        if (item.getType() != Material.PLAYER_HEAD) {
            return false;
        }
        if (!(item.getItemMeta() instanceof SkullMeta skullMeta)) {
            return false;
        }

        return skullMeta.getPersistentDataContainer().has(HeadManager.getKey(), PersistentDataType.STRING);
    }

    private void loadState() {
        lastSpawnDateByPlayer.clear();
        if (!plugin.getConfig().isConfigurationSection(STORAGE_PATH)) {
            return;
        }

        for (String key : plugin.getConfig().getConfigurationSection(STORAGE_PATH).getKeys(false)) {
            String rawDate = plugin.getConfig().getString(STORAGE_PATH + "." + key);
            if (rawDate == null || rawDate.isBlank()) {
                continue;
            }
            try {
                lastSpawnDateByPlayer.put(UUID.fromString(key), LocalDate.parse(rawDate));
            } catch (Exception ignored) {
            }
        }
    }

    private void saveState() {
        plugin.getConfig().set(STORAGE_PATH, null);
        for (Map.Entry<UUID, LocalDate> entry : lastSpawnDateByPlayer.entrySet()) {
            plugin.getConfig().set(STORAGE_PATH + "." + entry.getKey(), entry.getValue().toString());
        }
        plugin.saveConfig();
    }
}
