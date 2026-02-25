# Orc Warband Schematic Layouts (Captain → Warchief → Overlord)

This document defines **progressive orc base growth** for nemesis ranks and links to generated machine-readable layouts.

- Generated JSON: `docs/nemesis/generated/orc_warband_schematics.json`
- Generator script: `docs/nemesis/tools/orc_schematic_layout_generator.py`

## Design goals

1. Every rank slowly expands from stage 1 → 3.
2. Captains focus on huts and raider compounds.
3. Warchiefs scale into fortified settlements.
4. Overlords become regional fortress capitals.
5. Every variant has an orc-ish identity: spikes, trophies, smokeforges, beast pens, and ritual intimidation.

## Captain builds (huts)

### 1) Bonefire Raider Hut (`captain-bonefire-hut`)
- **Stage 1 / Scout Camp (9x9):** hide tent, cook trench, crate piles.
- **Stage 2 / Bonefire Hut (13x11):** tusk-framed hut + butcher rack + armory corner.
- **Stage 3 / Raider Compound (19x16):** guard huts + watch scaffold + brew pit.
- **Core flavor:** rough bone totems, ambush-ready firelight, raid trophies.

### 2) Scrap Longhut (`captain-scrap-longhut`)
- **Stage 1 / Wreck Shelter (8x10):** cart salvage shelter and ember brazier.
- **Stage 2 / Longhut Frame (16x8):** tactical hall + map table.
- **Stage 3 / War Longhut (22x12):** elite annex + wolf yard + warning pyres.
- **Core flavor:** frozen chains, scavenged metal, disciplined raider command space.

## Warchief builds (settlements)

### 3) Palisade War Settlement (`warchief-palisade-settlement`)
- **Stage 1 / Rally Ground (18x16):** captain tents and drum platform.
- **Stage 2 / Fortified Palisade (28x24):** enclosed lodge, cages, corner towers.
- **Stage 3 / Siege Hamlet (36x30):** forge, ammo sheds, beast pen, shaman hut.
- **Core flavor:** organized warband district with visible military hierarchy.

### 4) Smokeforge Kraal (`warchief-smokeforge-kraal`)
- **Stage 1 / Ash Corral (20x14):** beast corrals and ore handling.
- **Stage 2 / Smokeforge Hub (30x24):** smith bays and crane bridge.
- **Stage 3 / Warfoundry District (40x32):** foundry hall + armor stores + beacon.
- **Core flavor:** industrial, brutal, heat-and-smoke driven orc logistics hub.

## Overlord builds (strongholds)

### 5) Ironmaw Stronghold (`overlord-ironmaw-stronghold`)
- **Stage 1 / War Citadel Core (34x28):** keep, execution ring, signal towers.
- **Stage 2 / Conquest Bastion (46x38):** barracks, ritual arena, tribute vault.
- **Stage 3 / Empire Seat (60x52):** throne fortress + arena + tower + ziggurat.
- **Core flavor:** absolute dominion fortress with layered military infrastructure.

### 6) Bloodmoon Redoubt (`overlord-bloodmoon-redoubt`)
- **Stage 1 / Totem Bastion (30x30):** ritual mound and totem circles.
- **Stage 2 / Hexwall Fortress (44x40):** hex walls + corruption cistern.
- **Stage 3 / Bloodmoon Citadel (58x54):** citadel keep + altar basin + eclipse beacon.
- **Core flavor:** occult war-state with fear, curses, and night patrols.

## Usage

Regenerate the JSON catalog any time layouts change:

```bash
python3 LastBreathHC/docs/nemesis/tools/orc_schematic_layout_generator.py
```

The output can be transformed into:
- litematic template metadata,
- in-plugin structure footprint configs,
- weighted rank progression pools.
