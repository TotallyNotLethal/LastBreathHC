package com.lastbreath.hc.lastBreathHC.death;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.bounty.BountyManager;
import com.lastbreath.hc.lastBreathHC.bounty.BountyRecord;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerDeathReactionHandler;
import com.lastbreath.hc.lastBreathHC.integrations.discord.DiscordWebhookService;
import com.lastbreath.hc.lastBreathHC.revive.ReviveStateManager;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.team.TeamChatService;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import com.lastbreath.hc.lastBreathHC.token.ReviveTokenHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

public class DeathListener implements Listener {

    private static final String REVIVE_INTERCEPT_METADATA = "lastbreathhc.reviveIntercept";
    private final DeathMarkerManager deathMarkerManager;
    private final TeamChatService teamChatService;
    private final DiscordWebhookService discordWebhookService;
    private final FakePlayerDeathReactionHandler fakePlayerDeathReactionHandler;
    private final PlayerLastMessageTracker playerLastMessageTracker;
    private final DeathAuditLogger deathAuditLogger;

    public DeathListener(DeathMarkerManager deathMarkerManager,
                         TeamChatService teamChatService,
                         DiscordWebhookService discordWebhookService,
                         FakePlayerDeathReactionHandler fakePlayerDeathReactionHandler,
                         PlayerLastMessageTracker playerLastMessageTracker,
                         DeathAuditLogger deathAuditLogger) {
        this.deathMarkerManager = deathMarkerManager;
        this.teamChatService = teamChatService;
        this.discordWebhookService = discordWebhookService;
        this.fakePlayerDeathReactionHandler = fakePlayerDeathReactionHandler;
        this.playerLastMessageTracker = playerLastMessageTracker;
        this.deathAuditLogger = deathAuditLogger;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreDeathDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!ReviveTokenHelper.hasToken(player)) {
            return;
        }

        double remainingHealth = player.getHealth() - event.getFinalDamage();
        if (remainingHealth > 0.0) {
            return;
        }
        if (isHoldingTotem(player)) {
            return;
        }

        if (!ReviveTokenHelper.consumeToken(player)) {
            return;
        }

        event.setCancelled(true);
        player.setMetadata(
                REVIVE_INTERCEPT_METADATA,
                new FixedMetadataValue(LastBreathHC.getInstance(), true)
        );

        player.setHealth(1.0);
        Location destination = player.getBedSpawnLocation() != null
                ? player.getBedSpawnLocation()
                : player.getWorld().getSpawnLocation();
        player.teleport(destination);

        Bukkit.broadcastMessage(
                "§6⚡ " + TitleManager.getTitleTag(player) + player.getName()
                        + " defied death! §7(" + formatDamageCause(player.getLastDamageCause()) + ")"
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata(REVIVE_INTERCEPT_METADATA, LastBreathHC.getInstance());
            }
        }.runTaskLater(LastBreathHC.getInstance(), 40L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasMetadata(REVIVE_INTERCEPT_METADATA)) {
            player.removeMetadata(REVIVE_INTERCEPT_METADATA, LastBreathHC.getInstance());
            return;
        }
        fakePlayerDeathReactionHandler.handlePlayerDeath(player);

        String deathMessage = event.getDeathMessage();
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        stats.deaths++;
        TitleManager.unlockTitle(player, Title.THE_FALLEN, Title.THE_FALLEN.requirementDescription());
        if (stats.deaths >= 3) {
            TitleManager.unlockTitle(player, Title.DEATH_DEFIER, Title.DEATH_DEFIER.requirementDescription());
        }
        TitleManager.checkTimeBasedTitles(player);
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            handleBountyClaim(player, killer);
            BountyManager.createBounty(killer.getUniqueId());
        }

        boolean hasToken = ReviveTokenHelper.hasToken(player);
        String deathReason = formatDeathReason(player, deathMessage);
        String killerLabel = formatKillerLabel(player);

        // Stop vanilla behavior
        event.setDeathMessage(null);
        playGlobalDeathSound();
        if (hasToken) {
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
        } else {
            event.setKeepInventory(false);
            event.setKeepLevel(false);
        }

        Location deathLocation = player.getLocation().clone();
        sendTeamDeathLocation(player, deathLocation);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (hasToken) {
                    if (!ReviveTokenHelper.consumeToken(player)) {
                        banPlayer(
                                player,
                                "Revival token missing at time of death.",
                                deathReason,
                                stats,
                                killerLabel,
                                deathLocation
                        );
                    } else {
                        triggerReviveFlow(player, deathMessage);
                    }
                } else {
                    banPlayer(
                            player,
                            "You died with no revival token.",
                            deathReason,
                            stats,
                            killerLabel,
                            deathLocation
                    );
                }
                deathMarkerManager.spawnMarker(player, deathLocation);
            }
        }.runTaskLater(LastBreathHC.getInstance(), 1L);
    }

    private void sendTeamDeathLocation(Player player, Location deathLocation) {
        World world = deathLocation.getWorld();
        String worldName = world == null ? "Unknown" : world.getName();
        String message = player.getName()
                + " died at "
                + deathLocation.getBlockX()
                + ", "
                + deathLocation.getBlockY()
                + ", "
                + deathLocation.getBlockZ()
                + " in "
                + worldName;
        teamChatService.sendTeamSystemMessage(player, message);
    }

    private void triggerReviveFlow(Player player, String deathMessage) {
        player.setHealth(1.0);
        player.setGameMode(GameMode.SPECTATOR);

        // Global boom
        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_DEATH,
                1.5f,
                0.6f
        );

        String deathReason = formatDeathReason(player, deathMessage);
        broadcastHoverableDeathMessage(player, deathReason);
        applyRevive(player);
    }

    private void applyRevive(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        stats.revives++;
        TitleManager.unlockTitle(player, Title.REVIVED, Title.REVIVED.requirementDescription());
        if (stats.revives >= 3) {
            TitleManager.unlockTitle(player, Title.SOUL_RECLAIMER, Title.SOUL_RECLAIMER.requirementDescription());
        }

        player.setGameMode(GameMode.SURVIVAL);
        ReviveStateManager.markRevivePending(player.getUniqueId());
        player.teleport(
                player.getBedSpawnLocation() != null
                        ? player.getBedSpawnLocation()
                        : player.getWorld().getSpawnLocation()
        );

        Bukkit.broadcastMessage(
                "§6⚡ " + TitleManager.getTitleTag(player) + player.getName() + " defied death!"
        );
    }

    private void banPlayer(
            Player player,
            String reason,
            String deathReason,
            PlayerStats stats,
            String killerLabel,
            Location deathLocation
    ) {
        LastBreathHC.getInstance().getLogger().info(
                "Permanent death ban issued. player=" + player.getName()
                        + " reason=" + reason
                        + " deathReason=" + deathReason
                        + " sendingDiscordWebhook=true"
        );
        broadcastHoverableDeathMessage(player, deathReason);

        Bukkit.getBanList(BanList.Type.NAME)
                .addBan(player.getName(), reason, null, null);

        player.kickPlayer("You have died.\nNo revival token was used.");

        discordWebhookService.sendDeathWebhook(
                player,
                stats,
                deathReason,
                killerLabel,
                deathLocation,
                false,
                playerLastMessageTracker.getLastMessage(player.getUniqueId())
        );
    }

    private void broadcastHoverableDeathMessage(Player player, String deathReason) {
        String lastMessage = playerLastMessageTracker.getLastMessage(player.getUniqueId());
        deathAuditLogger.recordDeath(player, deathReason, lastMessage);
        Component hoverText = Component.text(buildHoverDetailText(player, lastMessage), NamedTextColor.LIGHT_PURPLE);
        Component deathText = Component.text(player.getName() + " " + deathReason, NamedTextColor.RED)
                .hoverEvent(hoverText);
        Bukkit.broadcast(deathText);
    }

    private String buildHoverDetailText(Player player, String lastMessage) {
        String joinDate = formatDate(player.getFirstPlayed());
        String deathDate = formatDate(System.currentTimeMillis());
        String playtime = formatDuration(player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L);
        String finalMessage = (lastMessage == null || lastMessage.isBlank()) ? "No last message recorded." : lastMessage;

        return joinDate + " -> " + deathDate
                + "\nPlaytime: " + playtime
                + "\n" + finalMessage;
    }

    private String formatDate(long epochMillis) {
        if (epochMillis <= 0L) {
            return "Unknown";
        }
        LocalDate date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        return date.toString();
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(remainingSeconds).append("s");
        return builder.toString().trim();
    }

    public void banPlayerForReviveDecision(Player player, String reason) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        String killerLabel = formatKillerLabel(player);
        Location deathLocation = player.getLocation().clone();
        banPlayer(player, reason, reason, stats, killerLabel, deathLocation);
    }

    private void handleBountyClaim(Player victim, Player killer) {
        BountyRecord record = BountyManager.claimBounty(
                victim.getUniqueId(),
                "Killed by " + killer.getName()
        );
        if (record == null) {
            return;
        }

        ItemStack reward = BountyManager.getRewardItemStack(record);
        if (reward == null) {
            return;
        }

        Map<Integer, ItemStack> leftover = killer.getInventory().addItem(reward);
        for (ItemStack item : leftover.values()) {
            killer.getWorld().dropItemNaturally(killer.getLocation(), item);
        }

        String targetName = record.targetName == null ? victim.getName() : record.targetName;
        String rewardLabel = reward.getAmount() + " " + formatMaterialName(reward.getType());
        Bukkit.broadcastMessage(
                "§6⚔ " + killer.getName() + " claimed the bounty on " + targetName + " for " + rewardLabel + "."
        );
        LastBreathHC.getInstance().getLogger().info(
                killer.getName() + " claimed the bounty on " + targetName + " for " + rewardLabel + "."
        );
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase(Locale.US).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(' ');
        }
        return builder.toString().trim();
    }

    private void playGlobalDeathSound() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }
    }

    private boolean isHoldingTotem(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING) {
            return true;
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING;
    }

    private static String formatDamageCause(EntityDamageEvent event) {
        if (event == null) {
            return "Unknown cause";
        }

        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            if (byEntityEvent.getDamager() instanceof Player damager) {
                return "slain by " + damager.getName();
            }
            String name = byEntityEvent.getDamager().getType().name().toLowerCase(Locale.US)
                    .replace('_', ' ');
            return "slain by " + name;
        }

        String cause = event.getCause().name().toLowerCase(Locale.US).replace('_', ' ');
        return cause.isBlank() ? "Unknown cause" : cause;
    }

    private static String formatDeathReason(Player player, String deathMessage) {
        if (deathMessage == null || deathMessage.isBlank()) {
            return "Unknown cause";
        }
        String prefix = player.getName() + " ";
        if (deathMessage.startsWith(prefix)) {
            return deathMessage.substring(prefix.length()).trim();
        }
        return deathMessage.trim();
    }

    private static String formatKillerLabel(Player player) {
        Player killer = player.getKiller();
        if (killer != null) {
            return killer.getName();
        }
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent byEntityEvent) {
            String name = byEntityEvent.getDamager().getType().name().toLowerCase(Locale.US).replace('_', ' ');
            return name;
        }
        return formatDamageCause(player.getLastDamageCause());
    }
}
