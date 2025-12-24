# 🌟 ZTimer
*Developed by Zenologia*

A precision-tuned, database-backed timer system built for competitive Minecraft gameplay — whether for parkour, dungeons, questlines, or race events.  
Designed for performance, flexibility, and admin control, it automatically tracks active runs, best times, and per-timer leaderboards with full PlaceholderAPI support.

## What it does (and doesn’t) do

- ✅ Tracks per-player active timers (start/stop/cancel/reset).
- ✅ Records millisecond-accurate best times and maintains per-timer leaderboards with caching.
- ✅ Runs async DB writes (HikariCP for MySQL; SQLite supported).
- ✅ Provides PlaceholderAPI expansion with live placeholders (per-player and a server-global active check).
- ✅ Supports configurable exit/fallback teleports, relog- and logout-commands.
- ❌ Not a queue manager; if you use a queue plugin, configure it to handle backend kick/teleport messages appropriately.
- ❌ Does not require Floodgate/Geyser integration — placeholders and timing are independent of proxy auth.

---

## ⚙️ FEATURES
- ⏱️ Unlimited timer IDs (auto-sanitized)
- 🔁 Auto-stop existing timers when a new one starts
- 🧮 Millisecond-accurate best time tracking (displayed in configured format)
- 🏆 Per-timer leaderboards with caching
- ⚡ Fully async database operations (HikariCP for MySQL; SQLite supported)
- 🗺️ Configurable exit & fallback locations
- 💬 Live-updating placeholders (PlaceholderAPI)
- 🔒 Confirmation prompts for global resets
- 🔄 Auto-update player names and times on next join
- 💡 Tab completion for timers and selectors
- 🧰 Admin command suite for complete timer management
- 🚪 logout-commands — run configured console commands immediately when a player logs out with an active timer
- 🔁 relog-commands — run configured console commands when the player rejoins (persisted to pending_teleports.yml)

---

## Requirements
- Paper 1.21.10+
- Java 17+
- PlaceholderAPI (required for placeholders)
- SQLite (included) or MySQL (optional; configure in `config.yml`)

---

## Optional dependencies
- Any permissions plugin (for fine-grained command access)

---

## Installation

1. Drop `ZTimer.jar` into your server's `plugins/` folder.
2. Start the server once — config and database files will auto-generate.
3. Edit `config.yml` to define:
   - Timer exit points
   - Leaderboard sizes
   - `logout-commands` / `relog-commands`
   - Fallback spawn
   - Storage type (SQLite/MySQL)
4. Reload or restart:
```
/ztimer reload
```
Note: If you change storage types, restart the whole server (changing storage at runtime is unsupported).

---

## Configuration (`config.yml`)

Example (representative excerpt):
```yaml
storage:
  type: sqlite
  sqlite:
    file: "ztimer.db"

leaderboards:
  global_top_n_default: 5
  per_timer:
    maze1: 3

mazes:
  maze1:
    exit_location:
      world: "world"
      x: 100.5
      y: 64.0
      z: 200.5
      yaw: 0.0
      pitch: 0.0

timers:
  maze1:
    relog-commands:
      - "say RELOG %player% %player_uuid%"
    logout-commands:
      - "say LOGOUT %player% %player_uuid%"

fallback_exit_location:
  enabled: false
  world: "world"
  x: 0.5
  y: 64.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

formatting:
  time_default: "-"
  time_pattern: "mm:ss"

messages:
  prefix: "&7[&bZTimer&7] "
  errors:
    no_permission: "You do not have permission."
    timer_not_running: "Timer &e%timer%&7 is not running for &b%player%&7."
    invalid_timer_id: "Timer ID &e%timer%&7 is invalid."
    invalid_player_selector: "No valid players found for selector &e%selector%&7."
  info:
    start: "Started timer &e%timer%&7 for &b%player%&7."
    stop: "Stopped timer &e%timer%&7 for &b%player%&7. Time: &a%time%&7."
    reset: "Reset timer &e%timer%&7 for &b%player%&7."
    cancel: "Canceled timer &e%timer%&7 for &b%player%&7."
    reload: "ZTimer configuration reloaded."
    reset_confirm: "&7This will reset all stored times for timer &e%timer%&7 for &e%selector%&7. Type &c/ztimer reset %timer% confirm&7 to confirm."
    reset_success: "&7Reset timer &e%timer%&7 for &e%selector%&7."

debug:
  enabled: false
  log_start_stop: true
  log_db_errors: true
```

### Tips
- Timer IDs are normalized by the plugin (auto-sanitized). Use the IDs you define in `config.yml` (e.g., `maze1`) or simple alphanumeric variants.
- Keep logout/relog commands lightweight (logout-commands run immediately on PlayerQuitEvent).
- Use `%player%` and `%player_uuid%` inside relog/logout commands — the plugin substitutes them before dispatching as the console.

---

## Commands

| Command | Description | Permission |
|---|---:|---|
| `/ztimer start <timerId> <playerSelector>` | Start a timer for a target player(s) | `ztimer.admin` |
| `/ztimer stop <timerId> <playerSelector>` | Stop a timer for a target player(s) | `ztimer.admin` |
| `/ztimer reset <timerId> <playerSelector>` | Reset stored best times for selector (requires confirm) | `ztimer.admin` |
| `/ztimer cancel <timerId> <playerSelector>` | Cancel an active timer and teleport to exit | `ztimer.admin` |
| `/ztimer reload` | Reload the plugin configuration | `ztimer.admin` |

Tab-completion is included for timers and selectors.

---

## Permissions

| Node | Who should get it | Effect |
|---|---:|---|
| `ztimer.admin` | Admins/staff | Full administrative access to `/ztimer` commands |
| `ztimer.cancel.self` | Players (default true) | Allows a player to cancel their own timer |

---

## Message placeholders (in-plugin messages)
Messages in `config.yml` use variable tokens that the plugin replaces when sending messages:
- `%timer%` — timer ID used in the command
- `%player%` — target player name
- `%time%` — formatted time for start/stop messages (format controlled by `formatting.time_pattern`)
- `%selector%` — the selector string used in admin commands (e.g., `@a`)

---

## Placeholders (PlaceholderAPI)
ZTimer registers a PlaceholderAPI expansion under the identifier `ztimer`. Use placeholders as:

```
%ztimer_<placeholder>%
```

All placeholders are implemented in the PlaceholderAPI expansion (see source: ZTimerExpansion.java).

Important notes on behavior:
- Many placeholders require a valid Player context — they return an empty string when `Player` is null. Some global placeholders work without a Player (see below).
- Timer IDs are normalized by the plugin. Invalid/unknown timer IDs may return empty strings or defaults.
- Leaderboard (`top_...`) positions are 1-based (1 = top entry).
- Formatted `current` and `best` fall back to `formatting.time_default` when no value exists.

Complete list of placeholders

1) Active check
- `%ztimer_active_<timerId>%`  
  - Per-player check. Returns `true` if the player viewing the placeholder currently has an active timer for `<timerId>`, otherwise `false`.  
  - Example: `%ztimer_active_maze1%` → `true` or `false`  
  - Requires a Player context. If the timerId is invalid the expansion returns `false`.

- `%ztimer_active_global_<timerId>%`  
  - Global server-level check (new). Returns `true` if any player on the server currently has an active timer for `<timerId>`, otherwise `false`.  
  - Example: `%ztimer_active_global_maze1%` → `true` or `false`  
  - Does NOT require a Player context — useful for server-wide displays or scoreboard lines.

2) Current timer
- `%ztimer_current_<timerId>%`  
  - Returns the current elapsed time for the player's active run on `<timerId>` formatted according to `formatting.time_pattern`, or `formatting.time_default` if no active timer.  
  - Example: `%ztimer_current_maze1%` → `01:23` or `-`

- `%ztimer_current_seconds_<timerId>%`  
  - Returns whole seconds (integer) of the current elapsed time, or empty string when unavailable.  
  - Example: `%ztimer_current_seconds_maze1%` → `83`

- `%ztimer_current_millis_<timerId>%`  
  - Returns elapsed milliseconds (integer), or empty string when unavailable.  
  - Example: `%ztimer_current_millis_maze1%` → `83000`

3) Best time (player's personal best for that timer)
- `%ztimer_best_<timerId>%`  
  - Returns the player's best time formatted according to `formatting.time_pattern`, or `formatting.time_default` if no best exists.  
  - Example: `%ztimer_best_maze1%` → `00:59` or `-`

- `%ztimer_best_seconds_<timerId>%`  
  - Returns whole seconds for the best time, or empty string when unavailable.  
  - Example: `%ztimer_best_seconds_maze1%` → `59`

- `%ztimer_best_millis_<timerId>%`  
  - Returns milliseconds for the best time, or empty string when unavailable.  
  - Example: `%ztimer_best_millis_maze1%` → `59000`

4) Leaderboard / top entries
- Pattern:
  - `%ztimer_top_<position>_<timerId>_name%` — player's name at that position
  - `%ztimer_top_<position>_<timerId>_time%` — formatted time (using `time_pattern`)
  - Example: `%ztimer_top_1_maze1_name%` → `SomePlayer`  
  - Example: `%ztimer_top_1_maze1_time%` → `00:45`

---

## Common scenarios

**Show a server-wide "maze1 active" indicator in a scoreboard:**  
Use the global placeholder in a scoreboard line:
```
%ztimer_active_global_maze1%
```
This returns `true` if any player is currently running `maze1`.

**Show a player's personal current time on a HUD or sidebar:**  
```
%ztimer_current_maze1%
```
Will format and display their current elapsed time (or `-` if not running).

---

## Troubleshooting

- Placeholder expansion returns empty string: many placeholders require a Player context. Use `%ztimer_active_global_<timerId>%` for server-global checks.
- Invalid timer IDs: Timer IDs are normalized; use IDs defined in `config.yml` or simple alphanumeric variants.
- DB errors: Enable `debug.log_db_errors` in `config.yml` to surface DB exceptions in the server log.
- Logout/relog commands not running: Ensure `logout-commands` are configured for the timer and that commands are valid console commands. Relog commands are persisted and run when the player rejoins.

---

## Uninstall

1. Remove the JAR from `plugins/`.
2. (Optional) Delete `plugins/ZTimer/` if you don’t need the data/config anymore.
3. Restart the server.

---

## 🧑‍💻 Author

- **Zenologia**
- [GitHub Repository](https://github.com/Zenologia/ZTimer)
- [License](https://github.com/Zenologia/ZTimer/blob/main/LICENSE)
