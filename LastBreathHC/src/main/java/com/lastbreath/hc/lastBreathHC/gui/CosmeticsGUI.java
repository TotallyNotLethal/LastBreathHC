package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossAura;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossKillMessage;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossPrefix;
import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CosmeticsGUI implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final String TITLE = "Boss Cosmetics";
    private static final String TYPE_PREFIX = "prefix";
    private static final String TYPE_AURA = "aura";
    private static final String TYPE_KILL = "kill";
    private static final String TYPE_CLEAR = "clear";

    private final NamespacedKey typeKey;
    private final NamespacedKey idKey;

    public CosmeticsGUI() {
        this.typeKey = new NamespacedKey(LastBreathHC.getInstance(), "cosmetic-type");
        this.idKey = new NamespacedKey(LastBreathHC.getInstance(), "cosmetic-id");
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, TITLE);
        inventory.setItem(4, buildInfoItem());
        inventory.setItem(2, buildClearItem("Clear Prefix", TYPE_CLEAR, TYPE_PREFIX));
        inventory.setItem(4 + 2, buildClearItem("Clear Aura", TYPE_CLEAR, TYPE_AURA));
        inventory.setItem(6 + 2, buildClearItem("Clear Kill Message", TYPE_CLEAR, TYPE_KILL));

        int prefixSlot = 9;
        for (BossPrefix prefix : BossPrefix.values()) {
            inventory.setItem(prefixSlot++, buildPrefixItem(player, prefix));
        }

        int auraSlot = 18;
        for (BossAura aura : BossAura.values()) {
            inventory.setItem(auraSlot++, buildAuraItem(player, aura));
        }

        int killSlot = 27;
        for (BossKillMessage message : BossKillMessage.values()) {
            inventory.setItem(killSlot++, buildKillMessageItem(player, message));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getItemMeta() == null) {
            return;
        }
        String type = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        String id = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (type == null) {
            return;
        }
        if (TYPE_CLEAR.equals(type)) {
            handleClear(player, id);
            open(player);
            return;
        }
        if (TYPE_PREFIX.equals(type)) {
            BossPrefix prefix = BossPrefix.fromInput(id);
            if (prefix == null) {
                return;
            }
            if (!CosmeticManager.equipPrefix(player, prefix)) {
                player.sendMessage(ChatColor.RED + "You have not unlocked that prefix yet.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Equipped prefix: " + prefix.displayName());
            }
        } else if (TYPE_AURA.equals(type)) {
            BossAura aura = BossAura.fromInput(id);
            if (aura == null) {
                return;
            }
            if (!CosmeticManager.equipAura(player, aura)) {
                player.sendMessage(ChatColor.RED + "You have not unlocked that aura yet.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Equipped aura: " + aura.displayName());
            }
        } else if (TYPE_KILL.equals(type)) {
            BossKillMessage message = BossKillMessage.fromInput(id);
            if (message == null) {
                return;
            }
            if (!CosmeticManager.equipKillMessage(player, message)) {
                player.sendMessage(ChatColor.RED + "You have not unlocked that kill message yet.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Equipped kill message: " + message.displayName());
            }
        }
        open(player);
    }

    private ItemStack buildInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Boss Cosmetics");
        meta.setLore(List.of(
                ChatColor.GRAY + "Equip boss-earned cosmetics here.",
                ChatColor.DARK_GRAY + "Unlocks drop from world bosses."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildClearItem(String label, String type, String id) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + label);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPrefixItem(Player player, BossPrefix prefix) {
        ItemStack item = new ItemStack(prefix.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(prefix.color() + prefix.displayName());
        List<String> lore = new ArrayList<>();
        boolean unlocked = CosmeticManager.getUnlockedPrefixes(player).contains(prefix);
        if (CosmeticManager.getEquippedPrefix(player) == prefix) {
            lore.add(ChatColor.GREEN + "Equipped");
        } else if (unlocked) {
            lore.add(ChatColor.YELLOW + "Click to equip.");
        } else {
            lore.add(ChatColor.DARK_RED + "Locked - defeat its boss.");
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, TYPE_PREFIX);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, prefix.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildAuraItem(Player player, BossAura aura) {
        ItemStack item = new ItemStack(aura.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(aura.color() + aura.displayName());
        List<String> lore = new ArrayList<>();
        boolean unlocked = CosmeticManager.getUnlockedAuras(player).contains(aura);
        if (CosmeticManager.getEquippedAura(player) == aura) {
            lore.add(ChatColor.GREEN + "Equipped");
        } else if (unlocked) {
            lore.add(ChatColor.YELLOW + "Click to equip.");
        } else {
            lore.add(ChatColor.DARK_RED + "Locked - defeat its boss.");
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, TYPE_AURA);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, aura.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildKillMessageItem(Player player, BossKillMessage message) {
        ItemStack item = new ItemStack(message.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(message.color() + message.displayName());
        List<String> lore = new ArrayList<>();
        boolean unlocked = CosmeticManager.getUnlockedKillMessages(player).contains(message);
        if (CosmeticManager.getEquippedKillMessage(player) == message) {
            lore.add(ChatColor.GREEN + "Equipped");
        } else if (unlocked) {
            lore.add(ChatColor.YELLOW + "Click to equip.");
        } else {
            lore.add(ChatColor.DARK_RED + "Locked - defeat its boss.");
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, TYPE_KILL);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, message.name());
        item.setItemMeta(meta);
        return item;
    }

    private void handleClear(Player player, String id) {
        if (TYPE_PREFIX.equals(id)) {
            CosmeticManager.equipPrefix(player, null);
            player.sendMessage(ChatColor.YELLOW + "Prefix cleared.");
        } else if (TYPE_AURA.equals(id)) {
            CosmeticManager.equipAura(player, null);
            player.sendMessage(ChatColor.YELLOW + "Aura cleared.");
        } else if (TYPE_KILL.equals(id)) {
            CosmeticManager.equipKillMessage(player, null);
            player.sendMessage(ChatColor.YELLOW + "Kill message cleared.");
        }
    }
}
