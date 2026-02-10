package com.lastbreath.hc.lastBreathHC.fakeplayer.platform;

import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Version-agnostic interface for fake-player visuals.
 *
 * <p>Important: these methods only control visual simulation (for example, tab-list rows).
 * They do not create a network-connected player, do not open a socket, and do not participate in
 * login/authentication. Real players are still only those connected through the normal Minecraft
 * client-server protocol.</p>
 */
public interface FakePlayerPlatformAdapter {

    /**
     * Spawns a visual tab-list entry for the provided fake-player record.
     */
    void spawnFakeTabEntry(FakePlayerRecord record);

    /**
     * Removes a visual tab-list entry for the fake player UUID.
     */
    void despawnFakeTabEntry(UUID uuid);

    /**
     * Updates visual display state (for example listed/unlisted, name, profile presentation).
     */
    void updateDisplayState(FakePlayerRecord record);

    default Optional<Player> getBukkitPlayer(FakePlayerRecord record) {
        return Optional.empty();
    }
}
