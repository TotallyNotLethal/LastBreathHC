package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.nickname.NicknameStorage;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickCommand implements BasicCommand {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 16;
    private static final String PERMISSION_NODE = "lastbreathhc.nick";
    private static final String HEX_COLOR_PATTERN = "(?i)" + ChatColor.COLOR_CHAR + "x("
            + ChatColor.COLOR_CHAR + "[0-9a-f]){6}";
    private static final Pattern HEX_INPUT_PATTERN = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Pattern DISALLOWED_FORMAT_PATTERN =
            Pattern.compile("(?i)(&[klmno]|" + ChatColor.COLOR_CHAR + "[klmno])");
    private static final Pattern FORBIDDEN_ROLE_PATTERN = Pattern.compile("(?i)(owner|admin|mod)");
    private final NamespacedKey nicknameKey;

    public NickCommand(LastBreathHC plugin) {
        this.nicknameKey = new NamespacedKey(plugin, "nickname");
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("off");
        }
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }
        if (!player.hasPermission(PERMISSION_NODE)) {
            player.sendMessage("Â§cNo permission.");
            return;
        }

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("off"))) {
            clearNickname(player);
            return;
        }

        String rawNickname = String.join(" ", args).trim();
        if (rawNickname.isEmpty()) {
            player.sendMessage("§cUsage: /nick <nickname> or /nick off");
            return;
        }
        if (containsWhitespace(rawNickname)) {
            player.sendMessage("§cNicknames cannot contain spaces.");
            return;
        }
        if (DISALLOWED_FORMAT_PATTERN.matcher(rawNickname).find()) {
            player.sendMessage("§cNicknames cannot use bold, italic, underline, strikethrough, or obfuscated text.");
            return;
        }
        String normalizedNickname = normalizeHexColors(rawNickname);
        String translated = ChatColor.translateAlternateColorCodes('&', normalizedNickname);
        String stripped = stripFormatting(translated);

        if (stripped.length() < MIN_LENGTH || stripped.length() > MAX_LENGTH) {
            player.sendMessage("§cNickname must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters.");
            return;
        }
        if (DISALLOWED_FORMAT_PATTERN.matcher(translated).find()) {
            player.sendMessage("§cNicknames cannot use bold, italic, underline, strikethrough, or obfuscated text.");
            return;
        }
        if (containsForbiddenRole(stripped)) {
            player.sendMessage("§cNicknames cannot contain staff titles.");
            return;
        }
        player.getPersistentDataContainer().set(nicknameKey, PersistentDataType.STRING, translated);
        applyNickname(player, translated);
        updateNicknameStats(player, translated);
        player.sendMessage("§aNickname set to " + translated + "§a.");
    }

    private void clearNickname(Player player) {
        player.getPersistentDataContainer().remove(nicknameKey);
        NicknameStorage.save(player.getUniqueId(), null);
        player.setDisplayName(player.getName());
        TitleManager.refreshPlayerTabTitle(player);
        updateNicknameStats(player, null);
        player.sendMessage("§aNickname cleared.");
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

    private String stripFormatting(String input) {
        if (input == null) {
            return "";
        }
        String withoutHex = input.replaceAll(HEX_COLOR_PATTERN, "");
        String stripped = ChatColor.stripColor(withoutHex);
        if (stripped == null) {
            return "";
        }
        return stripped.trim();
    }

    private boolean containsWhitespace(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (Character.isWhitespace(input.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsForbiddenRole(String stripped) {
        String lettersOnly = stripped.replaceAll("[^A-Za-z]", "");
        return FORBIDDEN_ROLE_PATTERN.matcher(lettersOnly).find();
    }

    private String normalizeHexColors(String input) {
        Matcher matcher = HEX_INPUT_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char digit : hex.toCharArray()) {
                replacement.append('&').append(digit);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
