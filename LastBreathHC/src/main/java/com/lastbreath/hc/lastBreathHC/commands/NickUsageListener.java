package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NickUsageListener implements Listener {

    private final NamespacedKey nicknameKey;

    public NickUsageListener(LastBreathHC plugin) {
        this.nicknameKey = new NamespacedKey(plugin, "nickname");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer container = player.getPersistentDataContainer();
        String nickname = container.get(nicknameKey, PersistentDataType.STRING);
        if (nickname == null || nickname.isBlank()) {
            return;
        }
        player.setDisplayName(nickname);
        player.setPlayerListName(TitleManager.getTitleTabTag(player) + nickname);
    }
}
