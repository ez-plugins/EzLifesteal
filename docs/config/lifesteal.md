---
title: Lifesteal Core
nav_order: 5
parent: Configuration
description: "Reference for lifesteal-core.yml — heart bounds, gain/loss math, ban policy, combat-logout protection, and consumption effects"
---

# Lifesteal Core Configuration (`lifesteal-core.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

`lifesteal-core.yml` controls the fundamental heart system: starting values, gain/loss amounts, the zero-heart ban or kick policy, health scaling, combat-logout protection, and item-consumption visual effects.

Settings for heart drops, world scoping, mob rewards, kill streaks, and revive beacon are in their own dedicated files:

- Heart drops → [`lifesteal-drops.yml`](./drops.md)
- World overrides → [`lifesteal-worlds.yml`](./worlds.md)
- Mob rewards → [`lifesteal-mobs.yml`](./mobs.md)
- Kill streaks → [`lifesteal-killstreaks.yml`](./killstreaks.md)
- Revive beacon → [`revive-beacon.yml`](./revive-beacon.md)

---

## Global Switch

### `global-enabled`

- Type: boolean
- Default: `true`
- Master switch. When `false`, all lifesteal gain/loss processing stops.

---

## Core Bounds

### `default-hearts`

- Type: number
- Default: `10.0`
- Heart count assigned to new players and used when resetting a profile.

### `min-hearts`

- Type: number
- Default: `1.0`
- Lower clamp. A player's stored hearts will never fall below this value through normal gameplay. Set to `0` to allow elimination.

### `max-hearts`

- Type: number
- Default: `40.0`
- Upper clamp. Hearts gained beyond this value are discarded.

---

## Health Scaling

### `apply-health-scale`

- Type: boolean
- Default: `false`
- When `true`, the plugin applies a fixed health scale to all players. Use this to normalise the displayed heart row to a fixed UI size regardless of actual max health.

### `health-scale`

- Type: number
- Default: `20.0`
- The health value used for Bukkit's `setHealthScale`. Only used when `apply-health-scale` is `true`.

---

## Gain / Loss Math

### `hearts-per-kill`

- Type: number
- Default: `1.0`
- Hearts added to the killer after a PvP kill. Supports decimals.

### `hearts-lost-on-death`

- Type: number
- Default: `1.0`
- Hearts removed from the victim on a PvP death. Supports decimals.

---

## Zero-heart Policy

### `ban-when-zero-hearts`

- Type: boolean
- Default: `false`
- When `true`, any player whose hearts reach zero is banned. Set to `false` to kick instead.

### `zero-heart-ban-message`

- Type: string
- Default: `"You have run out of hearts."`
- Message shown in the ban screen. Supports colour codes.

### `zero-heart-kick-message`

- Type: string
- Default: `"You have run out of hearts."`
- Message sent when kicking a player at zero hearts (used when `ban-when-zero-hearts` is `false`).

### `zero-heart-commands`

- Type: list of strings
- Default: `[]` (disabled)
- Console commands executed when a player reaches zero hearts, before the ban or kick. Supports placeholders:
  - `%player%` / `%player_uuid%` — the eliminated player
  - `%killer%` / `%killer_uuid%` — the killer (empty for non-PvP deaths)
  - `%hearts%` / `%remaining_hearts%`

```yaml
zero-heart-commands:
  - "broadcast &c%player% has been eliminated!"
  - "title %player% title {\"text\":\"Eliminated\",\"color\":\"red\"}"
```

---

## Combat-logout Protection

Prevents players from logging out mid-fight to avoid heart loss.

### `combat-logout-protection.enabled`

- Type: boolean
- Default: `false`

### `combat-logout-protection.tag-duration-seconds`

- Type: number
- Default: `15.0`
- Seconds a player remains "combat-tagged" after their last PvP hit. Logging out during this window applies the full heart loss.

```yaml
combat-logout-protection:
  enabled: true
  tag-duration-seconds: 15.0
```

---

## Heart Consumption Effects

Visual effects played when a player uses (right-clicks) a heart voucher item.

### `heart-consumption-effects.enabled`

- Type: boolean
- Default: `true`

### `heart-consumption-effects.stacking-particles`

- Type: boolean
- Default: `true`
- When `true`, particles are layered in sequence over `stacking-duration-ticks`.

### `heart-consumption-effects.stacking-duration-ticks`

- Type: integer
- Default: `40`
- Total duration of the stacked particle sequence.

### `heart-consumption-effects.particles`

- Type: list of particle definitions
- Each entry supports:
  - `type` — Bukkit particle name (e.g. `DUST`, `HEART`, `END_ROD`)
  - `count` — number of particles per burst
  - `speed` — particle speed/spread modifier
  - `color` — hex colour string; only used by `DUST` type (e.g. `"#8B0000"`)

```yaml
heart-consumption-effects:
  enabled: true
  stacking-particles: true
  stacking-duration-ticks: 40
  particles:
    - type: DUST
      count: 50
      speed: 0.1
      color: "#8B0000"
    - type: HEART
      count: 20
      speed: 0.05
```
