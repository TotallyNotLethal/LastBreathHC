package com.lastbreath.hc.lastBreathHC.noise;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.SoundEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoiseDampenerManager implements Listener {

    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "Noise Dampener";
    private static final int AREA_RADIUS = 5;
    private static final int RECENT_LIMIT = 45;
    private static final NamespacedKey SOUND_KEY =
            new NamespacedKey(LastBreathHC.getInstance(), "noise_dampener_sound");

    private final Map<BlockKey, NoiseDampenerConfig> dampeners = new HashMap<>();
    private final Map<UUID, BlockKey> openDampeners = new HashMap<>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!NoiseDampener.isNoiseDampener(item)) {
            return;
        }
        BlockKey key = BlockKey.from(event.getBlockPlaced().getLocation());
        dampeners.putIfAbsent(key, new NoiseDampenerConfig(event.getBlockPlaced().getLocation()));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        BlockKey key = BlockKey.from(event.getBlock().getLocation());
        NoiseDampenerConfig config = dampeners.remove(key);
        if (config == null) {
            return;
        }
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                NoiseDampener.create()
        );
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        BlockKey key = BlockKey.from(event.getClickedBlock().getLocation());
        NoiseDampenerConfig config = dampeners.get(key);
        if (config == null) {
            return;
        }
        event.setCancelled(true);
        openGui(event.getPlayer(), key, config);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSound(SoundEvent event) {
        Location location = event.getLocation();
        if (location == null) {
            return;
        }
        String soundKey = resolveSoundKey(event.getSound());
        if (soundKey == null) {
            return;
        }
        for (Map.Entry<BlockKey, NoiseDampenerConfig> entry : dampeners.entrySet()) {
            if (!isInRange(entry.getKey(), location)) {
                continue;
            }
            NoiseDampenerConfig config = entry.getValue();
            trackRecentSound(config, soundKey);
            if (config.getDampenedSoundKeys().contains(soundKey)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        BlockKey key = openDampeners.get(player.getUniqueId());
        if (key == null) {
            return;
        }
        NoiseDampenerConfig config = dampeners.get(key);
        if (config == null) {
            player.closeInventory();
            openDampeners.remove(player.getUniqueId());
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String soundKey = container.get(SOUND_KEY, PersistentDataType.STRING);
        if (soundKey == null) {
            return;
        }
        toggleSound(config, soundKey);
        event.getInventory().setItem(
                event.getSlot(),
                buildSoundItem(soundKey, config.getDampenedSoundKeys().contains(soundKey))
        );
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        openDampeners.remove(event.getPlayer().getUniqueId());
    }

    private void openGui(Player player, BlockKey key, NoiseDampenerConfig config) {
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        inventory.setItem(4, buildHeaderItem(config));
        List<String> sounds = new ArrayList<>(config.getRecentSoundKeys());
        sounds.sort(Comparator.naturalOrder());
        int slot = 9;
        if (sounds.isEmpty()) {
            inventory.setItem(22, buildEmptyItem());
        } else {
            for (String soundKey : sounds) {
                if (slot >= GUI_SIZE) {
                    break;
                }
                boolean dampened = config.getDampenedSoundKeys().contains(soundKey);
                inventory.setItem(slot++, buildSoundItem(soundKey, dampened));
            }
        }
        openDampeners.put(player.getUniqueId(), key);
        player.openInventory(inventory);
    }

    private ItemStack buildHeaderItem(NoiseDampenerConfig config) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        Location location = config.getLocation();
        meta.setDisplayName(ChatColor.AQUA + "Dampener Settings");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Location: " + ChatColor.WHITE
                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        lore.add(ChatColor.GRAY + "Range: " + ChatColor.WHITE + "10x10");
        lore.add(ChatColor.GRAY + "Click sounds to toggle dampening.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSoundItem(String soundKey, boolean dampened) {
        ItemStack item = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + soundKey);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Dampened: " + (dampened ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        lore.add(ChatColor.DARK_GRAY + "Click to toggle.");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(SOUND_KEY, PersistentDataType.STRING, soundKey);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildEmptyItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "No sounds detected yet.");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Trigger nearby sounds to populate this list.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void trackRecentSound(NoiseDampenerConfig config, String soundKey) {
        List<String> recent = config.getRecentSoundKeys();
        if (recent.contains(soundKey)) {
            return;
        }
        recent.add(soundKey);
        if (recent.size() > RECENT_LIMIT) {
            recent.remove(0);
        }
    }

    private void toggleSound(NoiseDampenerConfig config, String soundKey) {
        if (config.getDampenedSoundKeys().contains(soundKey)) {
            config.getDampenedSoundKeys().remove(soundKey);
        } else {
            config.getDampenedSoundKeys().add(soundKey);
        }
    }

    private boolean isInRange(BlockKey key, Location location) {
        if (location.getWorld() == null || !location.getWorld().getUID().equals(key.worldId())) {
            return false;
        }
        return Math.abs(location.getBlockX() - key.x()) <= AREA_RADIUS
                && Math.abs(location.getBlockZ() - key.z()) <= AREA_RADIUS;
    }

    private String resolveSoundKey(Sound sound) {
        if (sound == null) {
            return null;
        }
        if (sound.getKey() != null) {
            return sound.getKey().getKey();
        }
        return sound.name();
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        private static BlockKey from(Location location) {
            World world = location.getWorld();
            return new BlockKey(
                    world == null ? new UUID(0L, 0L) : world.getUID(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
        }
    }
}
