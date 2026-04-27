# 🧪 ZTimer Testing Guide
*Developed by Zenologia*

This guide is written for server admins who want to verify ZTimer 1.4 end to end without reading the source code.

## Before you start

- Use a staging server first if you can.
- Install PlaceholderAPI before enabling ZTimer.
- Have at least one admin account with `ztimer.admin`.
- Have a second player account available if you want to test selectors, leaderboards, and global placeholders properly.
- Back up `plugins/ZTimer/` before testing on a live server.

Files worth watching during testing:
- `plugins/ZTimer/config.yml`
- `plugins/ZTimer/messages.yml`
- `plugins/ZTimer/pending_teleports.yml`
- one active storage file:
  - `plugins/ZTimer/ztimer-data.yml`
  - `plugins/ZTimer/ztimer.db`
  - MySQL/MariaDB table `ztimer_best_times`

---

## 1. Startup and file generation

### Fresh install check
1. Start the server with ZTimer and PlaceholderAPI installed.
2. Confirm the plugin enables cleanly.

Expected result:
- `config.yml` is created.
- `messages.yml` is created.
- `pending_teleports.yml` is created.
- Because new installs default to YAML, `storage.type` is `yaml` in `config.yml`.
- `ztimer-data.yml` is created for YAML storage.

### Upgrade safety check
1. Use an older live-style `config.yml` with an existing `storage.type` already set.
2. Make sure `messages.yml` does not exist yet.
3. Start the server.

Expected result:
- The existing `storage.type` is left alone.
- The plugin does not switch the live config to YAML by itself.
- A new `messages.yml` is created.
- Legacy `config.yml` message values are copied into `messages.yml` on first startup.

---

## 2. Timer ID validation

### Valid timer test
1. Make sure `timers.maze1` exists in `config.yml`.
2. Run:
```text
/ztimer start maze1 <player>
```

Expected result:
- The command succeeds.

### Invalid timer test
1. Run:
```text
/ztimer start fake_timer <player>
```

Expected result:
- The command fails with the invalid timer message from `messages.yml`.

### Timers-only validation test
1. Add `mazes.maze2.exit_location`.
2. Add `leaderboards.per_timer.maze2`.
3. Do not add `timers.maze2`.
4. Run:
```text
/ztimer start maze2 <player>
```

Expected result:
- The command still fails.
- Only timer IDs listed under `timers` are accepted by admin commands.

---

## 3. Start behavior and roadmap message checks

### Normal start
1. Run:
```text
/ztimer start maze1 <player>
```

Expected result:
- The player gets a normal start message.

### Starting the same timer again
1. Start `maze1`.
2. Wait a few seconds.
3. Run the same command again:
```text
/ztimer start maze1 <player>
```

Expected result:
- The plugin says the timer is already running.
- The timer is not restarted.

Quick proof:
1. Wait a little longer.
2. Stop the timer.
3. Confirm the time reflects the original start, not the second command.

### Starting a different timer while one is active
1. Add `timers.maze2` to `config.yml`.
2. Start `maze1`.
3. Run:
```text
/ztimer start maze2 <player>
```

Expected result:
- The plugin says `maze2` started.
- The message clearly says the previous timer was canceled.
- The active timer is now `maze2`.

---

## 4. Stop, reset, and cancel commands

### Stop active timer
1. Start a timer.
2. Wait a few seconds.
3. Run:
```text
/ztimer stop maze1 <player>
```

Expected result:
- The stop message appears.
- The elapsed time is shown using `formatting.time_pattern`.
- A best time is saved.

### Stop inactive timer
1. Run stop again:
```text
/ztimer stop maze1 <player>
```

Expected result:
- The plugin reports that the timer is not running.

### Per-player reset
1. Save a best time for a player.
2. Run:
```text
/ztimer reset maze1 <player>
```

Expected result:
- That player’s best time is cleared.
- Other players’ times remain.

### Global reset confirmation
1. Run:
```text
/ztimer reset maze1
```

Expected result:
- The plugin sends the confirmation message.

### Global reset execution
1. Run:
```text
/ztimer reset maze1 confirm
```

Expected result:
- All stored times for `maze1` are removed.
- The leaderboard for that timer clears.

### Self cancel
1. Join as a normal player with `ztimer.cancel.self`.
2. Start a timer for yourself.
3. Run:
```text
/ztimer cancel maze1
```

Expected result:
- Your active timer is canceled.
- You are teleported to the timer exit, fallback exit, or world spawn fallback.

### Console self-cancel rejection
1. Run this from console:
```text
/ztimer cancel maze1
```

Expected result:
- The console gets the "only players may self-cancel" message.

### Admin cancel for another player
1. Start a timer for a target player.
2. Run:
```text
/ztimer cancel maze1 <player>
```

Expected result:
- The timer is canceled.
- The player is teleported.

---

## 5. Selector handling and tab completion

### Valid selector
1. Run:
```text
/ztimer start maze1 @a
```

Expected result:
- All online players are targeted.

### Invalid selector
1. Run:
```text
/ztimer start maze1 definitely_not_a_player
```

Expected result:
- The invalid selector message appears.

### Tab completion
1. Start typing `/ztimer st`.
2. Start typing `/ztimer start ma`.
3. Start typing `/ztimer start maze1 @`.

Expected result:
- Subcommands complete.
- Timer IDs from `timers` complete.
- Selectors and online player names complete.

---

## 6. Teleport handling

### Timer exit location
1. Set `mazes.maze1.exit_location`.
2. Start `maze1`.
3. Cancel it.

Expected result:
- The player lands at the configured timer exit location.

### Fallback exit location
1. Remove the timer-specific exit location for a test timer.
2. Enable `fallback_exit_location`.
3. Cancel the timer.

Expected result:
- The player lands at the fallback exit location.

### Final spawn fallback
1. Use a timer with no exit location.
2. Disable `fallback_exit_location`.
3. Cancel the timer.

Expected result:
- The player lands at the main world spawn, or the player’s world spawn if needed.

---

## 7. Logout and relog flow

### Logout commands
1. Configure `timers.maze1.logout-commands`.
2. Start `maze1`.
3. Disconnect the player.

Expected result:
- Logout commands run immediately as console commands.

### Pending teleport persistence
1. After the logout, inspect `pending_teleports.yml`.

Expected result:
- The player has a pending teleport entry.

### Relog handling
1. Rejoin the server with the same player.

Expected result:
- The player is teleported to the exit handling target.
- `relog-commands` run.
- The pending teleport entry is removed.

### Restart survival test
1. Start a timer.
2. Log out.
3. Restart the server before rejoining.
4. Rejoin.

Expected result:
- The pending teleport still works after restart.

---

## 8. Storage verification

### YAML storage
1. Set `storage.type: yaml`.
2. Restart the server.
3. Record a best time.
4. Open `ztimer-data.yml`.

Expected result:
- The player UUID exists under `players`.
- The timer ID is stored under that player.
- `best_millis` and `last_updated` are present.

### SQLite storage
1. Set `storage.type: sqlite`.
2. Restart the server.
3. Record a best time.

Expected result:
- `ztimer.db` exists.
- The best time is available through placeholders and leaderboards.

### MySQL or MariaDB storage
1. Set `storage.type: mysql` or `storage.type: mariadb`.
2. Enter valid connection details.
3. Restart the server.
4. Record a best time.

Expected result:
- The plugin connects successfully.
- Table `ztimer_best_times` is created if needed.
- Best times and leaderboards work normally.

Important note:
- Do not use `/ztimer reload` to switch storage types. Always restart the server after changing `storage.type`.

---

## 9. Placeholder checks

Use PlaceholderAPI tools, a scoreboard plugin, or `/papi parse me` if you have it available.

### Active placeholders
- `%ztimer_active_maze1%`
- `%ztimer_active_global_maze1%`

Expected result:
- Per-player active reflects the viewer’s own timer state.
- Global active returns `true` when anyone is running that timer.

### Current time placeholders
- `%ztimer_current_maze1%`
- `%ztimer_current_seconds_maze1%`
- `%ztimer_current_millis_maze1%`

Expected result:
- Values update while the timer is active.

### Best time placeholders
- `%ztimer_best_maze1%`
- `%ztimer_best_seconds_maze1%`
- `%ztimer_best_millis_maze1%`

Expected result:
- Values appear after a saved run.

### Leaderboard placeholders
- `%ztimer_top_1_maze1_name%`
- `%ztimer_top_1_maze1_time%`

Expected result:
- Top entries reflect saved best times in the correct order.

### Underscore timer ID check
1. Create a timer like `maze_one`.
2. Save times for it.
3. Test:
```text
%ztimer_top_1_maze_one_name%
%ztimer_top_1_maze_one_time%
```

Expected result:
- Leaderboard placeholders still resolve correctly.

---

## 10. Messages and reload behavior

### Message source of truth
1. Edit a message in `messages.yml`, such as `info.reload`.
2. Run:
```text
/ztimer reload
```

Expected result:
- The new reload message is used immediately.

### Command message verification
Confirm the following messages all come from `messages.yml`:
- invalid timer
- invalid selector
- start
- start replaced
- timer already running
- stop
- reset confirm
- reset success
- cancel
- reload
- usage messages

---

## 11. Final admin acceptance checklist

You can treat the plugin as ready when all of the following are true:
- Startup is clean.
- `config.yml`, `messages.yml`, and `pending_teleports.yml` are generated correctly.
- Timer IDs are only accepted when declared under `timers`.
- Starting the same timer twice does not reset it.
- Starting a different timer clearly reports the replacement.
- Stop, reset, cancel, and reload commands all behave correctly.
- Teleport handling works for timer exits, fallback exits, and spawn fallback.
- Logout and relog handling works, including restart survival.
- Your chosen storage backend saves and loads best times correctly.
- Placeholders return the expected values.
- Editing `messages.yml` and running `/ztimer reload` updates message output immediately.

---

## Suggested test order

If you want the shortest practical verification path, run the checks in this order:
1. Startup and file generation
2. Timer ID validation
3. Start behavior and roadmap message checks
4. Stop, reset, and cancel commands
5. Teleport handling
6. Logout and relog flow
7. Storage verification
8. Placeholder checks
9. Messages and reload behavior
