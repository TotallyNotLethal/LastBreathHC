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

Implementation details:

- Generic fake-player logic lives in `fakeplayer.*`.
- Version-specific internals (NMS / packet handling) are isolated in `fakeplayer.platform.v1_21_11.*`.
