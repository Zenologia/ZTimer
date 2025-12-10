# 🌟 ZTIMER
*Developed by Zenologia*

---

**ZTimer** is a precision-tuned, database-backed timer system built for competitive Minecraft gameplay — whether for parkour, dungeons, questlines, or race events.  
Designed for performance, flexibility, and admin control, it automatically tracks active runs, best times, and leaderboards with full PlaceholderAPI support.

---

## ⚙️ FEATURES
- ⏱️ Unlimited timer IDs (auto-sanitized)
- 🔁 Auto-stop existing timers when a new one starts
- 🧮 Millisecond-accurate best time tracking (displayed in seconds)
- 🏆 Per-timer leaderboards with caching
- ⚡ Fully async database (HikariCP + SQLite/MySQL)
- 🗺️ Configurable exit & fallback locations
- 💬 Live-updating placeholders
- 🔒 Confirmation prompts for global resets
- 🔄 Auto-update player names and times on next join
- 💡 Tab completion for timers and selectors
- 🧰 Admin command suite for complete timer management

---

## 📦 REQUIREMENTS
- **Paper 1.21.10+**
- **Java 17+**
- **PlaceholderAPI** *(for placeholders)*  
- SQLite *(included)* or MySQL *(optional)*

---

## 🧩 INSTALLATION
1. Drop `ZTimer.jar` into your `/plugins` folder.
2. Start the server — config and database files will auto-generate.
3. Edit `config.yml` to define:
   - Timer exit points
   - Leaderboard sizes
   - Fallback spawn
   - Storage type (SQLite/MySQL)
4. Reload using:
   ```
   /ztimer reload
   ```

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

🧍 **Player command**
```
/ztimer cancel <timerId>
```
Cancels your current active timer.

---

## 🔐 PERMISSIONS

| Node | Description |
|------|--------------|
| `ztimer.admin` | Access to all admin commands |
| `ztimer.cancel.self` | Allow players to cancel their own timers *(default: true)* |

Note: If you're managing the permissions, you will see an entry for `ztimer.cancel`.  This only shows up because of `ztimer.cancel.self` and functionally does nothing.
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
| `%ztimer_current_millis_<timerId>%` | Milliseconds |

### 🔸 Best Time Data
| Placeholder | Description |
|--------------|--------------|
| `%ztimer_best_<timerId>%` | Best formatted time |
| `%ztimer_best_seconds_<timerId>%` | Best time (seconds) |
| `%ztimer_best_millis_<timerId>%` | Best time (milliseconds) |

### 🏆 Leaderboard Placeholders
Format:
```
%ztimer_top_<position>_<timerId>_<field>%
```
**Fields:**
- `name` → Player’s name  
- `time` → Formatted best time  
- `seconds` → Whole seconds  
- `millis` → Milliseconds  

**Examples:**
```
%ztimer_top_1_maze1_name%
%ztimer_top_1_maze1_time%
%ztimer_top_5_parkour_seconds%
```

If no entry exists at the requested rank, returns **empty**.

---

## ⚙️ CONFIGURATION

### General
```yaml
settings:
  fallback-exit:
    world: world
    x: 0
    y: 64
    z: 0
  debug: false
```

### Timer Exits
```yaml
timer-exits:
  maze1:
    world: world
    x: 120
    y: 65
    z: 300
```

### Leaderboards
```yaml
leaderboards:
  maze1: 10
  parkour: 5
```

---

## 💾 DATABASE

### SQLite (default)
- Located at `plugins/ZTimer/ztimer.db`
- Ideal for small to medium servers

### MySQL (HikariCP)
```yaml
storage:
  type: mysql
  host: 127.0.0.1
  database: ztimer
  user: root
  pass: password
```
Leaderboards and caches update asynchronously.

---

## 🧩 TROUBLESHOOTING

| Problem | Fix |
|----------|-----|
| Timer won’t start | Check sanitized ID and permissions |
| Placeholders blank | Run `/papi reload` |
| Leaderboard empty | No players or incorrect timerId |
| Teleport fails | Verify world name and coordinates |
| DB errors | Delete DB to auto-rebuild schema |

---

## 🧠 TIPS
- Use PlaceholderAPI + DecentHolograms for live leaderboards.  
- For large-scale servers, prefer MySQL for stable async writes.  
- Always use **sanitized timer IDs** (no spaces/symbols).  

---

## 🧑‍💻 Author

- **Zenologia**
- [GitHub Repository](https://github.com/Zenologia/ZTimer)
- [License](https://github.com/Zenologia/ZTimer/blob/main/LICENSE)
