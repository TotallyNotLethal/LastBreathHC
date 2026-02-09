package com.lastbreath.hc.lastBreathHC.nickname;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NicknamePermissionMonitor implements Runnable {

    private static final String PERMISSION_NODE = "lastbreathhc.nick";
    private final NamespacedKey nicknameKey;

    public NicknamePermissionMonitor(LastBreathHC plugin) {
        this.nicknameKey = new NamespacedKey(plugin, "nickname");
    }

    @Override
    public void run() {
        for (Player player : LastBreathHC.getInstance().getServer().getOnlinePlayers()) {
            syncNickname(player);
        }
    }

    private void syncNickname(Player player) {
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
        }
    }

    private void clearNickname(Player player, PersistentDataContainer container) {
        container.remove(nicknameKey);
        player.setDisplayName(player.getName());
        TitleManager.refreshPlayerTabTitle(player);
        updateNicknameStats(player, null);
    }

    private void applyNickname(Player player, String nickname) {
        player.setDisplayName(nickname);
        player.setPlayerListName(TitleManager.getTitleTabTag(player) + nickname);
    }

    private void updateNicknameStats(Player player, String nickname) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        stats.nickname = nickname;
        StatsManager.save(player.getUniqueId());
    }
}
