# Custom Potion Branching Guide

## How the branch system works
- **Base brews** can be crafted directly from an Awkward potion by adding the listed ingredient.
- **Stronger variants** (or deeper branches) require **re-brewing the previous custom potion** with the new ingredient.
- The plugin enforces this by only allowing branch-only potions to be created from an existing custom potion, not straight from Awkward.

## Base brews (Awkward potion + ingredient)
These are the entry points for the potion tree:
- **Potion of Clear Mind** (`clear_mind`) + `AMETHYST_SHARD`
- **Potion of Copper Rush** (`copper_rush`) + `COPPER_INGOT`
- **Potion of Echoed Veil** (`echoed_veil`) + `ECHO_SHARD`
- **Potion of Honeyed Guard** (`honeyed_guard`) + `HONEYCOMB`
- **Potion of Glow Warmth** (`glow_warmth`) + `GLOW_BERRIES`
- **Potion of Deep Breath** (`deep_breath`) + `PRISMARINE_CRYSTALS`
- **Potion of Tidal Step** (`tidal_step`) + `NAUTILUS_SHELL`
- **Potion of Forager's Edge** (`forager_edge`) + `SWEET_BERRIES`
- **Potion of Crimson Guard** (`crimson_guard`) + `CRIMSON_FUNGUS`
- **Potion of Warped Focus** (`warped_focus`) + `WARPED_FUNGUS`

## Stronger variants (require previous potion + ingredient)
These potions **must** be brewed by re-adding the listed **previous custom potion** to the cauldron (or brewing stand) and then adding the ingredient. The ingredient **alone** (with Awkward or Water) will not work for these:
- **Elixir of Lucid Mind** (`lucid_mind`) requires: **Potion of Clear Mind** (`clear_mind`) *or* **Potion of Soulflame Guard** (`soulflame_guard`) *or* **Potion of Void Focus** (`void_focus`) + `AMETHYST_CLUSTER`
- **Potion of Tempered Rush** (`tempered_rush`) requires: **Potion of Copper Rush** (`copper_rush`) *or* **Elixir of Lucid Mind** (`lucid_mind`) *or* **Potion of Void Focus** (`void_focus`) + `COPPER_BLOCK`
- **Potion of Veiled Resonance** (`veiled_resonance`) requires: **Potion of Echoed Veil** (`echoed_veil`) *or* **Elixir of Lucid Mind** (`lucid_mind`) + `SCULK_CATALYST`
- **Potion of Amber Bulwark** (`amber_bulwark`) requires: **Potion of Honeyed Guard** (`honeyed_guard`) *or* **Potion of Tempered Rush** (`tempered_rush`) + `HONEY_BLOCK`
- **Potion of Radiant Warmth** (`radiant_warmth`) requires: **Potion of Glow Warmth** (`glow_warmth`) *or* **Potion of Tempered Rush** (`tempered_rush`) + `GLOWSTONE`
- **Potion of Abyssal Breath** (`abyssal_breath`) requires: **Potion of Deep Breath** (`deep_breath`) *or* **Potion of Amber Bulwark** (`amber_bulwark`) *or* **Potion of Radiant Warmth** (`radiant_warmth`) + `HEART_OF_THE_SEA`
- **Potion of Maelstrom Stride** (`maelstrom_stride`) requires: **Potion of Tidal Step** (`tidal_step`) *or* **Potion of Veiled Resonance** (`veiled_resonance`) *or* **Potion of Radiant Warmth** (`radiant_warmth`) *or* **Potion of Abyssal Breath** (`abyssal_breath`) + `SEA_LANTERN`
- **Potion of Harvester's Edge** (`harvester_edge`) requires: **Potion of Forager's Edge** (`forager_edge`) *or* **Potion of Abyssal Breath** (`abyssal_breath`) *or* **Potion of Maelstrom Stride** (`maelstrom_stride`) + `MOSS_BLOCK`
- **Potion of Soulflame Guard** (`soulflame_guard`) requires: **Potion of Crimson Guard** (`crimson_guard`) *or* **Potion of Amber Bulwark** (`amber_bulwark`) *or* **Potion of Harvester's Edge** (`harvester_edge`) + `MAGMA_CREAM`
- **Potion of Void Focus** (`void_focus`) requires: **Potion of Warped Focus** (`warped_focus`) *or* **Potion of Veiled Resonance** (`veiled_resonance`) *or* **Potion of Maelstrom Stride** (`maelstrom_stride`) *or* **Potion of Harvester's Edge** (`harvester_edge`) *or* **Potion of Soulflame Guard** (`soulflame_guard`) + `ENDER_EYE`

## Branch map
Use the following map to see which potion can be upgraded into another potion by adding the ingredient:

- **clear_mind**
  - ➜ **copper_rush** + `COPPER_INGOT`
  - ➜ **echoed_veil** + `ECHO_SHARD`
  - ➜ **lucid_mind** + `AMETHYST_CLUSTER`
- **copper_rush**
  - ➜ **honeyed_guard** + `HONEYCOMB`
  - ➜ **glow_warmth** + `GLOW_BERRIES`
  - ➜ **tempered_rush** + `COPPER_BLOCK`
- **echoed_veil**
  - ➜ **warped_focus** + `WARPED_FUNGUS`
  - ➜ **tidal_step** + `NAUTILUS_SHELL`
  - ➜ **veiled_resonance** + `SCULK_CATALYST`
- **honeyed_guard**
  - ➜ **crimson_guard** + `CRIMSON_FUNGUS`
  - ➜ **deep_breath** + `PRISMARINE_CRYSTALS`
  - ➜ **amber_bulwark** + `HONEY_BLOCK`
- **glow_warmth**
  - ➜ **deep_breath** + `PRISMARINE_CRYSTALS`
  - ➜ **tidal_step** + `NAUTILUS_SHELL`
  - ➜ **radiant_warmth** + `GLOWSTONE`
- **deep_breath**
  - ➜ **tidal_step** + `NAUTILUS_SHELL`
  - ➜ **forager_edge** + `SWEET_BERRIES`
  - ➜ **abyssal_breath** + `HEART_OF_THE_SEA`
- **tidal_step**
  - ➜ **forager_edge** + `SWEET_BERRIES`
  - ➜ **warped_focus** + `WARPED_FUNGUS`
  - ➜ **maelstrom_stride** + `SEA_LANTERN`
- **forager_edge**
  - ➜ **crimson_guard** + `CRIMSON_FUNGUS`
  - ➜ **warped_focus** + `WARPED_FUNGUS`
  - ➜ **harvester_edge** + `MOSS_BLOCK`
- **crimson_guard**
  - ➜ **warped_focus** + `WARPED_FUNGUS`
  - ➜ **clear_mind** + `AMETHYST_SHARD`
  - ➜ **soulflame_guard** + `MAGMA_CREAM`
- **warped_focus**
  - ➜ **clear_mind** + `AMETHYST_SHARD`
  - ➜ **copper_rush** + `COPPER_INGOT`
  - ➜ **void_focus** + `ENDER_EYE`
- **lucid_mind**
  - ➜ **tempered_rush** + `COPPER_BLOCK`
  - ➜ **veiled_resonance** + `SCULK_CATALYST`
- **tempered_rush**
  - ➜ **amber_bulwark** + `HONEY_BLOCK`
  - ➜ **radiant_warmth** + `GLOWSTONE`
- **veiled_resonance**
  - ➜ **void_focus** + `ENDER_EYE`
  - ➜ **maelstrom_stride** + `SEA_LANTERN`
- **amber_bulwark**
  - ➜ **soulflame_guard** + `MAGMA_CREAM`
  - ➜ **abyssal_breath** + `HEART_OF_THE_SEA`
- **radiant_warmth**
  - ➜ **abyssal_breath** + `HEART_OF_THE_SEA`
  - ➜ **maelstrom_stride** + `SEA_LANTERN`
- **abyssal_breath**
  - ➜ **maelstrom_stride** + `SEA_LANTERN`
  - ➜ **harvester_edge** + `MOSS_BLOCK`
- **maelstrom_stride**
  - ➜ **harvester_edge** + `MOSS_BLOCK`
  - ➜ **void_focus** + `ENDER_EYE`
- **harvester_edge**
  - ➜ **soulflame_guard** + `MAGMA_CREAM`
  - ➜ **void_focus** + `ENDER_EYE`
- **soulflame_guard**
  - ➜ **void_focus** + `ENDER_EYE`
  - ➜ **lucid_mind** + `AMETHYST_CLUSTER`
- **void_focus**
  - ➜ **lucid_mind** + `AMETHYST_CLUSTER`
  - ➜ **tempered_rush** + `COPPER_BLOCK`
