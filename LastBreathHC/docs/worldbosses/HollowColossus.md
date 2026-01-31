# Hollow Colossus — World Boss Guide

## Overview
The **Hollow Colossus** is a Ravager-based boss that alternates between **Closed** (invulnerable) and **Exposed** (vulnerable) phases on a fixed timer. During closed phases, it spawns minion waves and arena hazards. During exposed phases, only hits to the boss’s weak spot can deal damage.

## Stats
- **Entity type:** `RAVAGER`. 
- **Arena spawn radius:** `45` blocks from the arena center. 
- **Despawn timer:** `600` seconds (10 minutes). 
- **Safe zone radius:** `3.0` blocks (configurable). 

> **Config source:** `worldBoss.bosses.HollowColossus.*` in `config.yml`.

## Phases & Mechanics
### Phase 1 — Closed (Invulnerable)
- **Duration:** 240 ticks (≈12 seconds). 
- **Minion wave:** Spawns 3 + difficulty bonus zombies each cycle.
- **Debris rain:** Falling deepslate debris rains down outside safe zones.
- **Arena collapse:** After two full phase loops, sections of the floor collapse outside safe zones.

### Phase 2 — Exposed (Weak Spot)
- **Duration:** 120 ticks (≈6 seconds).
- **Weak spot requirement:** The boss only takes damage if the attacker hits from a height ≥70% of the boss’s height.
- **Telegraph:** The phase shift is telegraphed ~3 seconds before it occurs.

## Custom Moves
- **Timed Core Cycle:** Alternates between invulnerable and vulnerable states. 
- **Weak Spot Check:** Only high-angle hits count during the exposed phase. 
- **Minion Wave:** Zombies spawn with scaling health each cycle. 
- **Debris Rain:** Falling blocks target unsafe areas. 
- **Arena Collapse:** Falling floor tiles after multiple loops.

## How to Defeat the Hollow Colossus (Step-by-Step)
1. **Recognize the cycle.** The boss alternates between 12 seconds closed and 6 seconds exposed.
2. **Survive the closed phase.** Clear zombie waves while avoiding falling debris and arena collapse zones.
3. **Use safe zones.** Safe zones are marked by particle rings and protect against debris and collapse effects.
4. **Watch the telegraph.** Phase changes are signaled ~3 seconds in advance — reposition immediately.
5. **Target the weak spot.** During the exposed window, jump, gain elevation, or strike from higher ground to land hits above the 70% height threshold.
6. **Repeat the cycle.** Each loop increases difficulty, so prioritize clean damage during exposed windows.
7. **Finish before the arena degrades.** After multiple loops, the collapsing floor makes survival harder — end the fight quickly.
