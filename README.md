# LastBreathHC

LastBreathHC is a custom **Paper 1.21.x hardcore gameplay plugin** that layers progression systems, world events, social tools, custom consumables, and economy-like reward loops on top of vanilla survival.

> This README intentionally documents every major plugin system **except spectator mode**, as requested.

---

## Table of Contents
- [1) Platform & Build](#1-platform--build)
- [2) Quick Start (Server Owner)](#2-quick-start-server-owner)
- [3) Core Gameplay Systems](#3-core-gameplay-systems)
- [4) Commands](#4-commands)
- [5) Permissions](#5-permissions)
- [6) Configuration Deep Dive](#6-configuration-deep-dive)
- [7) Data Files & Persistence](#7-data-files--persistence)
- [8) Integrations](#8-integrations)
- [9) Player UX Systems (GUI / Messaging / UI)](#9-player-ux-systems-gui--messaging--ui)
- [10) Operational Notes](#10-operational-notes)
- [11) Included Design Docs](#11-included-design-docs)

---

## 1) Platform & Build

- **Server API:** Paper API `1.21.11-R0.1-SNAPSHOT`
- **Minecraft target:** `1.21`
- **Java target:** `21`
- **Plugin entrypoint:** `com.lastbreath.hc.lastBreathHC.LastBreathHC`

Build:

```bash
cd LastBreathHC
./gradlew build
```

Run dev server:

```bash
cd LastBreathHC
./gradlew runServer
```

---

## 2) Quick Start (Server Owner)

1. Build jar (`./gradlew build`).
2. Place `LastBreathHC/build/libs/LastBreathHC-1.0-SNAPSHOT.jar` into `plugins/`.
3. Start server once to generate config/data files.
4. Tune `plugins/LastBreathHC/config.yml` and other YAML resources as needed.
5. Restart or reload plugin (full restart preferred for major config changes).

Recommended first-pass setup:
- Verify Discord webhook settings.
- Verify world names (`world`, `world_boss_arena`, etc.) match your server.
- Validate world boss trigger settings and anti-cheese values.
- Validate team waypoint, tab refresh intervals, and event spawn intervals.

---

## 3) Core Gameplay Systems

### 3.1 Nemesis System (Flagship progression/combat loop)
A deep, stateful captain ecosystem inspired by nemesis-style rivalries.

Includes:
- Captain creation from eligible hostile mob encounters.
- Tiered scaling (health/damage multipliers, level-based growth).
- Captain traits, personalities, memories, political rank/promotion score, and social/loyalty data.
- Minion control and reinforcement behavior.
- Rivalry director and skirmish logic.
- Territory pressure simulation.
- Structure raids / building interactions.
- Anti-cheese monitoring.
- Rewards, XP, anger/rivalry tracking, and progression telemetry.
- Admin warband tools and captain listing UI.

Config namespace: `nemesis.*`

Companion docs:
- `LastBreathHC/docs/nemesis/nemesis-system.md`
- `LastBreathHC/docs/nemesis/nemesis-non-mission-implementation-plan.md`
- `LastBreathHC/docs/nemesis/shadow-of-war-gap-analysis.md`
- `LastBreathHC/docs/nemesis/orc-warband-schematics.md`

---

### 3.2 World Boss System
Biome-aware boss spawning with trigger sources, anti-cheese, reward tables, and optional arena/portal flow.

Key capabilities:
- Timed and triggered spawns (thunderstorm, blood moon, structure interaction).
- Named boss variants:
  - Gravewarden
  - StormHerald
  - HollowColossus
  - AshenOracle
- Per-boss mechanics/cooldowns/safe radii.
- Anti-cheese controls (distance, pillar height, ranged abuse thresholds).
- Optional world boss arena shell + portal generation.
- Reward bundles: drops, cosmetics, and title unlocks.

Config namespace: `worldBoss.*`

Boss design docs:
- `LastBreathHC/docs/worldbosses/Gravewarden.md`
- `LastBreathHC/docs/worldbosses/StormHerald.md`
- `LastBreathHC/docs/worldbosses/HollowColossus.md`
- `LastBreathHC/docs/worldbosses/AshenOracle.md`

---

### 3.3 Asteroid Event System
Randomized asteroid impacts with tier scaling, hazard waves, and loot pools.

Includes:
- Spawn windows + optional player-biased target selection.
- Tier-based combat pressure (mobs, effects, optional bosses at high tier).
- Countdown and impact logic.
- Loot tiers with material rolls, custom item chances, cosmetic rolls, and enchant pages.
- Cleanup tools for asteroid-tagged entities.

Config namespace: `asteroid.*`

---

### 3.4 Blood Moon
Global event toggling hostile pressure and event interactions.

Capabilities:
- Command-driven start/stop.
- Scheduler/manager/listener separation.
- Integration hooks with other systems (e.g., world-boss trigger path).

---

### 3.5 Teams, Team Chat, Emergency Ping, and Waypoints
A coordinated squad layer for hardcore play.

Includes:
- Team creation/join/requests/kick/lock/unlock flows.
- Team chat (`/t`).
- Emergency team ping with cooldown protection.
- Team waypoint set/clear/view + formatted team broadcasts.
- Team management GUI.

Config namespaces:
- `teamChat.*`
- `emergencyPing.*`
- `waypoint.*`

---

### 3.6 Cosmetics & Titles
Persistent vanity progression with GUI-driven equip flow.

Includes:
- Prefixes, kill messages, auras.
- Cosmetic token handling and random cosmetic reward paths.
- Daily cosmetic reward hooks.
- Title unlocking/equipping + background title support.
- Boss title landing handling.

Commands:
- `/cosmetics`
- `/titles`

---

### 3.7 Daily Rewards
Calendar/streak-based reward loop with optional auto-open behavior.

Includes:
- Reward schedule by day.
- Streak milestone rewards.
- Item/effect/cosmetic reward types.
- Daily reward GUI and join notifier listener.

Config namespace: `dailyRewards.*`

---

### 3.8 Custom Potion & Effect Framework
A large alchemy extension with branch brewing and custom side effects.

Includes:
- Registry-driven potion definitions (`potion-definitions.yml`).
- Branch paths and branch-brew compatibility graph.
- Positive + drawback + after-effect support.
- Custom effect IDs and effect metadata (`custom-effects.yml`).
- Cauldron brewing listener + potion handler.
- Effect status GUI and admin effect grant command.

Files:
- `LastBreathHC/src/main/resources/potion-definitions.yml`
- `LastBreathHC/src/main/resources/custom-effects.yml`
- `LastBreathHC/POTION_BRANCHES.md`

---

### 3.9 Custom Items, Tokens, and Recipes
Special progression items and recipe registration.

Includes:
- Rebirth Stone
- Totem of Life
- Gracestone
- Enhanced Grindstone
- Token recipes and revive token GUI recipe registration
- Event-linked item drops (asteroid/world boss/reward systems)

---

### 3.10 Hardcore Lifecycle & Death-Oriented Systems
Systems designed around risk, punishment, and recovery loops.

Includes:
- Revive state manager/listener.
- Death markers with timed duration.
- Banned-death zombie service.
- Head drops/tracking/logging.
- Aggressive logout mob handling.

Config namespaces:
- `deathMarker.*`

---

### 3.11 Mob & Combat Enhancements
Additional pressure and QoL combat rules.

Includes:
- Dynamic mob scaling by distance.
- Mob stacking and related combat/sign behavior.
- Arrow aggro behavior.
- Dispenser sword interactions.

Config namespaces:
- `arrowAggro.*`
- `environment.distanceScaling.*` (related world pressure)

---

### 3.12 Environmental Hazards & Biome Pressure
Biome-sensitive debuffs/modifiers layered onto movement and survival.

Configured zones include:
- Swamp poisoning profile.
- Desert heat profile.
- Mountain knockback amplification.
- Deep ocean swim/drowned modifiers.

Config namespace: `environment.*`

---

### 3.13 Fake Player System (Simulation Layer)
Admin-managed visual fake players (not true online Bukkit players).

Capabilities:
- Add/remove/kill/respawn fake players.
- Fake chat and fake whisper behavior.
- Startup auto-respawn of active fakes.
- Batched visual updates to avoid spikes.
- Optional fake death reaction chat behavior.
- Real/fake/combined player count reporting.

Commands:
- `/fake ...`
- `/chat <FakePlayerName> <message...>`
- `/list` (enhanced formatting/count behavior)

Docs:
- `LastBreathHC/docs/admin/fake-players.md`

---

### 3.14 Chat & Social Utilities
Includes:
- Inventory/ender chest sharing hooks in chat (`[item]`, `[ec]`, `[inv]`) via `lbshowinv` command path.
- Nickname command and permission monitor.
- Team chat command and formatting.

---

### 3.15 Tab Menu / UI Overlay
Configurable tab-list rendering with server stats.

Includes:
- Header/footer templates.
- Online/joins/deaths sections.
- Date-time line formatting with timezone control.
- Rank icon/color map.
- Refresh scheduler and model provider.

File:
- `LastBreathHC/src/main/resources/tab-menu.yml`

---

### 3.16 API + Discord Integrations
Plugin includes outbound/inbound integration scaffolding:

- Discord webhook service for event broadcasts (with dedicated asteroid webhook options).
- API client + API event listener + stats task hooks.

Config namespace:
- `discordWebhook.*`

---

## 4) Commands

> Spectator commands are intentionally omitted from this README.

Registered command set (non-spectator):

- `/daily`
- `/leaderboard`
- `/asteroid [x z|clear-mobs|cleanup|stop]`
- `/bloodmoon <start|stop>`
- `/titles [list|equip <title>|background <on|off|toggle|status>]`
- `/bounty [page]`
- `/effects [gui|list|give]`
- `/rtp`
- `/nick`
- `/discord`
- `/t <message>`
- `/tping`
- `/team <create|join|leave|kick|lock|unlock|requests|accept|deny|gui> ...`
- `/waypoint <set|clear|view> ...`
- `/worldboss <spawn|portal|enable|disable|cleanup> [type]`
- `/cosmetics`
- `/lbshowinv`
- `/fake ...`
- `/chat <FakePlayerName> <message...>`
- `/list`
- `/nemesis ...`

---

## 5) Permissions

Defined in `paper-plugin.yml`:

- `lastbreathhc.showinv` — chat inventory sharing.
- `lastbreathhc.nick` — nickname command use.
- `lastbreathhc.fake` — fake player management command.
- `lastbreathhc.fake.chat` — fake chat command.
- `lastbreathhc.nemesis` — player nemesis usage.
- `lastbreathhc.nemesis.admin` — admin nemesis controls.

---

## 6) Configuration Deep Dive

Primary config file: `LastBreathHC/src/main/resources/config.yml` (copied to plugin data folder on first run).

Top-level sections you should know:

- `fakePlayers.*` — lifecycle, batching, death chat reactions.
- `debug` — debug switch.
- `customEnchants.*` — enchant behavior/tuning.
- `discordWebhook.*` — webhook endpoints, bot identity, startup cleanup.
- `teamChat.*`, `deathMarker.*`, `emergencyPing.*`, `waypoint.*`.
- `tabMenu.*` — refresh cadence.
- `arrowAggro.*`, `potion.thermalVision.*`, `anvilCrush.*`.
- `asteroid.*` — spawn windows, tiers, mob/effect payloads, loot tables.
- `worldBoss.*` — trigger/scheduler/arena/anti-cheese/boss tuning/rewards.
- `environment.*` — biome pressure and scaling.
- `dailyRewards.*` — reward calendar and milestones.
- `nemesis.*` — creation/progression/social/political/memory/persona/territory/spawn/naming.

Recommendation: version your live `config.yml` in Git and use staged rollout for high-impact knobs (`worldBoss.antiCheese`, `nemesis.spawn`, `asteroid.spawn`).

---

## 7) Data Files & Persistence

Runtime YAML/state files include (non-exhaustive):

- `fake-players.yml` — fake player records.
- `nemesis-structures.yml` — structure footprint persistence.
- `player-placed-blocks.yml` — placement index for structure validation.
- Additional stats/progression files managed by subsystem serializers.

The plugin initializes and loads these during startup and uses periodic save/flush tasks for selected systems.

---

## 8) Integrations

### Discord webhook integration
Supports:
- Generic event posting.
- Asteroid-specific webhook channel/profile override.
- Startup cleanup behavior for asteroid message channel.

### LastBreath API integration
The codebase includes API client/event listener wiring and periodic stats task scheduling for external service communication.

---

## 9) Player UX Systems (GUI / Messaging / UI)

Available GUIs/list UIs include:
- Cosmetics GUI
- Titles GUI
- Daily Reward GUI
- Leaderboard GUI
- Team Management GUI
- Effects Status GUI
- Nemesis captain list UI

Messaging systems include:
- Rich chat formats for teams and inventory sharing.
- Broadcast/event announcements for bosses/asteroids.
- Tab menu with dynamic stats/date formatting.

---

## 10) Operational Notes

- Prefer full restarts over reloads for large config edits.
- Stress-test spawn-heavy systems in staging first:
  - Nemesis spawn director + minions
  - Asteroid mob waves
  - World boss trigger storms
- Keep webhook URLs secret in production; rotate compromised endpoints.
- Ensure `worldBoss.arena.worldName` exists if arena/portal is enabled.
- Watch server performance when reducing scheduler intervals (`tabMenu.refreshTicks`, nemesis tick periods, asteroid frequency).

---

## 11) Included Design Docs

- Nemesis docs in `LastBreathHC/docs/nemesis/`
- World boss docs in `LastBreathHC/docs/worldbosses/`
- Admin docs in `LastBreathHC/docs/admin/`
- Potion branch references in `LastBreathHC/POTION_BRANCHES.md`

If you want, I can also generate a second companion file that is purely **operator-focused** (with “safe defaults vs high-chaos defaults” for every critical setting), while still excluding spectator mode.
