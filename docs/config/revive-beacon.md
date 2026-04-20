---
title: Revive Beacon
nav_order: 10
parent: Configuration
description: "Reference for revive-beacon.yml — beacon-based player revive settings, target strategy, animation, and whitelist"
---

# Revive Beacon Configuration (`revive-beacon.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The revive beacon system allows a banned player to be revived by right-clicking (or shift-right-clicking) a placed beacon with a special revive voucher heart item. All settings live under the `revive-beacon:` root key.

---

## Core Settings

### `revive-beacon.enabled`

- Type: boolean
- Default: `true`
- Master switch. When `false`, no beacon interactions trigger revive logic.

### `revive-beacon.voucher-heart-id`

- Type: string
- Default: `"revive"`
- The heart ID (from `hearts.yml`) that acts as the revive voucher. Only items with this ID will trigger a revive when used on a beacon.

### `revive-beacon.require-sneak`

- Type: boolean
- Default: `false`
- When `true`, the player must be sneaking (shift held) while right-clicking the beacon to trigger a revive.

### `revive-beacon.max-distance`

- Type: number
- Default: `8.0`
- Radius in blocks around the beacon to search for a valid revive target.

### `revive-beacon.consume-on-fail`

- Type: boolean
- Default: `false`
- When `true`, the voucher item is consumed even if no valid revive target is found. When `false`, a failed revive does not cost the item.

### `revive-beacon.require-voucher-in-beacon`

- Type: boolean
- Default: `true`
- When `true`, the revive attempt requires the player to hold the voucher and complete a timed interaction (`voucher-hold-seconds`) rather than consuming it immediately.

### `revive-beacon.voucher-hold-seconds`

- Type: number
- Default: `300.0`
- Number of seconds the player must hold the interaction to complete a revive when `require-voucher-in-beacon` is `true`.

### `revive-beacon.whitelist-enabled`

- Type: boolean
- Default: `false`
- When `true`, only beacons listed in `revive-beacon-whitelist.yml` can be used as revive beacons.

---

## Target Strategy

### `revive-beacon.target-strategy`

- Type: string
- Default: `COMMAND_SELECTION`
- Controls how the revive target is chosen. Accepted values:

| Strategy | Description |
|---|---|
| `NEAREST_BANNED` | Revives the closest banned player within `max-distance`. Ties are broken by UUID lexical order. |
| `COMMAND_SELECTION` | The reviver must first run `/revive <player>` to pre-select a target. The beacon interaction then revives that specific player. |

---

## Broadcast

### `revive-beacon.broadcast.enabled`

- Type: boolean
- Default: `false`
- When `true`, broadcasts a server-wide message at the start and completion of a revive.

### `revive-beacon.broadcast.hold-start-message-key`

- Type: string
- Default: `"beacon-revive-broadcast-hold-start"`
- Language message key sent when a player begins holding the revive interaction.

### `revive-beacon.broadcast.complete-message-key`

- Type: string
- Default: `"beacon-revive-broadcast-complete"`
- Language message key sent when the revive completes.

---

## Animation

Visual effects played on the reviver and in the world during and after a successful revive.

### `revive-beacon.animation.enabled`

- Type: boolean
- Default: `true`

### `revive-beacon.animation.duration-ticks`

- Type: integer
- Default: `30`
- Length of the animation in ticks.

### `revive-beacon.animation.spiral-steps`

- Type: integer
- Default: `10`
- Number of steps in the spiral particle effect.

### `revive-beacon.animation.ring-count`

- Type: integer
- Default: `3`
- Number of particle rings during the animation.

### `revive-beacon.animation.vertical-lift-per-step`

- Type: number
- Default: `0.08`
- How much the animation rises per step.

### Particle definitions

Three named particle effects are configurable: `spiral`, `ring`, and `impact`. Each supports:

| Field | Type | Description |
|---|---|---|
| `type` | string | Bukkit particle name (e.g. `END_ROD`, `ENCHANT`, `TOTEM_OF_UNDYING`). |
| `count` | integer | Particles per burst. |
| `offset-x/y/z` | number | Spread radius on each axis. |
| `speed` | number | Particle speed/spread multiplier. |

### Sound definitions

Two sounds fire during the animation: `loop` (played during the hold) and `impact` (played on completion). Each supports:

| Field | Type | Description |
|---|---|---|
| `type` | string | Bukkit sound name (e.g. `BLOCK_BEACON_AMBIENT`). |
| `volume` | number | Playback volume (0.0–1.0+). |
| `pitch` | number | Playback pitch (0.5–2.0). |

### Full animation example

```yaml
animation:
  enabled: true
  duration-ticks: 30
  spiral-steps: 10
  ring-count: 3
  vertical-lift-per-step: 0.08
  particles:
    spiral:
      type: END_ROD
      count: 2
      offset-x: 0.0
      offset-y: 0.0
      offset-z: 0.0
      speed: 0.0
    ring:
      type: ENCHANT
      count: 2
      offset-x: 0.03
      offset-y: 0.03
      offset-z: 0.03
      speed: 0.01
    impact:
      type: TOTEM_OF_UNDYING
      count: 20
      offset-x: 0.4
      offset-y: 0.6
      offset-z: 0.4
      speed: 0.01
  sounds:
    loop:
      type: BLOCK_BEACON_AMBIENT
      volume: 0.3
      pitch: 1.2
    impact:
      type: BLOCK_BEACON_ACTIVATE
      volume: 1.0
      pitch: 1.15
```

---

## Beacon Whitelist (`revive-beacon-whitelist.yml`)

When `whitelist-enabled: true` in `revive-beacon.yml`, only beacon blocks at listed coordinates can trigger revives.

The whitelist is stored in a **separate file** (`revive-beacon-whitelist.yml`) and is managed entirely at runtime via commands — do not edit it manually while the server is running, as in-memory state will overwrite your changes on the next write.

Manage whitelisted beacons with the `/lifesteal` admin commands. The file structure looks like:

```yaml
revive-beacon-whitelist:
  whitelisted-beacons: []
```

Each entry added at runtime stores world, x, y, z coordinates. Leave `whitelist-enabled: false` (the default) to allow any beacon to be used.
