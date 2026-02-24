# Nemesis Implementation Plan (No Mission System)

This plan implements everything from `shadow-of-war-gap-analysis.md` **except autonomous mission ticks**.

## Scope constraints

- ✅ In scope: army hierarchy, social relationships, scars/returns, persona depth, territory pressure, player influence tools, observability.
- ❌ Out of scope: mission scheduler and mission simulation (duels/executions/raids).

---

## Phase 1 — Data model + backward-compatible persistence

### Goals
- Extend captain persistence to support political/social storytelling.
- Keep existing saves loadable without migration breakage.

### Changes
1. Extend `CaptainRecord` with optional sections:
   - `Political(rank, region, seatId, promotionScore, influence)`
   - `Social(loyalty, fear, ambition, confidence)`
   - `Relationships(allies, rivals, bodyguardOf, bloodBrotherOf)`
   - `Memory(lastDefeatCause, scars, humiliations, notablePlayers, callbackLinesSeed)`
   - `Persona(archetype, temperament, quirkTags, voicePackId)`
2. Update serializer/registry defaults so absent fields are treated as sane defaults.
3. Add config defaults under `nemesis.social`, `nemesis.political`, `nemesis.memory`, `nemesis.persona`, `nemesis.territory`.

### Acceptance criteria
- Existing captain data loads without errors.
- New captains get initialized optional sections.
- `/nemesis debug dump <id>` prints new sections when present.

---

## Phase 2 — Army graph + rank system

### Goals
- Move from standalone captains to a visible hierarchy.
- Provide player/admin readability for who leads whom.

### Changes
1. Add ranks: `CAPTAIN`, `WARCHIEF`, `OVERLORD`.
2. Add army graph index service (in-memory + persisted edges):
   - bodyguard assignments
   - rivalry links
   - blood-brother links
3. Add `/nemesis army` command and read-only GUI:
   - region tabs/world grouping
   - rank slots and vacancies
   - quick captain inspect from slot click
4. Promotion logic (non-mission based):
   - promotion score from kills, survivals, rivalry pressure, player deaths caused.
   - periodic evaluator upgrades/downgrades based on thresholds.

### Acceptance criteria
- Graph survives restart.
- Promotions happen through evaluator tick.
- Players can view hierarchy from `/nemesis army`.

---

## Phase 3 — Scar and return continuity

### Goals
- Make recurring captains feel like remembered characters.

### Changes
1. Capture defeat signature from killer context:
   - fire, explosion, projectile, melee, fall, magic, environmental.
2. On defy-death return:
   - add scar tag
   - mutate one weakness/immunity or behavior tendency
   - update epithet/title from scar template pool
   - generate unique return line with callback memory
3. Add anti-repeat guard to avoid identical scar mutation repeating too often.

### Acceptance criteria
- Returning captains show changed identity/behavior.
- `/nemesis info` exposes scar history.
- Encounter text references prior defeat cause when available.

---

## Phase 4 — Loyalty, betrayal, and social drama (without missions)

### Goals
- Create emergent drama from relationship shifts during live encounters.

### Changes
1. Loyalty model:
   - loyalty rises from victories/leader proximity
   - loyalty drops from humiliations, failed retreats, ally deaths
2. Betrayal triggers during combat events:
   - low loyalty + high ambition + pressure threshold
   - bodyguard may defect, flee, or turn hostile to leader
3. Revenge hooks:
   - blood-brother death increases aggression and targeted taunts
4. Event announcements:
   - concise chat narrative for betrayals/revenge turns

### Acceptance criteria
- Betrayal can occur during real encounters.
- Relationship edges update after betrayal/revenge.
- Narrative events are rate-limited and readable.

---

## Phase 5 — Persona + dialogue expansion

### Goals
- Improve identity and replayability through distinct voices.

### Changes
1. Add persona taxonomy:
   - archetypes (Berserker, Trickster, Tyrant, Fanatic, etc.)
   - temperaments (cowardly, proud, sadistic, honorable)
   - quirk tags (pyromaniac, collector, duelist)
2. Build contextual taunt engine:
   - encounter openers
   - revenge callbacks
   - low-health taunts
   - kill/escape lines
3. Add repetition controls:
   - per-player line cooldown
   - weighted rotation and recent-history suppression

### Acceptance criteria
- Captains produce context-aware lines, not generic repeats.
- Distinct-line diversity per session increases measurably.

---

## Phase 6 — Territory pressure + player influence tools

### Goals
- Make world state evolve in understandable ways without mission simulation.

### Changes
1. Territory pressure meter per region:
   - gains from active captain presence, recent player deaths, captain wins
   - losses from captain kills, player-held objectives, cleansing actions
2. Threshold effects:
   - high pressure boosts captain spawn quality/chance
   - low pressure suppresses elite rank emergence
3. Player influence mechanics (token/utilities):
   - `Intel Token`: reveals hidden relationships/scars
   - `Bribe Token`: temporary loyalty shift attempt
   - `Provocation Sigil`: forces rivalry hostility spike between linked captains
4. UI/commands:
   - `/nemesis territory` summary
   - integrate pressure and influence status into tab/info views

### Acceptance criteria
- Territory values shift over time and from player actions.
- Influence items have visible and explainable outcomes.

---

## Cross-cutting implementation checklist

- Telemetry counters for: promotions, betrayals, returns, scar types, loyalty shifts, pressure shifts.
- Config toggles and rollout flags per subsystem (`enabled: true/false`).
- Fallback behavior when optional sections are absent.
- Admin safety controls in `/nemesis debug` to inspect/reset specific subsystems.

---

## Suggested sequence (6 weeks, no mission system)

- **Week 1:** Phase 1 (schema/persistence).
- **Week 2:** Phase 2 (army graph + read-only UI).
- **Week 3:** Phase 3 (scar/return pipeline).
- **Week 4:** Phase 4 (loyalty/betrayal/revenge hooks).
- **Week 5:** Phase 5 (persona/dialogue system).
- **Week 6:** Phase 6 (territory pressure + influence items), then tuning.

---

## Definition of done

The system should feel alive when players can clearly observe:
1. captains moving through ranks and relationships,
2. recurring enemies returning changed by history,
3. social turns (betrayal/revenge) altering fights,
4. region pressure reacting to player success/failure,
5. varied personality-driven dialogue that references prior events.

