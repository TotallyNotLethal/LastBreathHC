# Storm Herald — World Boss Guide

## Overview
The **Storm Herald** is a Wither-based boss that begins shielded by lightning-rod anchors. Players must destroy every anchor to remove the shield. Once unsealed, the boss unleashes chain lightning and wind gusts, and becomes **enraged** at low health for increased damage and shorter cooldowns.

## Stats
- **Entity type:** `WITHER`. 
- **Arena spawn radius:** `50` blocks from the arena center. 
- **Despawn timer:** `600` seconds (10 minutes). 
- **Anchor lightning safe radius:** `3.0` blocks (configurable). 

> **Config source:** `worldBoss.bosses.StormHerald.*` in `config.yml`.

## Phases & Mechanics
### Phase 1 — Shielded
- **Invulnerable:** The boss is immune to damage while anchors remain. 
- **Anchors:** Four lightning rods (`LIGHTNING_ROD`) spawn in a ring around the boss. 
- **Safe rings:** Each anchor projects a safe ring that protects from lightning strikes.

### Phase 2 — Stormcaller
- **Chain lightning:** A lightning pulse hits players within 15 blocks. Players outside anchor safe rings take damage. 
- **Wind gusts:** Periodic knockback blast hits players within 12 blocks, dealing damage and pushing them away.
- **Enrage threshold:** At **≤20% health**, the boss becomes enraged, gaining a speed boost and more frequent attacks.

## Custom Moves
- **Anchor Shield:** Invulnerability tied to lightning-rod anchors. 
- **Chain Lightning:** Damages nearby players (4 damage base; 7 damage when enraged), with safe zones around anchors. 
- **Wind Gust:** Knockback and damage (2 damage base; 3 damage when enraged) to nearby players. 
- **Slowing Curse:** Boss attacks apply slowness (stronger when enraged).

## How to Defeat the Storm Herald (Step-by-Step)
1. **Identify and destroy the anchors.** The boss is invulnerable until all lightning rods are destroyed.
2. **Fight near anchor safe rings.** Standing near anchors provides safe zones against chain lightning while the shield is up.
3. **Push into Stormcaller phase.** Once all anchors are destroyed, the boss becomes vulnerable.
4. **Manage spacing.** Stay beyond 12 blocks when possible to avoid wind gust knockback. 
5. **Respect chain lightning.** Stay aware of the 15-block lightning radius and avoid clumping outside safe rings.
6. **Prepare for enrage.** At 20% health, the boss speeds up and attacks more frequently.
7. **Finish with coordinated damage.** Sustain DPS while rotating around safe zones and avoiding gusts.
