package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        String traitSuffix = selectTraitSuffix(random, traits);
        if (!traitSuffix.isBlank()) {
            suffix = suffix + " " + traitSuffix;
        }

        NameModifier modifier = selectModifier(random, entity, traits);
        String title = modifier == null ? "Captain" : modifier.title();
        String epithet = selectEpithet(random, traits, modifier);

        String prefix = modifier == null ? "" : modifier.prefix();
        String tail = modifier == null ? "" : modifier.suffix();
        String displayName = (prefix + first + " " + suffix + tail).trim() + " " + epithet;
        displayName = ensureUniqueDisplayName(displayName, captainId);
        String aliasSeed = Long.toHexString(seed).toUpperCase(Locale.ROOT);
        return new CaptainRecord.Naming(displayName.trim(), epithet, title, aliasSeed);
    }

    private String selectTraitSuffix(Random random, CaptainRecord.Traits traits) {
        if (traits == null) {
            return "";
        }
        String traitType = resolveTraitType(traits);
        if (traitType == null) {
            return "";
        }
        List<String> pool = weightedEntries("nemesis.naming.orcish.traitSuffixes." + traitType, random);
        return pick(pool, random, "");
    }

    private String resolveTraitType(CaptainRecord.Traits traits) {
        if (containsPrefix(traits.traits(), "strength_")) {
            return "strength";
        }
        if (containsPrefix(traits.immunities(), "immunity_")) {
            return "immunity";
        }
        if (containsPrefix(traits.weaknesses(), "weakness_")) {
            return "weakness";
        }
        if (containsPrefix(traits.traits(), "personality_")) {
            return "personality";
        }
        if (containsPrefix(traits.traits(), "context_")) {
            return "context";
        }
        return null;
    }

    private boolean containsPrefix(List<String> values, String prefix) {
        if (values == null) {
            return false;
        }
        return values.stream().anyMatch(value -> value != null && value.startsWith(prefix));
    }

    private String ensureUniqueDisplayName(String baseName, UUID captainId) {
        if (plugin.getCaptainRegistry() == null) {
            return baseName;
        }
        String normalizedBase = baseName.trim();
        long existing = plugin.getCaptainRegistry().getAll().stream()
                .filter(record -> record.naming() != null)
                .map(record -> record.naming().displayName())
                .filter(name -> name != null && name.equalsIgnoreCase(normalizedBase))
                .count();
        if (existing == 0L) {
            return normalizedBase;
        }
        String token = captainId.toString().substring(0, 4).toUpperCase(Locale.ROOT);
        return normalizedBase + " #" + token;
    }

    private String selectEpithet(Random random, CaptainRecord.Traits traits, NameModifier modifier) {
        List<String> pool = new ArrayList<>(weightedEpithetEntries(random, traits));
        String modifierEpithet = modifier == null ? "the Relentless" : modifier.epithet();
        if (!modifierEpithet.isBlank()) {
            pool.add(modifierEpithet);
        }
        if (pool.isEmpty()) {
            pool.add("the Relentless");
        }
        Collections.shuffle(pool, random);

        Set<String> usedEpithets = existingEpithets();
        for (String candidate : pool) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (!usedEpithets.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return pick(pool, random, "the Relentless");
    }

    private List<String> weightedEpithetEntries(Random random, CaptainRecord.Traits traits) {
        List<String> weighted = new ArrayList<>();
        String traitType = traits == null ? null : resolveTraitType(traits);
        if (traitType != null) {
            weighted.addAll(weightedEntries("nemesis.naming.epithets." + traitType, random));
        }
        weighted.addAll(weightedEntries("nemesis.naming.epithets.generic", random));
        return weighted;
    }

    private Set<String> existingEpithets() {
        if (plugin.getCaptainRegistry() == null) {
            return Set.of();
        }
        return plugin.getCaptainRegistry().getAll().stream()
                .filter(record -> record.naming() != null)
                .map(record -> record.naming().epithet())
                .filter(epithet -> epithet != null && !epithet.isBlank())
                .map(epithet -> epithet.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private NameModifier selectModifier(Random random, LivingEntity entity, CaptainRecord.Traits traits) {
        ConfigurationSection section = getConfigSection("nemesis.naming.modifiers");
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
        ConfigurationSection section = getConfigSection(path);
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

    private ConfigurationSection getConfigSection(String path) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null && !section.getKeys(false).isEmpty()) {
            return section;
        }

        FileConfiguration defaults = config.getDefaults();
        if (defaults == null) {
            return section;
        }

        ConfigurationSection defaultSection = defaults.getConfigurationSection(path);
        if (defaultSection != null && !defaultSection.getKeys(false).isEmpty()) {
            return defaultSection;
        }
        return section;
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
