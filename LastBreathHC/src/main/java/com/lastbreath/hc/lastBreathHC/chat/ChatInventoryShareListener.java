package com.lastbreath.hc.lastBreathHC.chat;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.chat.ChatInventoryShareService.ShareType;
import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticManager;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatInventoryShareListener implements Listener {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\[(inv|ec|item)]", Pattern.CASE_INSENSITIVE);
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private final LastBreathHC plugin;

    public ChatInventoryShareListener(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!ChatInventoryShareService.canShare(player)) {
            return;
        }
        String message = event.getMessage();
        Matcher matcher = TOKEN_PATTERN.matcher(message);
        if (!matcher.find()) {
            return;
        }

        event.setCancelled(true);
        Component formatted = buildFormattedMessage(player, message);
        Set<Player> recipients = Set.copyOf(event.getRecipients());
        Bukkit.getScheduler().runTask(plugin, () -> recipients.forEach(recipient -> recipient.sendMessage(formatted)));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isShareInventory(event.getInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isShareInventory(event.getInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    private boolean isShareInventory(InventoryHolder holder) {
        return holder instanceof ChatInventoryShareService.ShareInventoryHolder;
    }

    private Component buildFormattedMessage(Player player, String message) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        String nickname = stats.nickname;
        String displayName = nickname != null && !nickname.isBlank() ? nickname : player.getName();
        String prefixTag = CosmeticManager.getPrefixTag(player, false);
        Component name = LEGACY.deserialize(prefixTag + displayName);
        Component separator = Component.text(": ", NamedTextColor.GRAY);
        return name.append(separator).append(buildMessageComponent(message, player.getUniqueId()));
    }

    private Component buildMessageComponent(String message, UUID owner) {
        Matcher matcher = TOKEN_PATTERN.matcher(message);
        int lastIndex = 0;
        Component result = Component.empty();
        while (matcher.find()) {
            String before = message.substring(lastIndex, matcher.start());
            if (!before.isEmpty()) {
                result = result.append(Component.text(before, NamedTextColor.WHITE));
            }
            String token = matcher.group(1).toLowerCase(Locale.ROOT);
            ShareType type = ChatInventoryShareService.parseType(token);
            if (type != null) {
                result = result.append(buildTokenComponent(matcher.group(), type, owner));
            } else {
                result = result.append(Component.text(matcher.group(), NamedTextColor.WHITE));
            }
            lastIndex = matcher.end();
        }
        String tail = message.substring(lastIndex);
        if (!tail.isEmpty()) {
            result = result.append(Component.text(tail, NamedTextColor.WHITE));
        }
        return result;
    }

    private Component buildTokenComponent(String label, ShareType type, UUID owner) {
        String command = "/lbshowinv " + type.name().toLowerCase(Locale.ROOT) + " " + owner;
        String hoverText = "Click to view " + type.label();
        return Component.text(label, NamedTextColor.AQUA)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.GRAY)));
    }
}
