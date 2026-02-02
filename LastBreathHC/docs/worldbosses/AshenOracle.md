# Ashen Oracle — World Boss Guide

## Overview
The **Ashen Oracle** is a Wither Skeleton-based world boss that begins in a ritual shielded by crying-obsidian relics. Once the relics are destroyed, it enters a **Prophecy** phase with repeated omen blasts, and then escalates into **Cataclysm** at low health with ash bursts and burning melee attacks.

## Stats
- **Entity type:** `WITHER_SKELETON`.
- **Arena spawn radius:** `45` blocks from the arena center.
- **Despawn timer:** `600` seconds (10 minutes).
- **Ritual relic count:** `4` (configurable).
- **Ritual relic radius:** `7.0` blocks (configurable).
- **Omen safe radius:** `4.0` blocks (configurable).
- **Omen cooldown:** `160` ticks (≈8 seconds, configurable).
- **Ash burst radius:** `10.0` blocks (configurable).
- **Ash burst cooldown:** `140` ticks (≈7 seconds, configurable).
- **Cataclysm threshold:** `35%` health (configurable).

> **Config source:** `worldBoss.bosses.AshenOracle.*` in `config.yml`.

## Phases & Mechanics
### Phase 1 — Ritual (Shielded)
- **Invulnerable:** The boss cannot be damaged while any relics remain.
- **Relics:** Crying obsidian relics (`CRYING_OBSIDIAN`) spawn in a ring around the boss.
- **Relic rings:** Each relic projects a small SOUL particle ring to help locate it.

### Phase 2 — Prophecy
- **Omen pulses:** Periodic omen blasts damage and debuff players within 20 blocks, except those standing in the omen safe ring around the boss.
- **Debuffs:** Omen applies **Blindness** and **Slowness**.
- **Safe ring:** A persistent END_ROD ring around the boss indicates the omen safe radius.

### Phase 3 — Cataclysm (Low Health)
- **Trigger:** At ≤35% health.
- **Empowered omens:** Omen pulses deal higher damage and occur more frequently.
- **Ash burst:** Periodic ash explosions knock back, ignite, and damage players within range.
- **Burning melee:** Boss attacks ignite players during Cataclysm.

## Custom Moves
- **Ritual Shield:** Invulnerability tied to relics that must be destroyed.
- **Omen Pulse:** Area pulse that damages and applies Blindness + Slowness outside the safe ring.
- **Ash Burst (Cataclysm):** Fiery knockback burst that ignites and damages nearby players.
- **Burning Strikes (Cataclysm):** Boss melee hits set targets on fire.

## How to Defeat the Ashen Oracle (Step-by-Step)
1. **Break the relics.** The boss is invulnerable until all crying obsidian relics are destroyed.
2. **Watch for relic rings.** Use the SOUL particle rings to locate remaining relics quickly.
3. **Use the omen safe ring.** During Prophecy, stand inside the END_ROD ring to avoid omen damage.
4. **Spread out.** Omen pulses hit everyone in range; spacing helps healers keep up.
5. **Prepare for Cataclysm.** At 35% health, expect more frequent, stronger omens.
6. **Dodge ash bursts.** Back out of the ash burst radius when the telegraph hits to avoid knockback and fire.
7. **Manage fire damage.** Bring fire resistance or water to counter burning melee hits.
8. **Finish decisively.** Maintain DPS while rotating in and out of the safe ring to end the fight.
