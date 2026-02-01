package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.cosmetics.BossAura;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BossTitleLandingListener implements Listener {

    private static final int LANDING_PARTICLE_COUNT = 18;
    private static final double LANDING_OFFSET_XZ = 0.25;
    private static final double LANDING_OFFSET_Y = 0.1;
    private static final double LANDING_EXTRA = 0.02;

    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        wasOnGround.put(event.getPlayer().getUniqueId(), event.getPlayer().isOnGround());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        wasOnGround.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        boolean onGround = player.isOnGround();
        Boolean previous = wasOnGround.put(player.getUniqueId(), onGround);
        if (previous == null || previous || !onGround) {
            return;
        }
        Title title = TitleManager.getEquippedTitle(player);
        BossAura aura = TitleManager.getBossTitleAura(title);
        if (aura == null) {
            return;
        }
        Location location = player.getLocation().clone().add(0, LANDING_OFFSET_Y, 0);
        Particle particle = aura.particle();
        player.getWorld().spawnParticle(
                particle,
                location,
                LANDING_PARTICLE_COUNT,
                LANDING_OFFSET_XZ,
                0.0,
                LANDING_OFFSET_XZ,
                LANDING_EXTRA
        );
    }
}
