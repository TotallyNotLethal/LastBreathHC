package com.lastbreath.hc.lastBreathHC.mobs;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ArrowAggroListener implements Listener {

    private static final String ARROW_AGGRO_RADIUS_CONFIG = "arrowAggro.radius";
    private static final double DEFAULT_RADIUS = 4.0;

    private final LastBreathHC plugin;

    public ArrowAggroListener(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.getShooter() instanceof Player shooter)) {
            return;
        }

        if (event.getHitEntity() != null) {
            return;
        }

        if (event.getHitBlock() != null) {
            igniteImpactTarget(arrow, event.getHitBlock(), event.getHitBlockFace());
        }

        if (event.getHitBlock() == null && event.getHitBlockFace() == null) {
            return;
        }

        double radius = plugin.getConfig().getDouble(ARROW_AGGRO_RADIUS_CONFIG, DEFAULT_RADIUS);
        if (radius <= 0.0) {
            return;
        }

        Location hitLocation = arrow.getLocation();
        double radiusSquared = radius * radius;

        for (Entity entity : arrow.getWorld().getNearbyEntities(hitLocation, radius, radius, radius,
                candidate -> candidate instanceof Monster)) {
            Monster monster = (Monster) entity;
            if (monster.getLocation().distanceSquared(hitLocation) > radiusSquared) {
                continue;
            }
            if (!monster.hasLineOfSight(shooter)) {
                continue;
            }
            monster.setTarget(shooter);
        }
    }
    private void igniteImpactTarget(AbstractArrow arrow, Block hitBlock, BlockFace hitFace) {
        if (arrow.getFireTicks() <= 0) {
            return;
        }

        Block ignitionBlock = hitFace == null ? hitBlock : hitBlock.getRelative(hitFace);
        if (!ignitionBlock.getType().isAir()) {
            return;
        }

        boolean canUseSoulFire = hitBlock.getType() == Material.SOUL_SOIL || hitBlock.getType() == Material.SOUL_SAND;
        Material fireType = canUseSoulFire ? Material.SOUL_FIRE : Material.FIRE;

        BlockIgniteEvent igniteEvent = new BlockIgniteEvent(
                ignitionBlock,
                BlockIgniteEvent.IgniteCause.ARROW,
                arrow
        );
        Bukkit.getPluginManager().callEvent(igniteEvent);
        if (igniteEvent.isCancelled()) {
            return;
        }

        ignitionBlock.setType(fireType, true);
    }

}
