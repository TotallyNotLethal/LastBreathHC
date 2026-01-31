# Gravewarden — World Boss Guide

## Overview
The **Gravewarden** is a Warden-based world boss that begins the fight fully shielded by summoned gravestones. While shielded, it cannot be damaged and will repeatedly pulse its barrier. Once every gravestone is destroyed, the boss becomes unsealed, gains aggressive life-steal attacks, and periodically calls in skeletal archers to pressure players outside the gravestone safe zones.

## Stats
- **Entity type:** `WARDEN`. 
- **Arena spawn radius:** `40` blocks from the arena center. 
- **Despawn timer:** `600` seconds (10 minutes). 
- **Gravestone safe radius:** `3.0` blocks (configurable). 

> **Config source:** `worldBoss.bosses.Gravewarden.*` in `config.yml`.

## Phases & Mechanics
### Phase 1 — Shielded
- **Invulnerable:** The Gravewarden cannot take damage while any gravestones remain. 
- **Gravestones:** Five gravestones (`CHISELED_DEEPSLATE`) spawn in a ring around the boss. 
- **Shield pulses:** Regular particle and sound pulses telegraph that the shield is still active.
- **Safe rings:** Each gravestone projects a safe ring (SOUL particles) based on the gravestone safe radius.

### Phase 2 — Unsealed
- **Vulnerability:** The shield drops once all gravestones are destroyed.
- **Life-steal:** The boss heals for 10% of the damage it deals. 
- **Wither strikes:** Attacks apply **Wither** for a short duration. 
- **Archer summons:** Every 10 seconds, the boss telegraphs and spawns two skeletal archers that target players outside the gravestone safe rings.

## Custom Moves
- **Gravestone Shield:** Invulnerable phase with gravestones that must be destroyed. 
- **Soul Pulse:** Shield pulse effects while shielded. 
- **Archer Summons:** Telegraphed spawn of skeleton archers during the unsealed phase. 
- **Life-Steal & Wither:** Boss attacks heal it and inflict Wither.

## How to Defeat the Gravewarden (Step-by-Step)
1. **Locate the gravestones.** The boss spawns five gravestones around the arena. These are the only breakable mechanic blocks during this phase. 
2. **Focus all gravestones first.** The Gravewarden remains invulnerable until all gravestones are destroyed.
3. **Use safe rings for cover.** Gravestones project safe rings — use them to avoid archer pressure during the shield phase. 
4. **Prepare for the unsealed phase.** Once the shield drops, the boss becomes damageable and gains life-steal.
5. **Keep pressure high.** Because it heals on hit, sustained damage is key to prevent the boss from prolonging the fight.
6. **Handle archers quickly.** Skeletal archers will spawn every ~10 seconds — clear them or kite them to avoid extra damage while focusing the boss.
7. **Finish the fight.** Continue damaging the boss while avoiding Wither and maintaining control of the arena.
