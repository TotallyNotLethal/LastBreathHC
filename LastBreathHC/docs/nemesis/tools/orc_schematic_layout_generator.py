#!/usr/bin/env python3
"""Generate orc-themed schematic layout plans for nemesis ranks.

This creates progressive, stage-based base plans for captains, warchiefs,
and overlords with multiple variants each. Output is JSON so it can be
translated into in-game structure templates later.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True)
class StagePlan:
    stage: int
    title: str
    build_goal: str
    footprint: tuple[int, int]
    new_structures: list[str]
    block_palette: list[str]
    defenses: list[str]
    flavor: str


@dataclass(frozen=True)
class VariantPlan:
    rank: str
    variant_id: str
    display_name: str
    biome_fit: list[str]
    command_post: str
    stages: list[StagePlan]


def _stages(rows: Iterable[dict]) -> list[StagePlan]:
    return [StagePlan(**row) for row in rows]


def build_catalog() -> list[VariantPlan]:
    return [
        VariantPlan(
            rank="CAPTAIN",
            variant_id="captain-bonefire-hut",
            display_name="Bonefire Raider Hut",
            biome_fit=["plains", "savanna", "badlands"],
            command_post="spike-ring firepit with hanging skull lanterns",
            stages=_stages(
                [
                    {
                        "stage": 1,
                        "title": "Scout Camp",
                        "build_goal": "Establish a hidden staging point after first kill.",
                        "footprint": (9, 9),
                        "new_structures": [
                            "1x hide tent",
                            "1x cook-fire trench",
                            "2x crate piles",
                        ],
                        "block_palette": ["spruce_planks", "oak_log", "campfire", "bone_block"],
                        "defenses": ["thorn hedge", "tripwire alarm"],
                        "flavor": "Ash-stained banners mark fresh blood-debt territory.",
                    },
                    {
                        "stage": 2,
                        "title": "Bonefire Hut",
                        "build_goal": "Upgrade into a durable hut for captain + 2 guards.",
                        "footprint": (13, 11),
                        "new_structures": [
                            "1x tusk-framed hut",
                            "1x butcher rack",
                            "1x scavenged armory corner",
                        ],
                        "block_palette": ["dark_oak_planks", "stripped_spruce_log", "bone_block", "chain"],
                        "defenses": ["spike ditch", "wolf-pen gate"],
                        "flavor": "The captain displays enemy helmets above the doorway.",
                    },
                    {
                        "stage": 3,
                        "title": "Raider Compound",
                        "build_goal": "Create a mini-cluster to support patrol raids.",
                        "footprint": (19, 16),
                        "new_structures": [
                            "2x raider sleeping huts",
                            "1x watch scaffold",
                            "1x brew barrel pit",
                        ],
                        "block_palette": ["mud_bricks", "dark_oak_fence", "blackstone", "torch"],
                        "defenses": ["raised archer perch", "firepot chokepoint"],
                        "flavor": "Drums and horn blasts can be heard from two hills away.",
                    },
                ]
            ),
        ),
        VariantPlan(
            rank="CAPTAIN",
            variant_id="captain-scrap-longhut",
            display_name="Scrap Longhut",
            biome_fit=["taiga", "snowy_plains", "windswept_hills"],
            command_post="iron-scrap throne under stitched hide roof",
            stages=_stages(
                [
                    {
                        "stage": 1,
                        "title": "Wreck Shelter",
                        "build_goal": "Salvage ruined carts into temporary shelter.",
                        "footprint": (8, 10),
                        "new_structures": ["1x tilted cart-roof", "1x ember brazier"],
                        "block_palette": ["spruce_slab", "iron_bars", "cobblestone", "soul_torch"],
                        "defenses": ["noise traps", "snow berm wall"],
                        "flavor": "Frozen chains rattle to warn the camp.",
                    },
                    {
                        "stage": 2,
                        "title": "Longhut Frame",
                        "build_goal": "Expand to a narrow war hall for tactical planning.",
                        "footprint": (16, 8),
                        "new_structures": ["1x longhut", "1x map table", "1x supply lean-to"],
                        "block_palette": ["spruce_log", "spruce_planks", "packed_mud", "iron_bars"],
                        "defenses": ["front stake wall", "flanking barricades"],
                        "flavor": "Clan glyphs burned into planks record past raids.",
                    },
                    {
                        "stage": 3,
                        "title": "War Longhut",
                        "build_goal": "Fully armed hall with elite guard bunk annex.",
                        "footprint": (22, 12),
                        "new_structures": ["1x elite bunk annex", "1x chained wolf yard", "2x warning pyres"],
                        "block_palette": ["deepslate_tiles", "spruce_trapdoor", "chain", "lantern"],
                        "defenses": ["double gate entry", "crossfire lane"],
                        "flavor": "Smoke stacks paint the sky with oily black banners.",
                    },
                ]
            ),
        ),
        VariantPlan(
            rank="WARCHIEF",
            variant_id="warchief-palisade-settlement",
            display_name="Palisade War Settlement",
            biome_fit=["plains", "forest", "savanna"],
            command_post="central war dais surrounded by trophy poles",
            stages=_stages(
                [
                    {
                        "stage": 1,
                        "title": "Rally Ground",
                        "build_goal": "Unify nearby captains with a shared meeting yard.",
                        "footprint": (18, 16),
                        "new_structures": ["1x drum platform", "3x captain tents", "1x ration pit"],
                        "block_palette": ["oak_log", "spruce_fence", "hay_block", "campfire"],
                        "defenses": ["sentry ring", "alarm horns"],
                        "flavor": "Every post bears marks from sworn captains.",
                    },
                    {
                        "stage": 2,
                        "title": "Fortified Palisade",
                        "build_goal": "Enclose settlement and build command lodge.",
                        "footprint": (28, 24),
                        "new_structures": [
                            "1x warchief lodge",
                            "1x prisoner cage lane",
                            "4x corner watch towers",
                        ],
                        "block_palette": ["dark_oak_log", "stripped_oak_log", "stone_bricks", "chain"],
                        "defenses": ["reinforced gate", "murder slit walkway"],
                        "flavor": "Captured shields are bolted into the front gate.",
                    },
                    {
                        "stage": 3,
                        "title": "Siege Hamlet",
                        "build_goal": "Turn camp into a siege-forward operating base.",
                        "footprint": (36, 30),
                        "new_structures": [
                            "1x crude forge",
                            "2x ammo sheds",
                            "1x beast pen",
                            "1x war shaman hut",
                        ],
                        "block_palette": ["blackstone", "polished_andesite", "mangrove_planks", "blast_furnace"],
                        "defenses": ["outer spike belt", "boiling-oil catwalk"],
                        "flavor": "Drumbeats keep all squads on one marching rhythm.",
                    },
                ]
            ),
        ),
        VariantPlan(
            rank="WARCHIEF",
            variant_id="warchief-smokeforge-kraal",
            display_name="Smokeforge Kraal",
            biome_fit=["badlands", "stony_peaks", "desert"],
            command_post="lava-fed forge throne with chained anvils",
            stages=_stages(
                [
                    {
                        "stage": 1,
                        "title": "Ash Corral",
                        "build_goal": "Fence a volcanic corral for beasts and workers.",
                        "footprint": (20, 14),
                        "new_structures": ["1x beast corral", "1x ore cart pit", "1x slag dump"],
                        "block_palette": ["acacia_fence", "blackstone", "magma_block", "barrel"],
                        "defenses": ["magma trench", "smoke veil"],
                        "flavor": "Ore dust and blood mix in the same mud.",
                    },
                    {
                        "stage": 2,
                        "title": "Smokeforge Hub",
                        "build_goal": "Install forge lines and command bridge.",
                        "footprint": (30, 24),
                        "new_structures": ["2x smith bays", "1x command bridge", "1x chain-lift crane"],
                        "block_palette": ["deepslate_bricks", "iron_bars", "chain", "blast_furnace"],
                        "defenses": ["heated grate choke", "tower ballista"],
                        "flavor": "The warchief judges captains by the quality of forged axes.",
                    },
                    {
                        "stage": 3,
                        "title": "Warfoundry District",
                        "build_goal": "Mass-produce gear for regional domination pushes.",
                        "footprint": (40, 32),
                        "new_structures": ["1x foundry hall", "2x armor stores", "1x smoke beacon", "1x war map hall"],
                        "block_palette": ["nether_bricks", "polished_blackstone", "lava_cauldron", "anvil"],
                        "defenses": ["triple gate", "overhead chain drop traps"],
                        "flavor": "A constant furnace roar drowns out screams from the cages.",
                    },
                ]
            ),
        ),
        VariantPlan(
            rank="OVERLORD",
            variant_id="overlord-ironmaw-stronghold",
            display_name="Ironmaw Stronghold",
            biome_fit=["mountains", "stony_peaks", "windswept_hills"],
            command_post="obsidian throne pit with chained war map",
            stages=_stages(
                [
                    {
                        "stage": 1,
                        "title": "War Citadel Core",
                        "build_goal": "Anchor overlord authority with a stone keep nucleus.",
                        "footprint": (34, 28),
                        "new_structures": ["1x central keep", "1x execution ring", "2x signal towers"],
                        "block_palette": ["stone_bricks", "polished_blackstone", "obsidian", "soul_fire"],
                        "defenses": ["elevated gatehouse", "drawbridge killbox"],
                        "flavor": "The overlord decrees are carved into iron plates.",
                    },
                    {
                        "stage": 2,
                        "title": "Conquest Bastion",
                        "build_goal": "Add barracks and ritual courts to secure loyalty.",
                        "footprint": (46, 38),
                        "new_structures": ["2x legion barracks", "1x ritual arena", "1x tribute vault"],
                        "block_palette": ["deepslate_tiles", "iron_block", "chiseled_stone_bricks", "chain"],
                        "defenses": ["arrow parapets", "gate cauldron emplacements"],
                        "flavor": "Rival captains kneel before weekly blood oaths.",
                    },
                    {
                        "stage": 3,
                        "title": "Empire Seat",
                        "build_goal": "Complete capital-grade fortress controlling the region.",
                        "footprint": (60, 52),
                        "new_structures": [
                            "1x overlord throne fortress",
                            "1x war beast arena",
                            "1x treasury tower",
                            "1x command ziggurat",
                        ],
                        "block_palette": ["blackstone", "gilded_blackstone", "ancient_debris", "redstone_lamp"],
                        "defenses": ["multi-layer walls", "inner doom gate", "lava moat"],
                        "flavor": "Sky-fires burn night and day to mark total domination.",
                    },
                ]
            ),
        ),
        VariantPlan(
            rank="OVERLORD",
            variant_id="overlord-bloodmoon-redoubt",
            display_name="Bloodmoon Redoubt",
            biome_fit=["swamp", "dark_forest", "jungle_edge"],
            command_post="ritual mound ringed by crimson totems",
            stages=_stages(
                [
                    {
                        "stage": 1,
                        "title": "Totem Bastion",
                        "build_goal": "Raise fear with cursed totem circles and palisades.",
                        "footprint": (30, 30),
                        "new_structures": ["1x ritual mound", "3x totem circles", "1x shaman hall"],
                        "block_palette": ["mangrove_log", "mud_bricks", "red_nether_bricks", "soul_lantern"],
                        "defenses": ["bog trench", "totem fear aura perimeter"],
                        "flavor": "Moonlit rites bind captains under dread omens.",
                    },
                    {
                        "stage": 2,
                        "title": "Hexwall Fortress",
                        "build_goal": "Construct layered hex walls and corruption pools.",
                        "footprint": (44, 40),
                        "new_structures": ["1x hexwall ring", "1x corruption cistern", "2x hunter towers"],
                        "block_palette": ["cracked_deepslate_bricks", "mossy_stone_bricks", "crying_obsidian", "chain"],
                        "defenses": ["swamp gas burners", "ambush boardwalk"],
                        "flavor": "Nocturnal warbands disappear into marsh mist patrols.",
                    },
                    {
                        "stage": 3,
                        "title": "Bloodmoon Citadel",
                        "build_goal": "Manifest full occult military capital.",
                        "footprint": (58, 54),
                        "new_structures": ["1x citadel keep", "1x blood altar basin", "1x beast crypt", "1x eclipse beacon"],
                        "block_palette": ["netherite_block", "red_nether_bricks", "polished_basalt", "respawn_anchor"],
                        "defenses": ["curse totem gauntlet", "inner spirit firewall"],
                        "flavor": "The overlord is proclaimed war-prophet of the red moon.",
                    },
                ]
            ),
        ),
    ]


def main() -> None:
    catalog = build_catalog()
    out = {
        "schema": "lastbreath.nemesis.orc-layout.v1",
        "summary": {
            "ranks": ["CAPTAIN", "WARCHIEF", "OVERLORD"],
            "progression_rule": "Each variant advances from stage 1 to 3 based on rank milestones and regional pressure.",
            "theme": "Orc warbands that escalate from rough encampments into fortified regional capitals.",
        },
        "variants": [
            {
                "rank": variant.rank,
                "variantId": variant.variant_id,
                "displayName": variant.display_name,
                "biomeFit": variant.biome_fit,
                "commandPost": variant.command_post,
                "stages": [
                    {
                        "stage": s.stage,
                        "title": s.title,
                        "buildGoal": s.build_goal,
                        "footprint": {"width": s.footprint[0], "depth": s.footprint[1]},
                        "newStructures": s.new_structures,
                        "blockPalette": s.block_palette,
                        "defenses": s.defenses,
                        "flavor": s.flavor,
                    }
                    for s in variant.stages
                ],
            }
            for variant in catalog
        ],
    }

    output_path = Path(__file__).resolve().parents[1] / "generated" / "orc_warband_schematics.json"
    output_path.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(catalog)} variants to {output_path}")


if __name__ == "__main__":
    main()
