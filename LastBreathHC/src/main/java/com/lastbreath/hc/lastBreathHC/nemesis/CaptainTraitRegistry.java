package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CaptainTraitRegistry {

    private final LastBreathHC plugin;
    private final Map<String, TraitConfig> traits = new HashMap<>();
    private final Map<String, TraitDefinition> definitions = new HashMap<>();

    public CaptainTraitRegistry(LastBreathHC plugin) {
        this.plugin = plugin;
        registerDefaults();
        reload();
    }

    public void reload() {
        traits.clear();
        ConfigurationSection defs = plugin.getConfig().getConfigurationSection("nemesis.traits.definitions");
        if (defs == null) {
            return;
        }

        for (String id : defs.getKeys(false)) {
            ConfigurationSection row = defs.getConfigurationSection(id);
            if (row == null) {
                continue;
            }
            traits.put(id.toLowerCase(Locale.ROOT), new TraitConfig(
                    id.toLowerCase(Locale.ROOT),
                    Math.max(1, row.getInt("weight", 1)),
                    new HashSet<>(row.getStringList("requirements")),
                    new HashSet<>(row.getStringList("incompatibilities")),
                    row.getBoolean("weakness", false)
            ));
        }
    }

    public CaptainRecord.Traits selectTraits(UUID captainUuid, LivingEntity killer, UUID nemesisOf, CaptainRecord.NemesisScores scores) {
        long seed = captainUuid.getMostSignificantBits() ^ captainUuid.getLeastSignificantBits() ^ killer.getType().name().hashCode();
        Random random = new Random(seed);

        List<String> selectedTraits = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        selectedTraits.add("mob_" + killer.getType().name().toLowerCase(Locale.ROOT));

        String contextTrait = pickOne(filterByPrefixAndValidity("context_", killer, selectedTraits), random);
        if (contextTrait != null) {
            selectedTraits.add(contextTrait);
        }

        String personality = pickOne(filterByPrefixAndValidity("personality_", killer, selectedTraits), random);
        if (personality != null) {
            selectedTraits.add(personality);
        }

        List<String> weaknessPool = filterWeaknesses(killer, selectedTraits);
        String weakness = pickOne(weaknessPool, random);
        if (weakness != null) {
            weaknesses.add(weakness);
        } else {
            weaknesses.add("weakness_fragile");
        }

        return new CaptainRecord.Traits(selectedTraits, weaknesses, List.of());
    }

    private List<String> filterByPrefixAndValidity(String prefix, LivingEntity killer, List<String> selected) {
        List<String> candidates = new ArrayList<>();
        for (TraitConfig config : traits.values()) {
            if (!config.id().startsWith(prefix)) {
                continue;
            }
            if (!isValid(config, killer, selected)) {
                continue;
            }
            for (int i = 0; i < config.weight(); i++) {
                candidates.add(config.id());
            }
        }
        return candidates;
    }

    private List<String> filterWeaknesses(LivingEntity killer, List<String> selected) {
        List<String> candidates = new ArrayList<>();
        for (TraitConfig config : traits.values()) {
            if (!config.weakness()) {
                continue;
            }
            if (!isValid(config, killer, selected)) {
                continue;
            }
            for (int i = 0; i < config.weight(); i++) {
                candidates.add(config.id());
            }
        }
        return candidates;
    }

    private boolean isValid(TraitConfig config, LivingEntity killer, List<String> selected) {
        for (String requirement : config.requirements()) {
            String req = requirement.toLowerCase(Locale.ROOT);
            if (req.startsWith("mob:") && !killer.getType().name().equalsIgnoreCase(req.substring(4))) {
                return false;
            }
            if (req.equals("overworld") && killer.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
                return false;
            }
        }

        for (String selectedId : selected) {
            if (config.incompatibilities().contains(selectedId)) {
                return false;
            }
            TraitConfig selectedCfg = traits.get(selectedId);
            if (selectedCfg != null && selectedCfg.incompatibilities().contains(config.id())) {
                return false;
            }
        }
        return true;
    }

    private String pickOne(List<String> weighted, Random random) {
        if (weighted.isEmpty()) {
            return null;
        }
        return weighted.get(random.nextInt(weighted.size()));
    }

    public TraitDefinition definition(String id) {
        return definitions.get(id.toLowerCase(Locale.ROOT));
    }

    private void registerDefaults() {
        definitions.put("context_night_stalker", new NightStalkerTrait());
        definitions.put("personality_berserker", new BerserkerTrait());
        definitions.put("personality_predator", new PredatorTrait());
        definitions.put("weakness_fragile", new FragileWeaknessTrait());
        definitions.put("weakness_sunbound", new SunboundWeaknessTrait());
    }

    private record TraitConfig(String id, int weight, Set<String> requirements, Set<String> incompatibilities, boolean weakness) {
    }
}
