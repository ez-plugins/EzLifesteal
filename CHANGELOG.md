# Changelog

All notable changes to EzLifesteal are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Release tags use the `v` prefix (e.g. `v1.1.0`).

---

## [Unreleased]

---

## [1.1.2] - 2026-05-31

### Added

- **Hologram orphan cleanup command** — `/lifesteal hologram cleanup [radius]` (player-only, requires `lifesteal.scoreboard.remove` or `lifesteal.hologram` or `lifesteal.admin`). Scans nearby entities within the given radius (default 10, clamped to 1–64) and removes armor stands that look like EzLifesteal hologram lines but are no longer tracked by the manager. Useful after unexpected server crashes that leave ghost stands in the world.
- **PersistentDataContainer tagging** — every armor stand spawned by `TopHologramManager` is now tagged with a `NamespacedKey("hologram_line")` PDC entry. This makes orphan detection reliable across server restarts. Pre-existing untagged stands are identified via a heuristic (marker, invisible, no gravity, small, custom-name-visible, no base-plate, no arms).
- Language keys `hologram-cleanup-removed` and `hologram-cleanup-none` added to all 8 locale files (en, de, es, fr, nl, pt, ru, zh).

---

## [1.1.1] - 2026-05-24

### Added

- Bumped project version to 1.1.1.
- Short-term fix for team-bank persistence: perform a compensating rollback of a player's profile when a subsequent team-bank save fails, preventing inconsistent state between player hearts and the team bank.
- Added focused unit tests that exercise team-bank rollback and atomicity paths to address patch-level coverage requirements; JaCoCo checks pass locally.

### Changed

- Bumped dependency versions (WorldEdit, WorldGuard, EzCountdown, Testcontainers, JUnit, Awaitility, SLF4J and others) per dependabot updates.

### Fixed

- Resolved a patch-coverage failure by adding targeted tests for `TeamBankService` and `TeamBankAdminService`; coverage for modified lines now meets project rules.

---

## [1.1.0] - 2026-05-17

> **What's new in 1.1.0:** shared Team Heart Bank (requires TeamsAPI), automated revive beacon
> placement with scheduling and random regions, WorldGuard protection for auto-spawned beacons,
> EzCountdown support for beacon warm-up timers, and an expanded interactive beacon GUI.
> Several bug fixes for bans, beacons, and the team bank.

### Added

#### Team Heart Bank *(requires [TeamsAPI](https://modrinth.com/plugin/teams-api))*

Teams can now pool hearts into a shared bank. Players deposit and withdraw hearts; admins can
inspect or adjust any team's balance directly from the console or in-game.

**Player commands:**

| Command | Permission |
|---|---|
| `/lifesteal teambank balance` | `lifesteal.teambank.balance` |
| `/lifesteal teambank deposit <amount>` | `lifesteal.teambank.deposit` |
| `/lifesteal teambank withdraw <amount>` | `lifesteal.teambank.withdraw` |

**Admin commands** — operate on any team by name or UUID:

| Command | Permission |
|---|---|
| `/lifesteal teambank admin balance <team>` | `lifesteal.teambank.admin.balance` |
| `/lifesteal teambank admin deposit <team> <amount>` | `lifesteal.teambank.admin.deposit` |
| `/lifesteal teambank admin withdraw <team> <amount>` | `lifesteal.teambank.admin.withdraw` |
| `/lifesteal teambank admin reset <team>` | `lifesteal.teambank.admin.reset` |
| `/lifesteal teambank admin transfer <from> <to> <amount>` | `lifesteal.teambank.admin.transfer` |

The bank uses YAML or MySQL storage, matching your existing storage setting.

**New `lifesteal-core.yml` keys:**

```yaml
team-bank:
  enabled: true
  max-hearts: 100           # global cap on a team bank balance
  per-team-overrides:       # optional per-team cap (overrides max-hearts for that team)
    MyTeam: 50              # key: team name or UUID
```

**Team-kill bypass** — heart transfers are skipped when killer and victim share a team.
Replaces the old flat `team-kill-bypass-with-teams-api` boolean with a richer section
(old key still works as a fallback — see [Migration notes](#migration-notes) below):

```yaml
team-kill-bypass:
  enabled: true
  exempt-worlds:            # worlds where the bypass does NOT apply
    - pvp-arena
  min-team-size: 2          # bypass only activates when the team has at least N members
```

#### Beacon Auto-Spawn

The plugin can now place revive beacons in the world automatically — on a recurring timer,
on demand via command, or at a random location within a configured bounding box or weighted
region list.

**[EzCountdown](https://modrinth.com/plugin/ezcountdown) Bossbar countdown**

![EzCountdown Beacon Countdown](https://i.ibb.co/60VcQ6Z1/image.png)

**Warming up time**

![Minecraft Lifesteal Revive Beacon Warming Up](https://i.ibb.co/wZshsy8b/image.png)

**Player selection**

![Beacon Player selection Revive](https://i.ibb.co/KRpjZw0/image.png)

**New `/beacon` command** — top-level alias for `/lifesteal beacon` — with three new subcommands:

| Command | Description |
|---|---|
| `/beacon spawn [world x y z]` | Place a beacon at explicit coordinates, or a random location |
| `/beacon despawn <id\|all>` | Remove one or all active plugin-spawned beacons |
| `/beacon spawns` | List all active plugin-spawned beacons and their status |

**New `revive-beacon.yml` keys under `spawn`:**

```yaml
spawn:
  max-concurrent: 1           # maximum simultaneously active plugin-spawned beacons
  expiry-minutes: 60          # auto-remove after N minutes (0 = never expire)
  cooldown-minutes: 30        # minimum time between consecutive spawns (0 = no cooldown)
  schedule:
    interval-minutes: 120     # auto-spawn every N minutes (0 = disabled)
  random-spawn:
    world: world
    min-x: -1000
    max-x:  1000
    min-z: -1000
    max-z:  1000
    min-y: 0                  # 0 = use world surface height (getHighestBlockY)
    max-y: 0
  random-spawn-regions:       # optional: named weighted regions replace the single bounding box
    spawn-area:
      weight: 3
      min-x: -200
      max-x:  200
      min-z: -200
      max-z:  200
  availability-event:         # announcements/effects when beacon becomes usable
    broadcast: true
    title: true
    particles: true
    fireworks: true
  countdown:
    name-prefix: "beacon-"    # EzCountdown timer name prefix
    per-type-messages: {}     # per-display-type message overrides
```

#### WorldGuard integration *(optional)*

A WorldGuard region is automatically created around each plugin-spawned beacon to prevent
players from breaking or tampering with it. The region is removed when the beacon despawns.
Configurable radius and flag overrides: `deny-build`, `deny-pvp`, `deny-mob-damage`,
`deny-explosions`. Silently skipped when WorldGuard is not installed.

#### EzCountdown integration *(optional)*

An EzCountdown timer is shown while a beacon warms up before becoming available. Supports
`ACTION_BAR`, `BOSS_BAR`, `CHAT`, `TITLE`, and `SCOREBOARD` display types. Falls back to
an internal timer when EzCountdown is not installed.

#### Soft dependencies

`WorldGuard`, `EzCountdown`, and `TeamsAPI` are now listed as `softdepend` in `plugin.yml`.
The plugin loads and works fully without any of them — each integration is silently skipped
when its dependency is absent.

#### Translations

All new messages (team bank player/admin commands, beacon auto-spawn announcements) are
translated in all eight bundled locales: `en`, `de`, `es`, `fr`, `nl`, `pt`, `ru`, `zh`.

### Fixed

- **Players were not banned at zero hearts when using the default `min-hearts` setting** —
  the zero-heart ban check ran against an already-floored value, so players reaching zero
  hearts were never banned under the default `min-hearts: 1.0`. Ban now fires correctly.
- **`/pardon <player>` was reversed on server restart** — manually pardoning a player (via
  `/pardon` or by editing `banned-players.json`) was undone the next time the server started,
  because the plugin re-applied the stored ban. Bukkit's ban list is now treated as
  authoritative: if a ban is missing there, it is removed from storage too.
- **Crash on Paper 26.1.2 when banning players** — an incompatibility with the Paper 26.1.2
  ban API caused a `ClassCastException` whenever the plugin tried to issue a ban. Fixed.
- **Revive beacon GUI opened when holding a revive voucher** — right-clicking a beacon with a
  valid voucher in hand should trigger a revive, not open the info GUI. The GUI was opening
  anyway due to listener priority ordering. Fixed.
- **Beacon availability broadcast and effects not fired for instant-availability beacons** —
  beacons with no countdown (immediate availability) skipped the availability event entirely,
  so no server broadcast, title overlay, particles, or fireworks were shown. Fixed.
- **Team bank returned "invalid amount" instead of "feature disabled"** — when the team bank
  was turned off, `deposit`/`withdraw` validated the heart amount before checking whether the
  feature was enabled, returning a misleading error. Corrected to check enabled-state first.
- **Overflow heart items dropped at the victim's respawn point instead of the kill location**
  — when a killer's inventory was full, the extra heart item spawned at the victim's new
  respawn position rather than at the kill site. Now dropped at the killer's location.
- **Team-bank messages missing from non-English locales** — `de`, `es`, `fr`, `nl`, `pt`,
  `ru`, and `zh` locale files were missing the team-bank message keys introduced in earlier
  builds. All locale files are now complete.

### Changed

- **Beacon info GUI** expanded from 27 to 54 slots: rows 4–5 now list eliminated players
  (click to select a revive target); row 6 has pagination and a direct "Use Beacon" button
  — players no longer need to close and reopen the beacon to perform a revive.
- **`team-kill-bypass-with-teams-api`** flat key superseded by the new `team-kill-bypass`
  section (see Added above). The old flat key is still read as a fallback — no immediate
  action required.

### Migration notes

No breaking changes. Existing configs continue to work without modification.
One key is superseded but remains backward-compatible:

| Old key (`lifesteal-core.yml`) | Replacement | Notes |
|---|---|---|
| `team-kill-bypass-with-teams-api: true/false` | `team-kill-bypass.enabled: true/false` | Old key still works as a fallback |

### Developer API

Four Bukkit events are fired during the plugin-spawned beacon lifecycle
(package `com.skyblockexp.ezlifesteal.api.event`):

| Event | Cancellable | Fires when |
|---|---|---|
| `BeaconSpawnEvent` | ✓ | Before the beacon block is placed in the world |
| `BeaconAvailableEvent` | | Beacon transitions from warm-up to available |
| `BeaconUsedEvent` | | A player successfully uses a beacon to revive someone |
| `BeaconExpiredEvent` | | Beacon expires naturally or is forcibly despawned |