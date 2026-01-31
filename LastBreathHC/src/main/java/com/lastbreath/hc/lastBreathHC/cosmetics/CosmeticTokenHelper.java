package com.lastbreath.hc.lastBreathHC.cosmetics;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class CosmeticTokenHelper {

    public static final NamespacedKey TOKEN_TYPE_KEY = new NamespacedKey(LastBreathHC.getInstance(), "cosmetic-token-type");
    public static final NamespacedKey TOKEN_ID_KEY = new NamespacedKey(LastBreathHC.getInstance(), "cosmetic-token-id");

    private CosmeticTokenHelper() {
    }

    public static ItemStack createPrefixToken(BossPrefix prefix) {
        ItemStack item = new ItemStack(prefix.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(prefix.color() + "Prefix Unlock: " + ChatColor.WHITE + prefix.displayName());
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click to unlock this chat/tab prefix."
        ));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(TOKEN_TYPE_KEY, PersistentDataType.STRING, CosmeticTokenType.PREFIX.name());
        container.set(TOKEN_ID_KEY, PersistentDataType.STRING, prefix.name());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createAuraToken(BossAura aura) {
        ItemStack item = new ItemStack(aura.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(aura.color() + "Aura Unlock: " + ChatColor.WHITE + aura.displayName());
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click to unlock this particle aura."
        ));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(TOKEN_TYPE_KEY, PersistentDataType.STRING, CosmeticTokenType.AURA.name());
        container.set(TOKEN_ID_KEY, PersistentDataType.STRING, aura.name());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createKillMessageToken(BossKillMessage message) {
        ItemStack item = new ItemStack(message.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(message.color() + "Kill Message Unlock: " + ChatColor.WHITE + message.displayName());
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click to unlock this kill message effect."
        ));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(TOKEN_TYPE_KEY, PersistentDataType.STRING, CosmeticTokenType.KILL_MESSAGE.name());
        container.set(TOKEN_ID_KEY, PersistentDataType.STRING, message.name());
        item.setItemMeta(meta);
        return item;
    }

    public static CosmeticTokenType getTokenType(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        String raw = container.get(TOKEN_TYPE_KEY, PersistentDataType.STRING);
        return CosmeticTokenType.fromInput(raw);
    }

    public static String getTokenId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(TOKEN_ID_KEY, PersistentDataType.STRING);
    }
}
