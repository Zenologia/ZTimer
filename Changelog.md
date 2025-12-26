Changes in v1.1

Persistent pending teleports

Players who disconnect during a run now get a pending teleport recorded to plugins/ZTimer/pending_teleports.yml so the teleport survives server restarts.
Optional relog commands

Per-timer relog-commands can be configured (executed as console on the player's next join). Placeholders supported: %player%, %player_uuid%.

Changes in v1.2
Adding to the logic of relog command - added logout commands 

Changes in v1.3
Added placeholder `%ztimer_active_global_<timerId>` to globally show if a timer is active - not just to the one running the timer 

Changes in v1.3.1
Fixed leaderboards not parsing correctly due to underscores in timer IDs
