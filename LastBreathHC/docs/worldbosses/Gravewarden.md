# Gravewarden — World Boss Guide

## Overview
The **Gravewarden** is a Warden-based world boss that begins fully shielded by summoned gravestones. While shielded, it cannot be damaged and will repeatedly pulse its barrier. Once every gravestone is destroyed, the boss becomes unsealed, gains life-steal attacks, summons skeletal archers, and enters a **Revenant** phase at low health that adds wraith strikes and stronger debuffs.

## Stats
- **Entity type:** `WARDEN`.
- **Arena spawn radius:** `40` blocks from the arena center.
- **Despawn timer:** `600` seconds (10 minutes).
- **Gravestone safe radius:** `3.0` blocks (configurable).
- **Gravestone count:** `6` (configurable).
- **Archer summon cooldown:** `160` ticks (≈8 seconds, configurable).
- **Wraith strike cooldown:** `140` ticks (≈7 seconds, configurable).
- **Revenant threshold:** `35%` health (configurable).

> **Config source:** `worldBoss.bosses.Gravewarden.*` in `config.yml`.

## Phases & Mechanics
### Phase 1 — Shielded
- **Invulnerable:** The Gravewarden cannot take damage while any gravestones remain.
- **Gravestones:** Six gravestones (`CHISELED_DEEPSLATE`) spawn in a ring around the boss.
- **Shield pulses:** Regular particle/sound pulses telegraph that the shield is still active.
- **Safe rings:** Each gravestone projects a safe ring (SOUL particles) based on the gravestone safe radius.

### Phase 2 — Unsealed
- **Vulnerability:** The shield drops once all gravestones are destroyed.
- **Life-steal:** The boss heals for **10%** of the damage it deals.
- **Wither strikes:** Attacks apply **Wither** for a short duration.
- **Archer summons:** Every ~8 seconds the boss telegraphs and spawns **3** skeletal archers that prioritize players outside gravestone safe rings.

### Phase 3 — Revenant (Low Health)
- **Trigger:** At ≤35% health.
- **More pressure:** Archer summons add **+1** extra archer and occur more frequently.
- **Stronger debuffs:** Wither is applied at a higher amplifier on boss attacks.
- **Wraith strike:** Periodic soul strike centered on the nearest player deals AoE damage and applies **Weakness**.

## Custom Moves
- **Gravestone Shield:** Invulnerable phase with gravestones that must be destroyed.
- **Soul Pulse:** Shield pulse effects while shielded.
- **Archer Summons:** Telegraphed spawn of skeleton archers that focus players outside safe rings.
- **Life-Steal & Wither:** Boss attacks heal it and inflict Wither.
- **Wraith Strike (Revenant):** AoE soul strike that damages and weakens nearby players.

## How to Defeat the Gravewarden (Step-by-Step)
1. **Locate and destroy gravestones.** The boss is invulnerable until all six gravestones are broken.
2. **Use safe rings for cover.** Gravestones project safe rings — use them to avoid archer focus while shielded.
3. **Prepare for the unsealed phase.** Once the shield drops, the boss becomes damageable and gains life-steal.
4. **Keep pressure high.** Sustained damage reduces the impact of its healing on hit.
5. **Clear archers fast.** Archers spawn roughly every 8 seconds; remove them or kite them away from the group.
6. **Brace for Revenant.** At 35% health, expect more archers, stronger Wither, and periodic wraith strikes.
7. **Finish cleanly.** Stay out of wraith strike AoE, keep DPS steady, and end the fight before the healing snowballs.
