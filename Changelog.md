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

Changes in v1.4

Separate messages.yml

Player and admin-facing messages now live in messages.yml, and legacy config.yml message values are copied into messages.yml on first startup after upgrade.

Stricter timer validation

Admin timer commands now only accept timer IDs declared under timers.

Clearer timer start messaging

Starting the same timer again now reports that it is already running, and starting a different timer clearly says the previous active timer was canceled.

YAML storage

Added YAML storage support and made it the default for fresh installs without changing existing live storage settings.

Documentation refresh

README updated to match the code, and an admin-friendly TESTING.md was added for full verification.

Changes in v1.4.1

Automatic config migration

Existing config.yml files are now synchronized to the current layout on startup and reload, with timestamped backups created before any live rewrite. Legacy message keys are removed from config.yml, legacy maze relog/logout command paths are moved under timers, and existing storage backends are preserved.
