package com.lastbreath.hc.lastBreathHC.daily;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EffectRewardAction implements DailyRewardAction {

    private final PotionEffectType effectType;
    private final int durationSeconds;
    private final int amplifier;

    public EffectRewardAction(PotionEffectType effectType, int durationSeconds, int amplifier) {
        this.effectType = effectType;
        this.durationSeconds = Math.max(1, durationSeconds);
        this.amplifier = Math.max(0, amplifier);
    }

    @Override
    public String preview() {
        return ChatColor.LIGHT_PURPLE + "• " + ChatColor.WHITE + formatName() + " " + roman(amplifier + 1) + " " + durationSeconds + "s";
    }

    @Override
    public String grant(Player player) {
        player.addPotionEffect(new PotionEffect(effectType, durationSeconds * 20, amplifier, true, true, true));
        return formatName() + " " + roman(amplifier + 1) + " (" + durationSeconds + "s)";
    }

    private String formatName() {
        String key = effectType.getKey().getKey().replace('_', ' ');
        String[] words = key.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private static String roman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }
}
