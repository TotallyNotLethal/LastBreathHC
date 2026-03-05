package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

public class RtpCommand implements BasicCommand {

    private static final int MAX_ATTEMPTS = 40;
    private static final int RTP_RANGE = 5000;
    private final NamespacedKey rtpUsedKey;
    private final Random random = new Random();

    public RtpCommand(LastBreathHC plugin) {
        this.rtpUsedKey = new NamespacedKey(plugin, "rtp_used");
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }

        if (!player.isOp()) {
            PersistentDataContainer container = player.getPersistentDataContainer();
            if (container.getOrDefault(rtpUsedKey, PersistentDataType.BYTE, (byte) 0) == 1) {
                player.sendMessage("§cYou have already used /rtp once this life. You can use it again after you die.");
                return;
            }

            player.sendMessage("§eWarning: /rtp is a one-time command per life. You will not be able to use it again.");
            player.sendMessage("§eWarning: The further from spawn you travel, the harder mobs will become.");
        }

        Location target = findSafeLocation(player.getWorld());
        if (target == null) {
            player.sendMessage("§cUnable to find a safe RTP location. Please try again.");
            return;
        }

        player.teleport(target);
        player.sendMessage("§aTeleported to a safe random location.");

        if (!player.isOp()) {
            player.getPersistentDataContainer().set(rtpUsedKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private Location findSafeLocation(World world) {
        int minHeight = world.getMinHeight();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int blockX = (int) Math.floor((random.nextDouble() * 2.0 - 1.0) * RTP_RANGE);
            int blockZ = (int) Math.floor((random.nextDouble() * 2.0 - 1.0) * RTP_RANGE);
            int chunkX = blockX >> 4;
            int chunkZ = blockZ >> 4;

            if (!world.isChunkGenerated(chunkX, chunkZ)) {
                continue;
            }

            if (!world.isChunkLoaded(chunkX, chunkZ) && !world.loadChunk(chunkX, chunkZ, false)) {
                continue;
            }

            int y = world.getHighestBlockYAt(blockX, blockZ);
            if (y <= minHeight + 1) {
                continue;
            }

            Block ground = world.getBlockAt(blockX, y, blockZ);
            if (!ground.getType().isSolid() || ground.getType() == Material.LAVA) {
                continue;
            }

            Block feet = ground.getRelative(0, 1, 0);
            Block head = ground.getRelative(0, 2, 0);
            if (!feet.isPassable() || !head.isPassable()) {
                continue;
            }
            if (feet.getType() == Material.LAVA || head.getType() == Material.LAVA) {
                continue;
            }

            return new Location(world, ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5);
        }

        return null;
    }
}
