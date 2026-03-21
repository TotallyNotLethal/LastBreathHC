package com.lastbreath.hc.lastBreathHC.cosmetics;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import java.util.Comparator;
import java.util.List;
import org.bukkit.entity.Player;

public final class CosmeticManager {

    private CosmeticManager() {
    }

    public static void unlockPrefix(Player player, BossPrefix prefix, String reason) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (stats.unlockedPrefixes.add(prefix)) {
            StatsManager.markDirty(player.getUniqueId());
            player.sendMessage("§6New prefix unlocked: §e" + prefix.displayName() + "§6. " + reason);
        }
    }

    public static void unlockAura(Player player, BossAura aura, String reason) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (stats.unlockedAuras.add(aura)) {
            StatsManager.markDirty(player.getUniqueId());
            player.sendMessage("§6New aura unlocked: §e" + aura.displayName() + "§6. " + reason);
        }
    }

    public static void unlockKillMessage(Player player, BossKillMessage message, String reason) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (stats.unlockedKillMessages.add(message)) {
            StatsManager.markDirty(player.getUniqueId());
            player.sendMessage("§6New kill message unlocked: §e" + message.displayName() + "§6. " + reason);
        }
    }

    public static boolean equipPrefix(Player player, BossPrefix prefix) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (prefix != null && !stats.unlockedPrefixes.contains(prefix)) {
            return false;
        }
        stats.equippedPrefix = prefix;
        StatsManager.markDirty(player.getUniqueId());
        return true;
    }

    public static boolean equipAura(Player player, BossAura aura) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (aura != null && (!stats.unlockedAuras.contains(aura) || !aura.isPassiveAura())) {
            return false;
        }
        stats.equippedAura = aura;
        StatsManager.markDirty(player.getUniqueId());
        return true;
    }

    public static boolean equipKillMessage(Player player, BossKillMessage message) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (message != null && !stats.unlockedKillMessages.contains(message)) {
            return false;
        }
        stats.equippedKillMessage = message;
        StatsManager.markDirty(player.getUniqueId());
        return true;
    }

    public static boolean isBackgroundAuraEnabled(Player player, BossAura aura) {
        if (aura == null || !aura.isBackgroundEffect()) {
            return false;
        }
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        return stats.unlockedAuras.contains(aura) && stats.enabledBackgroundAuras.contains(aura);
    }

    public static boolean setBackgroundAuraEnabled(Player player, BossAura aura, boolean enabled) {
        if (aura == null || !aura.isBackgroundEffect()) {
            return false;
        }
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (!stats.unlockedAuras.contains(aura)) {
            return false;
        }
        boolean changed = enabled
                ? stats.enabledBackgroundAuras.add(aura)
                : stats.enabledBackgroundAuras.remove(aura);
        if (changed) {
            StatsManager.markDirty(player.getUniqueId());
        }
        return true;
    }

    public static boolean toggleBackgroundAura(Player player, BossAura aura) {
        boolean enabled = !isBackgroundAuraEnabled(player, aura);
        return setBackgroundAuraEnabled(player, aura, enabled);
    }

    public static void clearBackgroundAuras(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (!stats.enabledBackgroundAuras.isEmpty()) {
            stats.enabledBackgroundAuras.clear();
            StatsManager.markDirty(player.getUniqueId());
        }
    }

    public static BossPrefix getEquippedPrefix(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        return stats.equippedPrefix;
    }

    public static BossAura getEquippedAura(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (stats.equippedAura != null && !stats.equippedAura.isPassiveAura()) {
            stats.equippedAura = null;
            StatsManager.markDirty(player.getUniqueId());
        }
        return stats.equippedAura;
    }

    public static BossKillMessage getEquippedKillMessage(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        return stats.equippedKillMessage;
    }

    public static String getPrefixTag(Player player, boolean tabTag) {
        BossPrefix prefix = getEquippedPrefix(player);
        if (prefix == null) {
            return "";
        }
        return "§7[" + (tabTag ? prefix.tabTag() : prefix.chatTag()) + "§7] ";
    }

    public static List<BossPrefix> getUnlockedPrefixes(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        return stats.unlockedPrefixes.stream()
                .sorted(Comparator.comparing(BossPrefix::displayName))
                .toList();
    }

    public static List<BossAura> getUnlockedAuras(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        return stats.unlockedAuras.stream()
                .sorted(Comparator.comparing(BossAura::displayName))
                .toList();
    }

    public static List<BossAura> getEnabledBackgroundAuras(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        return stats.enabledBackgroundAuras.stream()
                .filter(stats.unlockedAuras::contains)
                .filter(BossAura::isBackgroundEffect)
                .sorted(Comparator.comparing(BossAura::displayName))
                .toList();
    }

    public static List<BossKillMessage> getUnlockedKillMessages(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        return stats.unlockedKillMessages.stream()
                .sorted(Comparator.comparing(BossKillMessage::displayName))
                .toList();
    }

    public static String formatKillMessage(Player killer, String bossName) {
        BossKillMessage message = getEquippedKillMessage(killer);
        if (message == null) {
            return "§6⚔ " + killer.getName() + " defeated " + bossName + "!";
        }
        return "§6⚔ " + message.template()
                .replace("%player%", killer.getName())
                .replace("%boss%", bossName)
                .replace("%color%", message.color().toString())
                .replace("%gray%", "§7");
    }
}
