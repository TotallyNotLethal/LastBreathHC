package com.lastbreath.hc.lastBreathHC.gui;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.death.DeathMarkerManager;
import com.lastbreath.hc.lastBreathHC.team.JoinOutcome;
import com.lastbreath.hc.lastBreathHC.team.LeaveOutcome;
import com.lastbreath.hc.lastBreathHC.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class TeamManagementGUI implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int LIST_LIMIT = 45;
    private static final String TITLE_BASE = "Team Manager";

    private final TeamManager teamManager;
    private final DeathMarkerManager deathMarkerManager;
    private final NamespacedKey teamKey;
    private final NamespacedKey memberKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey deathTrailKey;

    public TeamManagementGUI(LastBreathHC plugin, TeamManager teamManager, DeathMarkerManager deathMarkerManager) {
        this.teamManager = teamManager;
        this.deathMarkerManager = deathMarkerManager;
        this.teamKey = new NamespacedKey(plugin, "team-name");
        this.memberKey = new NamespacedKey(plugin, "team-member");
        this.actionKey = new NamespacedKey(plugin, "team-action");
        this.deathTrailKey = new NamespacedKey(plugin, "team-death-trail");
    }

    public void open(Player player) {
        Optional<Team> team = teamManager.getTeam(player);
        if (team.isEmpty()) {
            openTeamBrowser(player);
        } else {
            openTeamRoster(player, team.get());
        }
    }

    private void openTeamBrowser(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, TITLE_BASE + " - Teams");

        List<Team> teams = new ArrayList<>(teamManager.getTeams());
        teams.sort(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER));

        int slot = 0;
        for (Team team : teams) {
            if (slot >= LIST_LIMIT) {
                break;
            }
            inventory.setItem(slot++, buildTeamItem(team));
        }

        if (teams.isEmpty()) {
            inventory.setItem(22, buildInfoItem(Material.BARRIER,
                    ChatColor.YELLOW + "No teams yet",
                    List.of(ChatColor.GRAY + "Use /team create <name> to start one.")));
        }

        inventory.setItem(49, buildInfoItem(Material.BOOK,
                ChatColor.GOLD + "Team Manager",
                List.of(ChatColor.GRAY + "Click a team to join.",
                        ChatColor.GRAY + "Use /team create <name> to start one.")));

        player.openInventory(inventory);
    }

    private void openTeamRoster(Player player, Team team) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, TITLE_BASE + " - " + team.getName());

        List<String> members = new ArrayList<>(team.getEntries());
        members.sort(String.CASE_INSENSITIVE_ORDER);

        int slot = 0;
        for (String member : members) {
            if (slot >= LIST_LIMIT) {
                break;
            }
            inventory.setItem(slot++, buildMemberItem(team, member, player));
        }

        boolean isOwner = teamManager.isOwner(player, team);
        ItemStack leaveItem = buildActionItem(Material.BARRIER,
                isOwner ? ChatColor.RED + "Disband Team" : ChatColor.RED + "Leave Team",
                isOwner ? "disband" : "leave",
                List.of(ChatColor.GRAY + "Click to " + (isOwner ? "disband" : "leave") + " your team."));
        inventory.setItem(45, leaveItem);

        inventory.setItem(49, buildInfoItem(Material.PAPER,
                ChatColor.GOLD + team.getName(),
                List.of(ChatColor.GRAY + "Members: " + members.size(),
                        ChatColor.GRAY + "Owner: " + getOwnerName(team),
                        ChatColor.GRAY + "Joining: " + (teamManager.isJoinLocked(team) ? "Locked" : "Open"))));

        List<DeadTeammateEntry> deadTeammates = resolveDeadTeammates(team, player.getUniqueId());
        int[] deadSlots = new int[]{46, 47, 48, 50, 51, 52};
        for (int i = 0; i < deadSlots.length && i < deadTeammates.size(); i++) {
            inventory.setItem(deadSlots[i], buildDeadTeammateItem(deadTeammates.get(i)));
        }

        if (deadTeammates.isEmpty()) {
            inventory.setItem(53, buildInfoItem(Material.LIME_DYE,
                    ChatColor.GREEN + "No dead teammates",
                    List.of(ChatColor.GRAY + "Everyone on your team is alive.")));
        } else {
            inventory.setItem(53, buildInfoItem(Material.COMPASS,
                    ChatColor.GOLD + "Dead teammate trail",
                    List.of(ChatColor.GRAY + "Click a dead teammate head",
                            ChatColor.GRAY + "to start a guidance trail.")));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_BASE)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String action = container.get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            handleAction(player, action);
            return;
        }

        String teamName = container.get(teamKey, PersistentDataType.STRING);
        if (teamName != null) {
            handleJoin(player, teamName);
            return;
        }

        String memberId = container.get(memberKey, PersistentDataType.STRING);
        if (memberId != null) {
            handleKick(player, memberId);
            return;
        }

        String deathTrailPayload = container.get(deathTrailKey, PersistentDataType.STRING);
        if (deathTrailPayload != null) {
            handleDeadTeammateTrail(player, deathTrailPayload);
        }
    }

    private void handleJoin(Player player, String teamName) {
        Optional<Team> team = teamManager.getTeamByName(teamName);
        if (team.isEmpty()) {
            player.sendMessage(ChatColor.RED + "That team no longer exists.");
            open(player);
            return;
        }
        JoinOutcome outcome = teamManager.joinTeam(player, team.get());
        switch (outcome) {
            case JOINED -> player.sendMessage(ChatColor.GREEN + "Joined team " + teamName + ".");
            case REQUESTED -> {
                player.sendMessage(ChatColor.YELLOW + "Join request sent to the team owner.");
                notifyOwnerOfRequest(team.get(), player);
            }
            case REQUEST_ALREADY_PENDING -> player.sendMessage(ChatColor.YELLOW + "You already have a pending join request.");
            case ALREADY_MEMBER -> player.sendMessage(ChatColor.YELLOW + "You are already on that team.");
            case FAILED -> player.sendMessage(ChatColor.RED + "Unable to join that team.");
        }
        open(player);
    }

    private void handleAction(Player player, String action) {
        Optional<Team> team = teamManager.getTeam(player);
        if (team.isEmpty()) {
            open(player);
            return;
        }
        switch (action.toLowerCase(Locale.ROOT)) {
            case "leave" -> {
                LeaveOutcome outcome = teamManager.leaveTeam(player);
                if (outcome == LeaveOutcome.NOT_IN_TEAM) {
                    player.sendMessage(ChatColor.RED + "You are not on a team.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You left the team.");
                }
                open(player);
            }
            case "disband" -> {
                if (!teamManager.isOwner(player, team.get())) {
                    player.sendMessage(ChatColor.RED + "Only the team owner can disband.");
                    return;
                }
                teamManager.disbandTeam(team.get());
                player.sendMessage(ChatColor.RED + "Team disbanded.");
                open(player);
            }
            default -> {
            }
        }
    }

    private void handleKick(Player player, String memberId) {
        Optional<Team> team = teamManager.getTeam(player);
        if (team.isEmpty()) {
            open(player);
            return;
        }
        if (!teamManager.isOwner(player, team.get())) {
            player.sendMessage(ChatColor.RED + "Only the team owner can kick members.");
            return;
        }
        OfflinePlayer target = resolveMember(memberId);
        if (target == null || target.getName() == null) {
            player.sendMessage(ChatColor.RED + "That member could not be found.");
            return;
        }
        if (teamManager.kickMember(player, team.get(), target)) {
            player.sendMessage(ChatColor.YELLOW + "Kicked " + target.getName() + " from the team.");
            Player online = Bukkit.getPlayerExact(target.getName());
            if (online != null) {
                online.sendMessage(ChatColor.RED + "You were kicked from " + team.get().getName() + ".");
            }
            open(player);
        }
    }

    private void handleDeadTeammateTrail(Player player, String payload) {
        String[] parts = payload.split("\\|", 5);
        if (parts.length != 5) {
            player.sendMessage(ChatColor.RED + "That death trail entry is no longer valid.");
            return;
        }

        String targetName = parts[0];
        World world = Bukkit.getWorld(parts[1]);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "That world is currently unavailable.");
            return;
        }

        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(parts[2]);
            y = Double.parseDouble(parts[3]);
            z = Double.parseDouble(parts[4]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "That death trail entry is corrupted.");
            return;
        }

        Location location = new Location(world, x, y, z);
        deathMarkerManager.startPersonalTrail(player, targetName, location, 120);
        player.sendMessage(ChatColor.AQUA + "Trail started to " + targetName + "'s death location.");
        player.closeInventory();
    }

    private ItemStack buildTeamItem(Team team) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + team.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Members: " + team.getEntries().size());
        lore.add(ChatColor.GRAY + "Owner: " + getOwnerName(team));
        if (teamManager.isJoinLocked(team)) {
            lore.add(ChatColor.RED + "Joining: Locked");
            lore.add(ChatColor.YELLOW + "Click to request access.");
        } else {
            lore.add(ChatColor.GREEN + "Joining: Open");
            lore.add(ChatColor.YELLOW + "Click to join.");
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(teamKey, PersistentDataType.STRING, team.getName());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildMemberItem(Team team, String member, Player viewer) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(member);
            skullMeta.setOwningPlayer(offline);
            meta = skullMeta;
        }
        meta.setDisplayName(ChatColor.AQUA + member);
        List<String> lore = new ArrayList<>();
        boolean isOwner = teamManager.getOwner(team)
                .map(ownerId -> ownerId.equals(Bukkit.getOfflinePlayer(member).getUniqueId()))
                .orElse(false);
        lore.add(isOwner ? ChatColor.GOLD + "Owner" : ChatColor.GRAY + "Member");
        if (teamManager.isOwner(viewer, team) && !isOwner) {
            lore.add(ChatColor.RED + "Click to kick.");
            meta.getPersistentDataContainer().set(memberKey, PersistentDataType.STRING, member);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildDeadTeammateItem(DeadTeammateEntry entry) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(entry.player());
            meta = skullMeta;
        }

        String playerName = entry.player().getName() == null ? "Unknown" : entry.player().getName();
        meta.setDisplayName(ChatColor.RED + playerName + " (Dead)");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Death location:");
        lore.add(ChatColor.DARK_GRAY + entry.location().getWorld().getName()
                + " " + entry.location().getBlockX()
                + ", " + entry.location().getBlockY()
                + ", " + entry.location().getBlockZ());
        lore.add(ChatColor.YELLOW + "Click to start guidance trail.");
        meta.setLore(lore);

        String payload = playerName + "|"
                + entry.location().getWorld().getName() + "|"
                + entry.location().getX() + "|"
                + entry.location().getY() + "|"
                + entry.location().getZ();
        meta.getPersistentDataContainer().set(deathTrailKey, PersistentDataType.STRING, payload);
        item.setItemMeta(meta);
        return item;
    }

    private List<DeadTeammateEntry> resolveDeadTeammates(Team team, UUID viewerId) {
        List<DeadTeammateEntry> entries = new ArrayList<>();
        for (String member : team.getEntries()) {
            OfflinePlayer teammate = Bukkit.getOfflinePlayer(member);
            if (teammate.getUniqueId().equals(viewerId) || !teammate.isBanned()) {
                continue;
            }
            Location deathLocation = safeLastDeathLocation(teammate);
            if (deathLocation == null || deathLocation.getWorld() == null) {
                continue;
            }
            entries.add(new DeadTeammateEntry(teammate, deathLocation));
        }
        entries.sort(Comparator.comparing(entry -> {
            String name = entry.player().getName();
            return name == null ? "" : name.toLowerCase(Locale.ROOT);
        }));
        return entries;
    }

    private Location safeLastDeathLocation(OfflinePlayer player) {
        try {
            return player.getLastDeathLocation();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ItemStack buildActionItem(Material material, String title, String action, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem(Material material, String title, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getOwnerName(Team team) {
        return teamManager.getOwner(team)
                .map(ownerId -> {
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
                    return owner.getName() == null ? "Unknown" : owner.getName();
                })
                .orElse("Unknown");
    }

    private OfflinePlayer resolveMember(String memberId) {
        try {
            UUID uuid = UUID.fromString(memberId);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayer(memberId);
        }
    }

    private void notifyOwnerOfRequest(Team team, Player requester) {
        teamManager.getOwner(team)
                .map(requester.getServer()::getPlayer)
                .ifPresent(owner -> owner.sendMessage(ChatColor.YELLOW + requester.getName()
                        + " requested to join your team. Use /team accept " + requester.getName() + " or /team deny " + requester.getName() + "."));
    }

    private record DeadTeammateEntry(OfflinePlayer player, Location location) {
    }
}
