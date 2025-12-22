# ZTimer Plugin - Comprehensive Testing Checklist

**Last Updated:** 2025-12-16

---

## Overview
This document provides a comprehensive testing checklist for the ZTimer plugin. It covers all aspects of functionality, integration, performance, and user experience testing.

---

## 1. Unit Testing

### Core Timer Functionality
- [ ] Timer starts correctly when initialized
- [ ] Timer counts up/down as expected
- [ ] Timer pauses and resumes without losing time
- [ ] Timer stops and resets to initial state
- [ ] Timer handles edge cases (0 seconds, very large numbers)
- [ ] Timer precision is accurate (within 100ms tolerance)
- [ ] Multiple timer instances operate independently

### Time Calculation
- [ ] Seconds to minutes conversion is accurate
- [ ] Seconds to hours conversion is accurate
- [ ] Minutes to hours conversion is accurate
- [ ] Decimal to time format conversion works correctly
- [ ] Time parsing from various formats succeeds
- [ ] Invalid time formats are rejected with appropriate errors

### Configuration & Settings
- [ ] Default configuration loads correctly
- [ ] Custom configuration overrides defaults
- [ ] Configuration validation works for all parameters
- [ ] Invalid configurations throw appropriate errors
- [ ] Settings persist across plugin reloads
- [ ] Timezone settings are applied correctly

---

## 2. Integration Testing

### Plugin Integration
- [ ] Plugin loads without errors
- [ ] Plugin initializes with host application
- [ ] Plugin unloads cleanly
- [ ] No resource leaks on plugin unload
- [ ] Plugin communicates with host API correctly
- [ ] Error handling in API calls works as expected

### Database Integration
- [ ] Timer data saves to database correctly
- [ ] Timer data loads from database without corruption
- [ ] Database queries return expected results
- [ ] Database constraints are enforced
- [ ] Concurrent database operations don't cause conflicts
- [ ] Database rollback works on transaction failure

### User Interface Integration
- [ ] UI elements render correctly
- [ ] UI updates reflect timer state changes
- [ ] Button clicks trigger correct actions
- [ ] Input fields accept valid data
- [ ] Input validation prevents invalid entries
- [ ] UI remains responsive during long operations

### External Service Integration
- [ ] API calls to external services succeed
- [ ] Error handling for failed API calls works
- [ ] Timeout handling for slow services works
- [ ] Retry logic functions correctly
- [ ] Authentication/authorization is enforced
- [ ] Rate limiting is respected

---

## 3. Functional Testing

### Start/Stop Operations
- [ ] Start button initiates timer
- [ ] Stop button halts timer correctly
- [ ] Pause button pauses timer without stopping
- [ ] Resume button continues from pause point
- [ ] Reset button returns timer to initial state
- [ ] Starting an already-running timer doesn't restart it
- [ ] Stopping a non-running timer shows appropriate message

### Display & Formatting
- [ ] Timer displays in correct format (HH:MM:SS)
- [ ] Display updates in real-time
- [ ] Large numbers display without truncation
- [ ] Milliseconds display when applicable
- [ ] Display formatting respects locale settings
- [ ] AM/PM display works for 12-hour format

### Time Manipulation
- [ ] Manual time entry is accepted and validated
- [ ] Increment button adds time correctly
- [ ] Decrement button subtracts time correctly
- [ ] Fast-forward feature works as expected
- [ ] Rewind feature works as expected

---

## 4. Logout-Commands (NEW FEATURE) — Functional & Integration Tests

These tests cover the new "logout-commands" feature which runs configured commands immediately when a player logs out while an active timer is running. They also check interaction with the existing relog-commands and pending teleport persistence.

### Configuration & Discovery
- [ ] `mazes.<id>.logout-commands` is recognized and loaded from config
- [ ] `timers.<id>.logout-commands` (alternative location) is recognized and loaded
- [ ] When no logout-commands are configured, no commands are executed on logout

### Immediate Execution on Logout
- [ ] Commands listed under logout-commands are executed immediately when PlayerQuitEvent fires with an active timer
- [ ] Commands are dispatched as console commands (Bukkit console sender)
- [ ] Placeholders `%player%` and `%player_uuid%` are correctly replaced in executed commands
- [ ] Commands run even if relog-commands are also configured (both behaviors should work independently)

### Interaction with Pending Teleports & Relog Commands
- [ ] Pending teleport entry is still written to pending_teleports.yml on logout (unchanged behavior)
- [ ] relog-commands remain persisted and are executed on player join as before
- [ ] logout-commands do not replace or remove relog-commands unless explicitly configured by the admin

### Threading & Performance Safety
- [ ] Executing logout-commands on PlayerQuitEvent does not block the server tick for more than acceptable threshold (measure on a test server)
- [ ] If logout-commands may be long-running, ensure server tick times are acceptable (recommendation: avoid heavy blocking commands)
- [ ] Verify that running logout-commands does not cause concurrency issues with other parts of the plugin (e.g., storage writes)

### Error Handling & Logging
- [ ] If a logout command throws an exception, it is caught and logged (when debug enabled) without preventing pending teleport persistence
- [ ] Debug logging reflects logout-commands execution when debug is enabled
- [ ] Malformed commands are logged but do not crash the plugin

### Edge Cases
- [ ] Player disconnects while logout-commands are executing — verify no uncaught exceptions and pending teleport saved
- [ ] Multiple players logging out simultaneously with logout-commands configured — verify commands run for each player and no cross-contamination of placeholders
- [ ] Commands with no placeholders behave correctly
- [ ] Empty/null command entries in config are ignored safely
- [ ] If plugin is disabled before PlayerQuitEvent completes, ensure safe handling (no NPEs)

### Cross-Platform / Storage Interactions
- [ ] Logout-commands behavior is identical on both sqlite and mysql configurations
- [ ] Pending teleports file (pending_teleports.yml) continues to be written and read correctly after logout-commands run

### Security & Permissions
- [ ] Confirm commands run as console (so server-level permissions apply) and expected effects occur
- [ ] Validate that no privileged operations are inadvertently exposed (admins should configure commands intentionally)

---

## 5. Suggested Automated Tests & Mocks

Automated testing is recommended using MockBukkit or a dedicated integration test environment:

### Unit / Mock Tests
- [ ] Unit test TimerManager.handleLogout(Player) using a mock Player that verifies:
  - runRelog/dispatch method is invoked with expected command strings
  - pendingTeleportManager.setPendingTeleport is called with correct timer id and relog-commands
- [ ] Add a test for placeholder substitution method (or runRelogCommands helper), asserting correct replacement of `%player%` and `%player_uuid%`
- [ ] Mock the Bukkit scheduler to ensure no scheduling regressions (e.g., relog runs with runTaskLater)

### Integration Tests
- [ ] Integration test on a test server:
  - Configure logout-commands and relog-commands for a timer
  - Start a timer, disconnect the player, assert logout command side-effects (e.g., entry in a test log or plugin state)
  - Reconnect player and assert relog commands run and pending teleport consumed
- [ ] Performance test:
  - Create a logout-command that simulates some work (lightweight) and measure tick durations during high concurrent logouts

---

## 6. Notes & Test Data

- Example config for tests:
  - `mazes.testMaze.logout-commands: ["say LOGOUT %player% %player_uuid%"]`
  - `mazes.testMaze.relog-commands: ["say RELOG %player% %player_uuid%"]`

- Test players: use deterministic names and UUIDs for assertion stubs.

---

## 7. Misc / Regression Checklist
- [ ] Verify PlayerQuitListener still calls TimerManager.handleLogout correctly
- [ ] Ensure no regression in cancel/reset/start/stop flows after adding logout-commands
- [ ] Update README / CHANGELOG to note the new logout-commands option

---

## 8. Manual Acceptance Criteria
- [ ] Admin can configure logout-commands and observe immediate effects on logout
- [ ] Admin can still configure relog-commands and observe delayed effects on join
- [ ] No crash, no data loss, and plugin logs helpful debug information when enabled
