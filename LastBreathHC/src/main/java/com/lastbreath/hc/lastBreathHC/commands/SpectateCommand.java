package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.spectate.AdminSpectateHotbarListener;
import com.lastbreath.hc.lastBreathHC.spectate.SpectateSession;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SpectateCommand implements BasicCommand, Listener {

    private static final List<String> SUB_MODES = List.of("view", "enderchest", "stats", "inv");
    private static final String LEAVE_COMMAND = "leave";
    private static final String STOP_COMMAND = "stop";
    private final Map<UUID, SpectateSession> sessions = new ConcurrentHashMap<>();
    private final LastBreathHC plugin;
    private AdminSpectateHotbarListener adminHotbarListener;

    public SpectateCommand(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    public void setAdminHotbarListener(AdminSpectateHotbarListener adminHotbarListener) {
        this.adminHotbarListener = adminHotbarListener;
    }

    public SpectateSession getSession(UUID viewerId) {
        return sessions.get(viewerId);
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toCollection(ArrayList::new));
            suggestions.add(LEAVE_COMMAND);
            return suggestions;
        }
        if (args.length == 2 || args.length == 3) {
            return SUB_MODES;
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(ChatColor.RED + "Players only.");
            return;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /spectate <player> <mode?> or /spectate leave");
            return;
        }

        String firstArg = args[0].toLowerCase();
        if (firstArg.equals(LEAVE_COMMAND) || firstArg.equals(STOP_COMMAND)) {
            endSpectate(player, true);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "That player is not online.");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot spectate yourself.");
            return;
        }

        SpectateSession existing = sessions.get(player.getUniqueId());
        if (existing != null) {
            player.sendMessage(ChatColor.YELLOW + "You are already spectating. Use /spectate leave to stop.");
            return;
        }

        startSpectate(player, target, args.length >= 2 ? args[1].toLowerCase() : "view");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (sessions.containsKey(player.getUniqueId())) {
            endSpectate(player, false);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        if (isAdmin(joiningPlayer)) {
            return;
        }
        sessions.entrySet().stream()
                .filter(entry -> entry.getValue().isAdminSpectate())
                .forEach(entry -> {
                    Player viewer = Bukkit.getPlayer(entry.getKey());
                    if (viewer != null && viewer.isOnline()) {
                        joiningPlayer.hidePlayer(plugin, viewer);
                    }
                });
    }

    private void startSpectate(Player viewer, Player target, String mode) {
        boolean adminSpectate = shouldHideAdminSpectator(viewer, mode);
        SpectateSession session = captureSession(viewer, adminSpectate);
        sessions.put(viewer.getUniqueId(), session);

        viewer.getInventory().clear();
        viewer.getInventory().setArmorContents(new ItemStack[viewer.getInventory().getArmorContents().length]);
        viewer.getInventory().setItemInOffHand(null);
        if (adminSpectate && adminHotbarListener != null) {
            adminHotbarListener.applyHotbar(viewer);
        } else {
            viewer.getInventory().setItem(0, new ItemStack(Material.COMPASS));
            viewer.getInventory().setItem(8, new ItemStack(Material.BARRIER));
        }
        viewer.updateInventory();

        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.teleport(target.getLocation());
        viewer.setSpectatorTarget(target);
        viewer.sendMessage(ChatColor.AQUA + "Now spectating " + target.getName() + " (" + mode + ").");

        if (adminSpectate) {
            hideAdminSpectatorFromNonAdmins(viewer);
        }
    }

    private SpectateSession captureSession(Player player, boolean adminSpectate) {
        ItemStack[] inventoryContents = Arrays.copyOf(player.getInventory().getContents(), player.getInventory().getContents().length);
        ItemStack[] armorContents = Arrays.copyOf(player.getInventory().getArmorContents(), player.getInventory().getArmorContents().length);
        ItemStack offhand = player.getInventory().getItemInOffHand() == null ? null : player.getInventory().getItemInOffHand().clone();
        List<PotionEffect> potionEffects = new ArrayList<>(player.getActivePotionEffects());

        return new SpectateSession(
                player.getLocation().clone(),
                player.getGameMode(),
                inventoryContents,
                armorContents,
                offhand,
                player.getExp(),
                player.getLevel(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                potionEffects,
                adminSpectate
        );
    }

    private void endSpectate(Player player, boolean notify) {
        SpectateSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            if (notify) {
                player.sendMessage(ChatColor.RED + "You are not spectating anyone.");
            }
            return;
        }

        if (session.isAdminSpectate()) {
            showAdminSpectatorToAll(player);
        }
        restoreSession(player, session);
        if (notify) {
            player.sendMessage(ChatColor.GREEN + "Stopped spectating.");
        }
    }

    private void restoreSession(Player player, SpectateSession session) {
        Location returnLocation = session.getReturnLocation();
        player.setSpectatorTarget(null);
        player.teleport(returnLocation);
        player.setGameMode(session.getReturnMode());

        player.getInventory().setContents(session.getInventoryContents());
        player.getInventory().setArmorContents(session.getArmorContents());
        player.getInventory().setItemInOffHand(session.getOffhand());
        player.setExp(session.getExp());
        player.setLevel(session.getLevel());
        player.setHealth(Math.min(session.getHealth(), player.getMaxHealth()));
        player.setFoodLevel(session.getFoodLevel());
        player.setSaturation(session.getSaturation());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : session.getPotionEffects()) {
            player.addPotionEffect(effect);
        }
        player.updateInventory();
    }

    private boolean shouldHideAdminSpectator(Player viewer, String mode) {
        return isAdmin(viewer) && "view".equalsIgnoreCase(mode);
    }

    private boolean isAdmin(Player player) {
        return player.isOp();
    }

    private void hideAdminSpectatorFromNonAdmins(Player adminPlayer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(adminPlayer.getUniqueId()) || isAdmin(other)) {
                continue;
            }
            other.hidePlayer(plugin, adminPlayer);
        }
    }

    private void showAdminSpectatorToAll(Player adminPlayer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(adminPlayer.getUniqueId())) {
                continue;
            }
            other.showPlayer(plugin, adminPlayer);
        }
    }
}
