package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.nickname.NicknameStorage;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
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
    private static final String PERMISSION_NODE = "lastbreathhc.nick";

    public NickUsageListener(LastBreathHC plugin) {
        this.nicknameKey = new NamespacedKey(plugin, "nickname");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer container = player.getPersistentDataContainer();
        String nickname = container.get(nicknameKey, PersistentDataType.STRING);
        boolean hasPermission = player.hasPermission(PERMISSION_NODE);
        if (!hasPermission) {
            if (nickname != null && !nickname.isBlank()) {
                NicknameStorage.save(player.getUniqueId(), nickname);
                clearNickname(player, container);
            }
            return;
        }

        if (nickname == null || nickname.isBlank()) {
            String savedNickname = NicknameStorage.load(player.getUniqueId());
            if (savedNickname != null && !savedNickname.isBlank()) {
                container.set(nicknameKey, PersistentDataType.STRING, savedNickname);
                applyNickname(player, savedNickname);
                updateNicknameStats(player, savedNickname);
            }
            return;
        }

        applyNickname(player, nickname);
    }

    private void clearNickname(Player player, PersistentDataContainer container) {
        container.remove(nicknameKey);
        TitleManager.refreshPlayerDisplayName(player);
        updateNicknameStats(player, null);
    }

    private void applyNickname(Player player, String nickname) {
        TitleManager.refreshPlayerDisplayName(player);
    }

    private void updateNicknameStats(Player player, String nickname) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        stats.nickname = nickname;
        StatsManager.save(player.getUniqueId());
    }
}
