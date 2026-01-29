package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

public class TitleListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerStats stats = StatsManager.load(event.getPlayer().getUniqueId());
        NamespacedKey nicknameKey = new NamespacedKey(LastBreathHC.getInstance(), "nickname");
        String nickname = event.getPlayer().getPersistentDataContainer().get(nicknameKey, PersistentDataType.STRING);
        if (nickname != null && !nickname.isBlank() && !nickname.equals(stats.nickname)) {
            stats.nickname = nickname;
            StatsManager.save(event.getPlayer().getUniqueId());
        }
        TitleManager.initialize(stats);
        TitleManager.checkTimeBasedTitles(event.getPlayer());
        TitleManager.checkProgressTitles(event.getPlayer());
        TitleManager.applyEquippedTitleEffects(event.getPlayer(), stats.equippedTitle);
        TitleManager.refreshPlayerTabTitle(event.getPlayer());
        event.getPlayer().sendMessage("§7Current title: §b" + stats.equippedTitle.displayName());
    }
}
