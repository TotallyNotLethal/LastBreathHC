# Storm Herald — World Boss Guide

## Overview
The **Storm Herald** is a Wither-based boss that begins shielded by lightning-rod anchors. Players must destroy every anchor to remove the shield. Once unsealed, the boss unleashes chain lightning and wind gusts, then escalates into a **Tempest** phase at low health with stronger damage, larger radius, and a dedicated tempest storm attack.

## Stats
- **Entity type:** `WITHER`.
- **Arena spawn radius:** `50` blocks from the arena center.
- **Despawn timer:** `600` seconds (10 minutes).
- **Anchor lightning safe radius:** `3.0` blocks (configurable).
- **Tempest threshold:** `30%` health (configurable).

> **Config source:** `worldBoss.bosses.StormHerald.*` in `config.yml`.

## Phases & Mechanics
### Phase 1 — Shielded
- **Invulnerable:** The boss is immune to damage while anchors remain.
- **Anchors:** Four lightning rods (`LIGHTNING_ROD`) spawn in a ring around the boss.
- **Safe rings:** Each anchor projects a safe ring that protects from chain lightning.

### Phase 2 — Stormcaller
- **Chain lightning:** A lightning pulse hits players within 16 blocks of the boss. Players inside anchor safe rings are protected.
- **Wind gusts:** Periodic knockback blast damages and pushes players within the wind gust radius.
- **Slowing curse:** Boss attacks apply **Slowness I**.

### Phase 3 — Tempest (Low Health)
- **Trigger:** At ≤30% health.
- **Increased pressure:** Chain lightning and gusts deal bonus damage and fire more frequently.
- **Larger gusts:** Wind gust radius and knockback increase.
- **Tempest storm:** Periodic lightning storm damages and slows players within the storm radius.
- **Slowing curse:** Boss attacks apply **Slowness II**.

## Custom Moves
- **Anchor Shield:** Invulnerability tied to lightning-rod anchors.
- **Chain Lightning:** Damages nearby players unless they stand in anchor safe rings.
- **Wind Gust:** Knockback + damage blast around the boss.
- **Tempest Storm (Tempest):** Lightning storm that damages and slows within a wide radius.
- **Slowing Curse:** Boss attacks apply slowness (stronger in Tempest).

## How to Defeat the Storm Herald (Step-by-Step)
1. **Identify and destroy the anchors.** The boss is invulnerable until all lightning rods are destroyed.
2. **Use anchor safe rings.** Safe rings mitigate chain lightning and give the team breathing room.
3. **Push into Stormcaller phase.** Once all anchors fall, the boss becomes vulnerable.
4. **Manage spacing.** Stay beyond the wind gust radius when possible to reduce knockback and damage.
5. **Respect chain lightning.** Avoid clumping outside safe rings to minimize lightning hits.
6. **Prepare for Tempest.** At 30% health, expect faster cooldowns, higher damage, and the new tempest storm.
7. **Rotate defensively.** Move between safe rings and keep healing ready during Tempest storm bursts.
8. **Finish with coordinated damage.** Sustain DPS while avoiding gusts and storm strikes.
