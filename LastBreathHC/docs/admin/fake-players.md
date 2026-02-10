# Fake Players: Visual Simulation Notes

`/fake` entries are **visual simulation only**.

## `/list` behavior and admin command

This implementation uses packet-level tab visuals and does **not** create real online players in Bukkit.
Because of that, vanilla `/list` only includes real connected players and will not include fake entries.

Use `/fake counts` for an explicit breakdown:
- real online players (Bukkit online count),
- active fake players,
- combined total (real + fake).


- They can appear as tab-list rows so admins can stage events, stories, or UI demonstrations.
- They are **not real network-connected players**.
- They do **not** authenticate, join the world, hold inventories, trigger login events, or create an active client session.
- Any fake-player chat or reactions are plugin-authored simulation behavior, not traffic from a real Minecraft client.

## Whisper alias compatibility (command spoofing)

The plugin now intercepts common whisper aliases (`/msg`, `/tell`, `/w`, plus common plugin DM aliases) and, when no real online player matches the target, can route that command to an active fake-player record.

This is **command compatibility spoofing only**:
- it allows sender-facing DM UX with fake names,
- it does **not** create a true authenticated player session,
- and it does **not** satisfy APIs that require a real Bukkit `Player` instance.

## Full selector and real-player API compatibility

If your server needs true support for selectors (`@a`, scoreboard selectors, etc.) or integrations that strictly depend on real `Player` objects from other plugins, that requires a separate architecture:

1. introduce a **virtual player registry** abstraction,
2. explicitly patch each dependent command/plugin integration against that registry,
3. keep this spoofed whisper bridge as best-effort compatibility, not full identity emulation.

Implementation details:

- Generic fake-player logic lives in `fakeplayer.*`.
- Version-specific internals (NMS / packet handling) are isolated in `fakeplayer.platform.v1_21_11.*`.
