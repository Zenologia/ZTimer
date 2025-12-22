# 🌟 ZTIMER
*Developed by Zenologia*

---

**ZTimer** is a precision-tuned, database-backed timer system built for competitive Minecraft gameplay — whether for parkour, dungeons, questlines, or race events.  
Designed for performance, flexibility, and admin control, it automatically tracks active runs, best times, and leaderboards with full PlaceholderAPI support.

---

## ⚙️ FEATURES
- ⏱️ Unlimited timer IDs (auto-sanitized)
- 🔁 Auto-stop existing timers when a new one starts
- 🧮 Millisecond-accurate best time tracking (displayed in seconds/mm:ss)
- 🏆 Per-timer leaderboards with caching
- ⚡ Fully async database operations (HikariCP for MySQL, SQLite supported)
- 🗺️ Configurable exit & fallback locations
- 💬 Live-updating placeholders (PlaceholderAPI)
- 🔒 Confirmation prompts for global resets
- 🔄 Auto-update player names and times on next join
- 💡 Tab completion for timers and selectors
- 🧰 Admin command suite for complete timer management
- 🚪 NEW: logout-commands — run configured console commands immediately when a player logs out with an active timer
- 🔁 relog-commands — run configured console commands when the player rejoins (persisted in pending_teleports.yml)

---

## 📦 REQUIREMENTS
- **Paper 1.21.10+**
- **Java 17+**
- **PlaceholderAPI** *(required for placeholders)*
- SQLite *(included)* or MySQL *(optional)*

---

## 🧩 INSTALLATION
1. Drop `ZTimer.jar` into your `/plugins` folder.
2. Start the server — config and database files will auto-generate.
3. Edit `config.yml` to define:
   - Timer exit points
   - Leaderboard sizes
   - logout-commands / relog-commands
   - Fallback spawn
   - Storage type (SQLite/MySQL)
4. Reload or restart:
   ```
   /ztimer reload
   ```
   Note: If you change storage types, restart the whole server (changing storage at runtime is unsupported).

---

## ⚙️ CONFIGURATION (logout-commands & relog-commands)

Two related command keys are supported for each timer (config locations `mazes.<id>.*` or `timers.<id>.*`):

- `logout-commands` — NEW: Run immediately when the player logs out while an active timer is running. Commands are dispatched as the server console.
- `relog-commands` — Existing behavior: persisted on logout to `pending_teleports.yml` and executed when the player rejoins.

Placeholders available in commands:
- `%player%` — player name
- `%player_uuid%` — player UUID

Example config snippet:
```yaml
mazes:
  example-maze:
    exit_location:
      world: world
      x: 0.5
      y: 65.0
      z: 0.5
      yaw: 0.0
      pitch: 0.0
    logout-commands:
      - "someplugin record-logout %player% %player_uuid%"
    relog-commands:
      - "someplugin restore-progress %player_uuid%"
```

Behavior notes:
- `logout-commands` run immediately on PlayerQuitEvent on the main thread. Keep commands lightweight or ensure the commands themselves are async-friendly to avoid blocking the server tick.
- `relog-commands` are written to `plugins/ZTimer/pending_teleports.yml` and executed when the player rejoins; they are run after teleport handling to ensure the player entity is ready.
- Both keys support placement under either `mazes.<id>` or `timers.<id>` for compatibility.

---

## ⚔️ COMMANDS

| Command | Description |
|----------|-------------|
| `/ztimer start <timerId> <selector>` | Start a timer for the given players |
| `/ztimer stop <timerId> <selector>` | Stop timer(s) and record completion |
| `/ztimer reset <timerId> <selector>` | Reset a player's best time |
| `/ztimer reset <timerId>` | Ask for confirmation to reset globally |
| `/ztimer reset <timerId> confirm` | Confirm global reset for this timer |
| `/ztimer cancel <timerId> <selector>` | Force-cancel timer(s) and teleport players |
| `/ztimer reload` | Reload config and caches |

Player command:
```
/ztimer cancel <timerId>
```
Cancels your current active timer (requires `ztimer.cancel.self` permission).

---

## 🔐 PERMISSIONS

| Node | Description |
|------|--------------|
| `ztimer.admin` | Access to all admin commands |
| `ztimer.cancel.self` | Allow players to cancel their own timers *(default: true)* |

If you maintain a `plugin.yml` or permissions guide, consider documenting `logout-commands` usage and possible side-effects from console-executed commands.

---

## 🧾 PLACEHOLDERS

All placeholders follow the format:
```
%ztimer_<placeholder>%
```

### 🔸 Active Timer
| Placeholder | Description |
|--------------|--------------|
| `%ztimer_active_<timerId>%` | Returns true/false |

### 🔸 Current Timer Data
| Placeholder | Description |
|--------------|--------------|
| `%ztimer_current_<timerId>%` | Formatted time (mm:ss) |
| `%ztimer_current_seconds_<timerId>%` | Whole seconds |

(See the source for a full list of supported placeholders by the included PlaceholderAPI expansion.)

---

## 💽 STORAGE & SCHEMA

- Default storage is SQLite (file `ztimer.db` in plugin folder).
- MySQL/MariaDB supported via HikariCP (configure `storage.type` in `config.yml`).
- Table `ztimer_best_times` stores best times and indexed by `(timer_id, best_millis)` for fast leaderboards.

Important note for SQLite:
- The plugin previously used a single shared Connection for SQLite. If you run into intermittent SQLite errors under heavy concurrency, consider using a connection-per-operation approach or switch to MySQL. See Testing_Checklist.md and Troubleshooting below.

---

## 🐞 TROUBLESHOOTING
- PlaceholderAPI missing? Plugin will disable itself on startup — install PlaceholderAPI first.
- DB errors with SQLite under load? Try MySQL or ensure your SQLite JDBC driver/connection settings allow concurrent use.
- Logout commands blocking server tick? Keep logout commands light or have them call async-capable plugin endpoints.

---

## 🧪 TESTING & QA
- See Testing_Checklist.md for a full set of manual and automated checks.
- New items added: tests for `logout-commands` execution on PlayerQuitEvent and for concurrent logout behavior.
- Pending teleports are persisted to `plugins/ZTimer/pending_teleports.yml` — verify file reads/writes in your environment.

---

## 📜 CHANGELOG & CONTRIBUTING
- Add an entry in `Changelog.md` for the `logout-commands` feature and document the new config keys.
- Contributions welcome — open issues or PRs on the repository. Please follow the coding style and include tests where applicable.

---

## 🛠️ DEVELOPMENT
- Build: `mvn clean package` (see `pom.xml`)
- Java 17
- Use MockBukkit / integration test server for behavioral tests involving Bukkit events and scheduler.

---

Thanks for using ZTimer — if you have feature requests (like additional placeholder tokens or more flexible command scheduling), open an issue and we’ll discuss priorities.
