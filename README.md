# 🌟 ZTimer
*Developed by Zenologia*

---

**ZTimer** is a precision-tuned, database-backed timer system built for competitive Minecraft gameplay — whether for parkour, dungeons, questlines, or race events.  
Designed for performance, flexibility, and admin control, it automatically tracks active runs, best times, and per-timer leaderboards with full PlaceholderAPI support.

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

## 📦 REQUIREMENTS
- Paper 1.21.10+
- Java 17+
- PlaceholderAPI (required for placeholders)
- SQLite (included) or MySQL (optional; configure in `config.yml`)

---

## 🧩 INSTALLATION
1. Drop `ZTimer.jar` into your `/plugins` folder.
2. Start the server — config and database files will auto-generate.
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

## 🔧 CONFIG HIGHLIGHTS
- Timers may be defined under either `mazes.<id>` or `timers.<id>` in `config.yml`.
- `logout-commands` are executed immediately on PlayerQuitEvent (console sender). Use them with care — keep commands lightweight to avoid blocking the main thread.
- `relog-commands` are persisted in `plugins/ZTimer/pending_teleports.yml` and run on the player's next join.

Placeholders supported in `logout-commands` / `relog-commands`:
- `%player%` — player name
- `%player_uuid%` — player UUID

Example snippet:
```yaml
timers:
  maze1:
    relog-commands:
      - "someplugin restore-progress %player_uuid%"
    logout-commands:
      - "someplugin record-logout %player% %player_uuid%"
```

---

## 🧾 MESSAGES PLACEHOLDERS (IN-plugin messages)
Messages in `config.yml` use variable tokens that the plugin replaces when sending messages to players/admins:
- `%timer%` — timer ID used in the command
- `%player%` — target player name
- `%time%` — formatted time for start/stop messages (format controlled by `formatting.time_pattern`)
- `%selector%` — the selector string used in admin commands (e.g., `@a`)

---

## 🧾 PLACEHOLDERS (PlaceholderAPI)
ZTimer registers a PlaceholderAPI expansion under the identifier `ztimer`. Use placeholders as:

`%ztimer_<placeholder>%`

All placeholders are implemented in the PlaceholderAPI expansion (see source: ZTimerExpansion.java).

Important notes on behavior:
- Placeholder evaluation requires a valid Player context (most placeholders return an empty string if the player is null).
- Timer IDs are normalized by the plugin (auto-sanitized). Use the IDs as defined in your `config.yml` or try simple alphanumeric variants; invalid/unknown timer IDs may return empty strings or defaults as noted below.
- For leaderboard (`top_...`) placeholders position is 1-based (1 = top entry).
- Many placeholders return an empty string when no value exists (e.g., player has no current run or has no recorded best time). The formatted `current` and `best` variants fall back to the configured `formatting.time_default` string.

Complete list of placeholders:

1) Active check
- Placeholder: `%ztimer_active_<timerId>%`
- Returns: `true` if the specified player currently has an active timer for `<timerId>`, otherwise `false`.
- Example: `%ztimer_active_maze1%` → `true` or `false`
- Notes: If the timerId is invalid (cannot be normalized) the expansion returns `false`.

2) Current timer
- Placeholder: `%ztimer_current_<timerId>%`
  - Returns the current elapsed time for the player's active run on `<timerId>` formatted according to `formatting.time_pattern` (e.g., `mm:ss`), or the `formatting.time_default` if no active timer.
  - Example: `%ztimer_current_maze1%` → `01:23` or `-` (if no active timer)
- Placeholder: `%ztimer_current_seconds_<timerId>%`
  - Returns whole seconds (integer) of the current elapsed time, or empty string when unavailable.
  - Example: `%ztimer_current_seconds_maze1%` → `83`
- Placeholder: `%ztimer_current_millis_<timerId>%`
  - Returns elapsed milliseconds (integer), or empty string when unavailable.
  - Example: `%ztimer_current_millis_maze1%` → `83000`

3) Best time (player's personal best for that timer)
- Placeholder: `%ztimer_best_<timerId>%`
  - Returns the player's best time formatted according to `formatting.time_pattern`, or the `formatting.time_default` if no best exists.
  - Example: `%ztimer_best_maze1%` → `00:59` or `-`
- Placeholder: `%ztimer_best_seconds_<timerId>%`
  - Returns whole seconds for the best time, or empty string when unavailable.
  - Example: `%ztimer_best_seconds_maze1%` → `59`
- Placeholder: `%ztimer_best_millis_<timerId>%`
  - Returns milliseconds for the best time, or empty string when unavailable.
  - Example: `%ztimer_best_millis_maze1%` → `59000`

4) Leaderboard / top entries
- Placeholders follow the pattern:
  - `%ztimer_top_<position>_<timerId>_name%` — player's name at that position
  - `%ztimer_top_<position>_<timerId>_time%` — formatted time (using time_pattern)
  - `%ztimer_top_<position>_<timerId>_seconds%` — seconds (integer)
  - `%ztimer_top_<position>_<timerId>_millis%` — milliseconds (integer)
- Position is 1-based. If the requested position is out of range or the timerId is invalid, these placeholders return an empty string.
- Examples:
  - `%ztimer_top_1_maze1_name%` → `SomePlayer`
  - `%ztimer_top_1_maze1_time%` → `00:45`
  - `%ztimer_top_3_maze1_seconds%` → `57`

Behavior summary for edge-cases:
- Player argument null (no Player context): Placeholder returns empty string (general case).
- Invalid timer ID (cannot be normalized): many placeholders return empty string; `active_` returns `false`; formatted `current`/`best` default to `formatting.time_default`.
- No active run / no best time / leaderboard position out of range: placeholders return empty string or the configured default for formatted current/best.

---

## ⌨️ COMMANDS (quick)
Admin commands (see full README):
- `/ztimer start <timerId> <selector>`
- `/ztimer stop <timerId> <selector>`
- `/ztimer reset <timerId> <selector>`
- `/ztimer reset <timerId>` (ask for global confirmation)
- `/ztimer reset <timerId> confirm` (confirm global reset)
- `/ztimer cancel <timerId> <selector>`
- `/ztimer reload` (reload config & caches)

Player command:
- `/ztimer cancel <timerId>` — cancels your current active timer (requires `ztimer.cancel.self`)

Permissions:
- `ztimer.admin` — admin control (default: op)
- `ztimer.cancel.self` — allow players to cancel their own timers (default: true)

---

## 💽 STORAGE & SCHEMA
- Default storage: SQLite file `ztimer.db`
- MySQL supported via HikariCP (configure under `storage` in `config.yml`)
- Leaderboards and best times saved in DB tables (see source for schema details)

---

## 🐞 TROUBLESHOOTING
- PlaceholderAPI missing? Plugin will disable itself at startup — install PlaceholderAPI first.
- DB concurrency issues on SQLite in high-load setups? Consider switching to MySQL.
- `logout-commands` run synchronously on PlayerQuitEvent — keep them lightweight or have the target plugin accept async-friendly requests.

---

## 🧪 TESTING & QA
- Verify `logout-commands` and `relog-commands` placeholders (`%player%`, `%player_uuid%`) expand correctly.
- Check all PlaceholderAPI tokens in your HUD/scoreboard/leaderboards to ensure they're populated in your runtime context (some placeholders require the player object).

---

## 🔗 SOURCE (placeholder expansion)
The PlaceholderAPI expansion implementation that defines above tokens is available here:
- [ZTimerExpansion.java](https://github.com/Zenologia/ZTimer/blob/93979ab582b736c9e10d33305deaeefd0edca75e/src/main/java/com/zenologia/ztimer/placeholder/ZTimerExpansion.java)

---

## 🛠️ DEVELOPMENT
- Build: `mvn clean package` (see `pom.xml`)
- Java 17
- Use MockBukkit / integration test server for behavior tests involving Bukkit events and scheduler.

---
