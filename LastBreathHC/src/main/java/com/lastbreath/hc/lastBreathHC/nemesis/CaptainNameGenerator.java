package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class CaptainNameGenerator {
    private final LastBreathHC plugin;

    public CaptainNameGenerator(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    public CaptainRecord.Naming generate(UUID captainId, LivingEntity entity, CaptainRecord.Traits traits) {
        long seed = captainId.getMostSignificantBits() ^ captainId.getLeastSignificantBits() ^ (long) entity.getType().name().hashCode();
        Random random = new Random(seed);

        List<String> firstNames = weightedEntries("nemesis.naming.orcish.firstNames", random);
        List<String> suffixes = weightedEntries("nemesis.naming.orcish.suffixes", random);

        String first = pick(firstNames, random, entity.getType().name().substring(0, 1).toUpperCase(Locale.ROOT) + entity.getType().name().substring(1).toLowerCase(Locale.ROOT));
        String suffix = pick(suffixes, random, "fang");

        NameModifier modifier = selectModifier(random, entity, traits);
        String title = modifier == null ? "Captain" : modifier.title();
        String epithet = modifier == null ? "the Relentless" : modifier.epithet();

        String prefix = modifier == null ? "" : modifier.prefix();
        String tail = modifier == null ? "" : modifier.suffix();
        String displayName = (prefix + first + " " + suffix + tail).trim() + " " + epithet;
        String aliasSeed = Long.toHexString(seed).toUpperCase(Locale.ROOT);
        return new CaptainRecord.Naming(displayName.trim(), epithet, title, aliasSeed);
    }

    private NameModifier selectModifier(Random random, LivingEntity entity, CaptainRecord.Traits traits) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("nemesis.naming.modifiers");
        if (section == null) {
            return null;
        }
        List<NameModifier> pool = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection row = section.getConfigurationSection(key);
            if (row == null || !matches(row, entity, traits)) {
                continue;
            }
            int weight = Math.max(1, row.getInt("weight", 1));
            NameModifier modifier = new NameModifier(
                    row.getString("prefix", ""),
                    row.getString("suffix", ""),
                    row.getString("epithet", "the Relentless"),
                    row.getString("title", "Captain")
            );
            for (int i = 0; i < weight; i++) {
                pool.add(modifier);
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private boolean matches(ConfigurationSection row, LivingEntity entity, CaptainRecord.Traits traits) {
        List<String> mobTypes = row.getStringList("mobTypes");
        if (!mobTypes.isEmpty()) {
            boolean matched = mobTypes.stream().anyMatch(type -> type.equalsIgnoreCase(entity.getType().name()));
            if (!matched) {
                return false;
            }
        }
        List<String> requiredTraits = row.getStringList("requiredTraits");
        if (!requiredTraits.isEmpty() && traits != null) {
            for (String required : requiredTraits) {
                if (!traits.traits().contains(required) && !traits.weaknesses().contains(required)) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<String> weightedEntries(String path, Random random) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        List<String> weighted = new ArrayList<>();
        if (section == null) {
            return weighted;
        }
        for (String key : section.getKeys(false)) {
            int weight = Math.max(1, section.getInt(key + ".weight", 1));
            String value = section.getString(key + ".value", key);
            for (int i = 0; i < weight; i++) {
                weighted.add(value);
            }
        }
        return weighted;
    }

    private String pick(List<String> options, Random random, String fallback) {
        if (options.isEmpty()) {
            return fallback;
        }
        return options.get(random.nextInt(options.size()));
    }

    private record NameModifier(String prefix, String suffix, String epithet, String title) {
    }
}
