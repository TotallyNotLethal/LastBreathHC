# Hollow Colossus — World Boss Guide

## Overview
The **Hollow Colossus** is a Ravager-based boss that rotates through **Closed**, **Exposed**, and **Rampaging** phases on a fixed timer. Closed phases are invulnerable and spawn minions plus arena hazards. Exposed phases allow damage only through its weak spot. Rampaging phases unleash shockwaves and targeted debris before the boss seals itself again.

## Stats
- **Entity type:** `RAVAGER`.
- **Arena spawn radius:** `45` blocks from the arena center.
- **Despawn timer:** `600` seconds (10 minutes).
- **Safe zone radius:** `3.0` blocks (configurable).
- **Closed phase:** `240` ticks (≈12 seconds).
- **Exposed phase:** `120` ticks (≈6 seconds).
- **Rampaging phase:** `100` ticks (≈5 seconds).
- **Phase shift telegraph:** `60` ticks (≈3 seconds) before the swap.

> **Config source:** `worldBoss.bosses.HollowColossus.*` in `config.yml`.

## Phases & Mechanics
### Phase 1 — Closed (Invulnerable)
- **Invulnerable:** The boss ignores all damage.
- **Minion wave:** Spawns **4 + difficulty bonus** zombies with scaling health each cycle.
- **Debris rain:** Falling deepslate debris targets unsafe areas outside safe rings.
- **Arena collapse:** After two full loops, floor tiles collapse outside safe zones.

### Phase 2 — Exposed (Weak Spot)
- **Weak spot requirement:** The boss only takes damage if the attacker hits from a height ≥70% of the boss’s height.
- **Telegraph:** Phase shift is telegraphed about 3 seconds before it occurs.

### Phase 3 — Rampaging
- **Shockwave:** Periodic radial blast that damages and launches players within range.
- **Targeted debris:** Falling deepslate drops near player positions, excluding safe zones.
- **Transition:** After rampaging, the core closes and the cycle repeats with increased difficulty.

## Custom Moves
- **Timed Core Cycle:** Closed → Exposed → Rampaging rotation on fixed timers.
- **Weak Spot Check:** Only high-angle hits count during the exposed phase.
- **Minion Wave:** Zombies spawn with scaling health each loop.
- **Debris Rain:** Falling blocks target unsafe areas.
- **Arena Collapse:** Floor tiles collapse outside safe zones after multiple loops.
- **Shockwave (Rampaging):** Damage + knockback within shockwave radius.
- **Targeted Debris (Rampaging):** Drops debris near player positions.

## How to Defeat the Hollow Colossus (Step-by-Step)
1. **Recognize the cycle.** Expect a 12s closed phase, 6s exposed phase, then a 5s rampage.
2. **Survive the closed phase.** Clear zombie waves while avoiding falling debris and collapse zones.
3. **Use safe zones.** Safe zones are marked by particle rings and protect against debris and collapse effects.
4. **Watch the telegraph.** Phase changes are signaled ~3 seconds in advance — reposition immediately.
5. **Target the weak spot.** During exposed windows, jump, gain elevation, or strike from higher ground to land hits above the 70% height threshold.
6. **Brace for rampage.** Spread out to avoid shockwave knockback and watch for targeted debris.
7. **Repeat efficiently.** Each loop increases difficulty, so prioritize clean damage during exposed windows.
8. **Finish before the arena degrades.** Collapsing tiles stack up after multiple loops — end the fight quickly.
