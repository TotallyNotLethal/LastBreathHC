package com.lastbreath.hc.lastBreathHC.stats;

import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.EnumSet;
import java.util.Set;

public class StatsListener implements Listener {
    private static final Set<Material> CROP_BLOCKS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.SWEET_BERRY_BUSH,
            Material.PUMPKIN,
            Material.MELON
    );
    private static final Set<Material> RARE_ORES = EnumSet.of(
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS
    );

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        StatsManager.save(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        PlayerStats stats = StatsManager.get(killer.getUniqueId());
        stats.mobsKilled++;
        StatsManager.markDirty(killer.getUniqueId());
        TitleManager.checkProgressTitles(killer);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        Material type = event.getBlock().getType();
        boolean updated = false;
        if (isCropBlock(type) && isMatureCrop(event.getBlock())) {
            stats.cropsHarvested++;
            updated = true;
        }
        if (isMiningBlock(type)) {
            stats.blocksMined++;
            updated = true;
        }
        if (isRareOre(type)) {
            stats.rareOresMined++;
            updated = true;
        }
        if (updated) {
            StatsManager.markDirty(player.getUniqueId());
            TitleManager.checkProgressTitles(player);
        }
    }

    private boolean isCropBlock(Material type) {
        return CROP_BLOCKS.contains(type);
    }

    private boolean isMatureCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return true;
    }

    private boolean isMiningBlock(Material type) {
        return Tag.BASE_STONE_OVERWORLD.isTagged(type)
                || Tag.BASE_STONE_NETHER.isTagged(type)
                || Tag.COAL_ORES.isTagged(type)
                || Tag.IRON_ORES.isTagged(type)
                || Tag.COPPER_ORES.isTagged(type)
                || Tag.GOLD_ORES.isTagged(type)
                || Tag.REDSTONE_ORES.isTagged(type)
                || Tag.LAPIS_ORES.isTagged(type)
                || Tag.EMERALD_ORES.isTagged(type)
                || Tag.DIAMOND_ORES.isTagged(type)
                || type == Material.END_STONE;
    }

    private boolean isRareOre(Material type) {
        return RARE_ORES.contains(type);
    }
}
