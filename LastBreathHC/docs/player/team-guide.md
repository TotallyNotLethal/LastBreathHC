# Player Guide: Teams (`/team`)

This guide explains **everything players need** to use the Team system in LastBreathHC, in plain language.

> This is a player-focused guide (not admin setup).

---

## 1) What teams do for you

A team lets you:
- Stay grouped with friends.
- Talk in private team chat with `/t`.
- Send emergency help pings with `/tping`.
- Share a team waypoint with `/waypoint`.
- Quickly manage your roster with `/team gui`.
- Track dead teammates in the Team GUI (if they are dead/banned) and start a guidance trail to their death location.

---

## 2) Quick start (30 seconds)

1. Create a team:
   - `/team create MyTeam`
2. Have your friends join:
   - `/team join MyTeam`
3. If you want approvals first, lock the team:
   - `/team lock`
4. Open the Team menu any time:
   - `/team` or `/team gui`

---

## 3) `/team` commands (player view)

### `/team` or `/team gui`
Opens the Team Management GUI.

- If you are **not on a team**: shows all teams and lets you click to join/request.
- If you **are on a team**: shows members, owner, join status, leave/disband button, and dead teammate tracking heads.

### `/team create <name>`
Creates a team and puts you in it immediately.

Rules:
- Name cannot contain spaces.
- Name must be 16 characters or less.
- Name must not already exist.

### `/team join <name>`
Attempts to join a team.

Possible outcomes:
- **Joined instantly** (team is open).
- **Request sent** (team is locked).
- **Already pending** (you already requested).
- **Already member** (you are already in that team).

If you were already in another team, joining a new one moves you over automatically.

### `/team leave`
Leaves your current team.

If you are the owner:
- If nobody remains: team is disbanded.
- If members remain: ownership transfers to another member.

### `/team kick <player>`
Owner-only. Removes a member from your team.

Notes:
- You cannot kick yourself.
- Target must be in your team.

### `/team lock`
Owner-only. Team becomes **request-only**.

Players can still attempt `/team join`, but they must be approved.

### `/team unlock`
Owner-only. Team becomes **open join** again.

### `/team requests`
Owner-only. Shows all pending join requests.

### `/team accept <player>`
Owner-only. Approves a pending request and adds that player.

### `/team deny <player>`
Owner-only. Rejects a pending request.

---

## 4) What the Team GUI shows

When you open `/team gui`:

### If you are not in a team
- You get a list of teams.
- Each team card shows:
  - Team name
  - Member count
  - Owner name
  - Join mode (Open or Locked)
- Clicking an open team joins instantly.
- Clicking a locked team sends a request.

### If you are in a team
- Member heads appear in the top area.
- Owners can click non-owner members to kick them.
- Bottom controls include:
  - **Leave Team** (members)
  - **Disband Team** (owner)
  - Team info card (members, owner, open/locked)
- Dead teammate heads can appear in dedicated bottom slots:
  - Click one to start a personal guidance trail to their death spot.

---

## 5) Related team commands you should use

These are not `/team` subcommands, but they are core team tools.

### `/t <message>` — Team chat
Sends a private message to online members of your current team.

If you are not in a team, the message does not send.

### `/tping` — Emergency team ping
Sends a help ping to online teammates.

Ping contains:
- Your name
- World type (Overworld/Nether/The End)
- Approximate distance from each recipient

Cooldown:
- Default is 300 seconds (5 minutes), so spam is blocked.

### `/waypoint`
Shows current team waypoint with world + direction + approximate distance.

### `/waypoint set <name>`
Sets the team waypoint at your current location and broadcasts it to the team.

### `/waypoint clear`
Removes the team waypoint and broadcasts that it was cleared.

---

## 6) Common mistakes and fixes

- **"That team does not exist."**
  - Check spelling/caps and run `/team gui` to browse teams.

- **"Team names cannot contain spaces."**
  - Use `MyTeam` or `My_Team` style names.

- **"Team names must be 16 characters or fewer."**
  - Shorten the name.

- **"Only the team owner can..."**
  - Ask your owner, or have ownership transferred (owner leaves and transfer happens automatically if members remain).

- **"No pending join request for <player>."**
  - The request was already handled, expired from data changes, or never existed.

- **"Your team does not have a waypoint."**
  - Set one with `/waypoint set <name>`.

---

## 7) Pro tips for players

- Use **open join** for casual public squads.
- Use **locked join + accept/deny** for trusted runs.
- Use `/tping` only for real emergencies (it has cooldown for a reason).
- Keep waypoint names clear: `Home`, `Vault`, `Boss Portal`, `Rescue Point`.
- Use `/team gui` as your one-stop menu during fights and recovery.

