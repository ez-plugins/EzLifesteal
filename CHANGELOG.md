# Changelog

All notable changes to EzLifesteal are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Release tags use the `v` prefix (e.g. `v1.1.0`).

---

## [Unreleased]

### Added

### Changed

### Fixed

### Removed

---

## [1.1.0] - 2026-05-12

### Added

- **Team Heart Bank (TeamsAPI-backed)**:
  - New `/lifesteal teambank` command group with:
    - `balance`
    - `deposit <amount>`
    - `withdraw <amount>`
  - Shared team-bank balances stored per TeamsAPI team UUID.
  - New storage layer for team banks with YAML and MySQL implementations.
  - New permission nodes:
    - `lifesteal.teambank.balance`
    - `lifesteal.teambank.deposit`
    - `lifesteal.teambank.withdraw`
  - New `lifesteal-core.yml` keys:
    - `team-bank.enabled`
    - `team-bank.max-hearts`
  - New language keys for team-bank success/failure and validation states.
  - Unit tests for team-bank service, command handling, and storage repositories.
- **Beacon spawn subsystem** (`revive-beacon.yml` → `revive-beacon.spawn.*`): the plugin can
  now place BEACON blocks in the world automatically on a configurable schedule or on demand
  via command.
  - `max-concurrent` — cap on simultaneously active plugin-spawned beacons.
  - `expiry-minutes` — beacons are auto-removed after the configured duration (set to `0` to
    keep indefinitely).
  - `random-spawn` — configurable world and XZ bounding box used when no explicit coordinates
    are supplied.
  - `schedule` — recurring auto-spawn at a configurable `interval-minutes`.
  - `availability-event` — server broadcast, title overlay, particles, and fireworks that fire
    when a beacon transitions to the AVAILABLE state.
- **WorldGuard integration**: an optional region is created around each plugin-spawned beacon,
  preventing tampering. Configurable radius and flag overrides (`deny-build`, `deny-pvp`,
  `deny-mob-damage`, `deny-explosions`). Silently skipped when WorldGuard is absent.
- **EzCountdown integration**: an optional countdown timer is shown while the beacon is warming
  up. Supports `ACTION_BAR`, `BOSS_BAR`, `CHAT`, `TITLE`, and `SCOREBOARD` display types.
  Falls back to an internal Bukkit task when EzCountdown is absent.
- **TeamsAPI integration**: optional team-kill bypass — heart transfers are skipped when the
  killer and victim are in the same team. Enabled via the new
  `team-kill-bypass-with-teams-api` key in `lifesteal-core.yml`.
- **`/beacon` command**: standalone alias for `/lifesteal beacon`. Accepts the same
  subcommands: `add`, `remove`, `list`, `clear`, `spawn`, `despawn`, `spawns`.
- **`beacon spawn / despawn / spawns` subcommands** under `/lifesteal beacon`:
  - `spawn [world x y z]` — spawn a beacon at explicit coordinates or a random location.
  - `despawn <id|all>` — remove one or all active plugin-spawned beacons.
  - `spawns` — list all currently active plugin-spawned beacons and their status.
- New language keys in all bundled locale files (`en`, `de`, `es`, `fr`, `nl`, `pt`, `ru`, `zh`):
  `beacon-spawn-available-broadcast`, `beacon-spawn-available-title`,
  `beacon-spawn-available-subtitle`, `beacon-spawn-not-available`, `beacon-spawn-protected`.
- `WorldGuard`, `EzCountdown`, and `TeamsAPI` added to `softdepend` in `plugin.yml`.

### Changed

- Documentation expanded for team bank support:
  - Added `/lifesteal teambank` entries to command reference.
  - Added team-bank nodes to permissions reference and role inheritance.
  - Added team-bank configuration docs (including defaults and requirements).
  - Updated core configuration overview to include team settings.
- GitHub Actions dependencies bumped via Dependabot:
  `actions/checkout` → v6, `actions/configure-pages` → v6, `actions/deploy-pages` → v5,
  `actions/upload-artifact` → v7, `actions/upload-pages-artifact` → v5,
  `DavidAnson/markdownlint-cli2-action` → v23, `codecov/codecov-action` → v6.

---

[Unreleased]: https://github.com/ez-plugins/EzLifesteal/compare/v1.1.0...HEAD
[1.1.0]: https://modrinth.com/plugin/ezlifesteal/version/1.1.0
