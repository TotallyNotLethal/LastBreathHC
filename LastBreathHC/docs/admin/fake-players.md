# Fake Players: Visual Simulation Notes

`/fake` entries are **visual simulation only**.

- They can appear as tab-list rows so admins can stage events, stories, or UI demonstrations.
- They are **not real network-connected players**.
- They do **not** authenticate, join the world, hold inventories, trigger login events, or create an active client session.
- Any fake-player chat or reactions are plugin-authored simulation behavior, not traffic from a real Minecraft client.

Implementation detail:

- Generic fake-player logic lives in `fakeplayer.*`.
- Version-specific internals (NMS / packet handling) are isolated in `fakeplayer.platform.v1_21_11.*`.
