# 🌟 ZTimer
*Developed by Zenologia*

A precision-tuned, storage-backed timer system built for competitive Minecraft gameplay — whether for parkour, dungeons, questlines, or race events.  
Designed for performance, flexibility, and admin control, it tracks active runs, best times, and per-timer leaderboards with full PlaceholderAPI support.

## What it does (and doesn’t) do

- ✅ Tracks per-player active timers (`start`, `stop`, `cancel`, `reset`).
- ✅ Records millisecond-accurate best times and maintains per-timer leaderboards with caching.
- ✅ Supports YAML, SQLite, and MySQL/MariaDB storage.
- ✅ Loads admin and player-facing text from a separate `messages.yml`.
- ✅ Provides PlaceholderAPI expansion with live placeholders, including a server-global active check.
- ✅ Supports configurable exit/fallback teleports plus relog- and logout-commands.
- ❌ Not a queue manager; if you use a queue plugin, configure it to handle backend kick or teleport messages appropriately.
- ❌ Does not require Floodgate or Geyser integration — placeholders and timing are independent of proxy auth.

---

## ⚙️ FEATURES
- ⏱️ Per-player active timers with millisecond tracking
- 🔁 Clear start behavior when the same timer is already active or when another timer gets replaced
- 🧪 Strict admin timer validation against `timers` entries in `config.yml`
- 🏆 Per-timer leaderboards with caching
- ⚡ Async storage updates for timer saves, resets, and name refreshes
- 🗺️ Configurable exit and fallback locations
- 💬 Separate `messages.yml` for all player and admin-facing messages
- 💡 PlaceholderAPI expansion with active, current, best, and leaderboard placeholders
- 🔒 Confirmation prompts for global resets
- 🚪 logout-commands that run immediately when a player logs out mid-run
- 🔁 relog-commands that run when a player rejoins after logging out mid-run
- 🧰 Admin command suite with tab completion for timers and selectors

---

## Requirements
- Paper 1.21.10+
- Java 17+
- PlaceholderAPI
- One storage backend:
  - YAML (default for new installs)
  - SQLite
  - MySQL or MariaDB

---

## Optional dependencies
- Any permissions plugin (for fine-grained command access)

---

## Installation

1. Drop `ZTimer.jar` into your server's `plugins/` folder.
2. Start the server once so the plugin can generate its files.
3. Edit `config.yml` to define:
   - valid timer IDs under `timers`
   - exit points
   - leaderboard sizes
   - `logout-commands` and `relog-commands`
   - fallback spawn
   - storage type
4. Edit `messages.yml` if you want to customize text output.
5. Reload or restart:
```text
/ztimer reload
```

Important notes:
- If you change `storage.type`, restart the whole server. Runtime storage switching is not supported.
- On upgrade to `v1.4.1` or newer, ZTimer automatically synchronizes `config.yml` to the current layout, writes `config_version: 2`, removes legacy `messages` keys, and creates timestamped backups before rewriting a live `config.yml` or existing `messages.yml`.
- On upgrade to `v1.4.1` or newer, `messages.yml` is synchronized from bundled defaults. Existing `messages.yml` values win, and legacy `config.yml` message values only fill any missing keys on first sync.
- Fresh installs use YAML storage by default. Existing installs keep whatever `storage.type` is already set in their live `config.yml`.

---

## Configuration (`config.yml`)

Example:
```yaml
config_version: 2

storage:
  type: yaml
  yaml:
    file: "ztimer-data.yml"
  sqlite:
    file: "ztimer.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "ztimer"
    user: "ztimer"
    password: "password"
    useSSL: false

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

debug:
  enabled: false
  log_start_stop: true
  log_db_errors: true
```

### Notes
- `config_version` is managed by the plugin and is used for one-time upgrade syncs.
- Timer IDs are defined by the keys under `timers`.
- Admin commands reject timer IDs that are not listed under `timers`, even if the same ID appears under `mazes` or `leaderboards.per_timer`.
- `mazes.<timerId>.exit_location` controls where players are teleported on cancel or relog handling.
- Legacy `mazes.<timerId>.relog-commands` and `mazes.<timerId>.logout-commands` are migrated into `timers.<timerId>` on upgrade.
- `fallback_exit_location` is only used when a timer-specific exit location is missing.

---

## Configuration (`messages.yml`)

All player and admin-facing messages now live in `messages.yml`.

Example:
```yaml
prefix: "&7[&bZTimer&7] "

shared:
  all_players: "all players"

errors:
  no_permission: "You do not have permission."
  timer_not_running: "Timer &e%timer%&7 is not running for &b%player%&7."
  invalid_timer_id: "Timer ID &e%timer%&7 is not configured under timers."
  invalid_player_selector: "No valid players found for selector &e%selector%&7."
  only_players_self_cancel: "Only players may self-cancel."

info:
  start: "Started timer &e%timer%&7 for &b%player%&7."
  start_replaced: "Started timer &e%timer%&7 for &b%player%&7. Active timer &e%previous_timer%&7 was canceled."
  timer_already_running: "Timer &e%timer%&7 is already running for &b%player%&7."
  stop: "Stopped timer &e%timer%&7 for &b%player%&7. Time: &a%time%&7."
  reset: "Reset timer &e%timer%&7 for &b%player%&7."
  cancel: "Canceled timer &e%timer%&7 for &b%player%&7."
  reload: "ZTimer configuration reloaded."
  reset_confirm: "&7This will reset all stored times for timer &e%timer%&7 for &b%selector%&7. Type &c/ztimer reset %timer% confirm&7 to confirm."
  reset_success: "Reset timer &e%timer%&7 for &b%selector%&7."

usage:
  base: "/ztimer <start|stop|reset|cancel|reload> ..."
  start: "Usage: /ztimer start <timerId> <playerSelector>"
  stop: "Usage: /ztimer stop <timerId> <playerSelector>"
  reset: "Usage: /ztimer reset <timerId> [playerSelector|confirm]"
  cancel: "Usage: /ztimer cancel <timerId> [playerSelector]"
```

---

## Commands

| Command | Description | Permission |
|---|---:|---|
| `/ztimer start <timerId> <playerSelector>` | Start a timer for target player(s) | `ztimer.admin` |
| `/ztimer stop <timerId> <playerSelector>` | Stop a timer for target player(s) | `ztimer.admin` |
| `/ztimer reset <timerId>` | Show the global reset confirmation message | `ztimer.admin` |
| `/ztimer reset <timerId> confirm` | Reset all stored times for that timer | `ztimer.admin` |
| `/ztimer reset <timerId> <playerSelector>` | Reset stored best times for target player(s) | `ztimer.admin` |
| `/ztimer cancel <timerId>` | Cancel your own active timer | `ztimer.cancel.self` |
| `/ztimer cancel <timerId> <playerSelector>` | Cancel active timers for target player(s) | `ztimer.admin` |
| `/ztimer reload` | Reload `config.yml` and `messages.yml` | `ztimer.admin` |

Tab-completion is included for subcommands, timer IDs, selectors, and online player names.

---

## Permissions

| Node | Who should get it | Effect |
|---|---:|---|
| `ztimer.admin` | Admins or staff | Full administrative access to `/ztimer` commands |
| `ztimer.cancel.self` | Players (default true) | Allows a player to cancel their own timer |

---

## Message placeholders (in-plugin messages)

Messages in `messages.yml` can use the following replacement tokens:
- `%timer%` — timer ID used in the command
- `%previous_timer%` — the timer that was replaced when a different timer starts
- `%player%` — target player name
- `%time%` — formatted elapsed time
- `%selector%` — selector text used in admin messages

---

## Placeholders (PlaceholderAPI)

ZTimer registers a PlaceholderAPI expansion under the identifier `ztimer`. Use placeholders as:

```text
%ztimer_<placeholder>%
```

Important notes on behavior:
- Many placeholders require a valid Player context and return an empty string when `Player` is null.
- `%ztimer_active_global_<timerId>%` works without a Player context.
- Leaderboard positions are 1-based.
- Formatted `current` and `best` placeholders fall back to `formatting.time_default` when no value exists.
- For consistency, use timer IDs that are defined under `timers` in `config.yml`.

Complete list of placeholders

1) Active check
- `%ztimer_active_<timerId>%`
  - Per-player check. Returns `true` if the player currently has an active timer for `<timerId>`, otherwise `false`.
- `%ztimer_active_global_<timerId>%`
  - Global check. Returns `true` if any player on the server currently has an active timer for `<timerId>`, otherwise `false`.

2) Current timer
- `%ztimer_current_<timerId>%`
  - Returns the current elapsed time formatted with `formatting.time_pattern`, or `formatting.time_default` if no active timer exists.
- `%ztimer_current_seconds_<timerId>%`
  - Returns whole seconds of the current elapsed time, or an empty string when unavailable.
- `%ztimer_current_millis_<timerId>%`
  - Returns elapsed milliseconds, or an empty string when unavailable.

3) Best time
- `%ztimer_best_<timerId>%`
  - Returns the player's best time formatted with `formatting.time_pattern`, or `formatting.time_default` if none exists.
- `%ztimer_best_seconds_<timerId>%`
  - Returns whole seconds of the best time, or an empty string when unavailable.
- `%ztimer_best_millis_<timerId>%`
  - Returns the best time in milliseconds, or an empty string when unavailable.

4) Leaderboard / top entries
- `%ztimer_top_<position>_<timerId>_name%`
  - Returns the player name at that leaderboard position.
- `%ztimer_top_<position>_<timerId>_time%`
  - Returns the formatted time at that leaderboard position.

Examples:
- `%ztimer_active_global_maze1%`
- `%ztimer_current_maze1%`
- `%ztimer_best_maze1%`
- `%ztimer_top_1_maze1_name%`
- `%ztimer_top_1_maze1_time%`

---

## Common scenarios

**Show a server-wide "maze1 active" indicator in a scoreboard:**  
```text
%ztimer_active_global_maze1%
```

**Show a player's current run time on a sidebar or HUD:**  
```text
%ztimer_current_maze1%
```

**Verify leaderboard first place for a timer:**  
```text
%ztimer_top_1_maze1_name%
%ztimer_top_1_maze1_time%
```

---

## Troubleshooting

- Invalid timer ID on admin commands: make sure the timer exists under `timers` in `config.yml`.
- Placeholder expansion returns an empty string: many placeholders require a Player context.
- Messages are not updating: edit `messages.yml`, then run `/ztimer reload`.
- Storage type changes are not taking effect: changing `storage.type` requires a full server restart.
- Logout or relog commands are not running: make sure they are configured under `timers.<id>` and are valid console commands.

---

## Testing

For a full admin-friendly verification pass, see [TESTING.md](./TESTING.md).

---

## Uninstall

1. Remove the JAR from `plugins/`.
2. Delete `plugins/ZTimer/` if you no longer need the configs or data.
3. Restart the server.

---

## 🧑‍💻 Author

- **Zenologia**
- [GitHub Repository](https://github.com/Zenologia/ZTimer)
- [License](https://github.com/Zenologia/ZTimer/blob/main/LICENSE)
