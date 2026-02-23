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
            String normalizedId = id.toLowerCase(Locale.ROOT);
            traits.put(normalizedId, new TraitConfig(
                    normalizedId,
                    Math.max(1, row.getInt("weight", 1)),
                    new HashSet<>(normalize(row.getStringList("requirements"))),
                    new HashSet<>(normalize(row.getStringList("incompatibilities"))),
                    row.getBoolean("weakness", false),
                    row.getBoolean("immunity", false),
                    row.getString("displayName", prettyName(id))
            ));
        }
    }

    public CaptainRecord.Traits selectTraits(UUID captainUuid, LivingEntity killer, UUID nemesisOf, CaptainRecord.NemesisScores scores) {
        long seed = captainUuid.getMostSignificantBits() ^ captainUuid.getLeastSignificantBits() ^ killer.getType().name().hashCode();
        Random random = new Random(seed);

        List<String> selectedTraits = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> immunities = new ArrayList<>();

        selectedTraits.add("mob_" + killer.getType().name().toLowerCase(Locale.ROOT));

        String contextTrait = pickOne(filterByPrefixAndValidity("context_", killer, selectedTraits), random);
        if (contextTrait != null) {
            selectedTraits.add(contextTrait);
        }

        String personality = pickOne(filterByPrefixAndValidity("personality_", killer, selectedTraits), random);
        if (personality != null) {
            selectedTraits.add(personality);
        }

        String strength = pickOne(filterByPrefixAndValidity("strength_", killer, selectedTraits), random);
        selectedTraits.add(strength == null ? "strength_brutal_strikes" : strength);

        if (random.nextDouble() <= 0.5) {
            int weaknessCount = 1;
            if (random.nextDouble() <= 0.35) {
                weaknessCount++;
            }
            if (weaknessCount < 3 && random.nextDouble() <= 0.2) {
                weaknessCount++;
            }

            for (int i = 0; i < weaknessCount; i++) {
                List<String> selectedAndWeaknesses = new ArrayList<>(selectedTraits);
                selectedAndWeaknesses.addAll(weaknesses);
                String weakness = pickOne(filterWeaknesses(killer, selectedAndWeaknesses), random);
                if (weakness == null || weaknesses.contains(weakness)) {
                    break;
                }
                weaknesses.add(weakness);
            }

            if (weaknesses.isEmpty()) {
                weaknesses.add("weakness_fragile");
            }
        }

        String immunity = pickOne(filterImmunities(killer, selectedTraits), random);
        if (immunity != null) {
            immunities.add(immunity);
        }

        return new CaptainRecord.Traits(selectedTraits, weaknesses, immunities);
    }

    private List<String> filterByPrefixAndValidity(String prefix, LivingEntity killer, List<String> selected) {
        List<String> candidates = new ArrayList<>();
        for (TraitConfig config : traits.values()) {
            if (!config.id().startsWith(prefix) || config.weakness() || config.immunity()) {
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
            if (!config.weakness() || !isValid(config, killer, selected)) {
                continue;
            }
            for (int i = 0; i < config.weight(); i++) {
                candidates.add(config.id());
            }
        }
        return candidates;
    }

    private List<String> filterImmunities(LivingEntity killer, List<String> selected) {
        List<String> candidates = new ArrayList<>();
        for (TraitConfig config : traits.values()) {
            if (!config.immunity() || !isValid(config, killer, selected)) {
                continue;
            }
            for (int i = 0; i < config.weight(); i++) {
                candidates.add(config.id());
            }
        }
        return candidates;
    }

    private boolean isValid(TraitConfig config, LivingEntity killer, List<String> selected) {
        for (String req : config.requirements()) {
            if (req.startsWith("mob:") && !killer.getType().name().equalsIgnoreCase(req.substring(4))) {
                return false;
            }
            if (req.equals("overworld") && killer.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
                return false;
            }
            if (req.equals("night") && killer.getWorld().getTime() < 12000L) {
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

    public String displayName(String id) {
        if (id == null) {
            return "Unknown";
        }
        TraitConfig cfg = traits.get(id.toLowerCase(Locale.ROOT));
        return cfg == null ? prettyName(id) : cfg.displayName();
    }

    private void registerDefaults() {
        definitions.put("context_night_stalker", new NightStalkerTrait());
        definitions.put("context_frostbound", new FrostboundTrait());
        definitions.put("personality_berserker", new BerserkerTrait());
        definitions.put("personality_predator", new PredatorTrait());
        definitions.put("strength_brutal_strikes", new BrutalStrikesTrait());
        definitions.put("weakness_fragile", new FragileWeaknessTrait());
        definitions.put("weakness_sunbound", new SunboundWeaknessTrait());
        definitions.put("weakness_slow_recovery", new SlowRecoveryWeaknessTrait());
        definitions.put("immunity_knockback", new KnockbackImmunityTrait());
        definitions.put("immunity_fireproof", new FireproofImmunityTrait());
        definitions.put("weakness_gold_cursed", new GoldCursedWeaknessTrait());
        definitions.put("weakness_fire_vulnerable", new FireVulnerableWeaknessTrait());
        definitions.put("weakness_daylight_hunted", new DaylightHuntedWeaknessTrait());
        definitions.put("weakness_holy_water", new HolyWaterWeaknessTrait());
        definitions.put("strength_vicious_combo", new ViciousComboTrait());
        definitions.put("strength_warlord_presence", new WarlordPresenceTrait());
        definitions.put("strength_venom_blade", new VenomBladeTrait());
        definitions.put("immunity_projectile_guard", new ProjectileGuardImmunityTrait());
    }

    private List<String> normalize(List<String> values) {
        return values.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList();
    }

    private String prettyName(String id) {
        String[] words = id.replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (w.equals("context") || w.equals("personality") || w.equals("strength") || w.equals("weakness") || w.equals("immunity")) {
                continue;
            }
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.isEmpty() ? id : out.toString();
    }

    private record TraitConfig(String id, int weight, Set<String> requirements, Set<String> incompatibilities,
                               boolean weakness, boolean immunity, String displayName) {
    }
}
