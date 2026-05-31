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
| --- | --- |
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
| --- | --- | --- |
| `type` | string | Bukkit particle name (e.g. `END_ROD`, `ENCHANT`, `TOTEM_OF_UNDYING`). |
| `count` | integer | Particles per burst. |
| `offset-x/y/z` | number | Spread radius on each axis. |
| `speed` | number | Particle speed/spread multiplier. |

### Sound definitions

Two sounds fire during the animation: `loop` (played during the hold) and `impact` (played on completion). Each supports:

| Field | Type | Description |
| --- | --- | --- |
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

---

## Beacon Spawn

The beacon spawn subsystem lets the plugin place BEACON blocks in the world automatically — on a recurring schedule, manually via command, or from a fixed set of coordinates. It lives under the `spawn:` key inside `revive-beacon.yml` and requires `revive-beacon.enabled: true`.

### `spawn.enabled`

- Type: boolean
- Default: `false`
- Master switch for the whole spawn subsystem.

### `spawn.max-concurrent`

- Type: integer
- Default: `1`
- Maximum number of plugin-spawned beacons that may be active at the same time. A new spawn is rejected until the count drops below this value.

---

### WorldGuard protection (`spawn.worldguard`)

When WorldGuard is installed, a cuboid region is automatically created around each spawned beacon. If WorldGuard is absent the section is silently ignored.

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `worldguard.enabled` | boolean | `true` | Create a protected region. |
| `worldguard.radius` | integer | `10` | Half-size of the protected cuboid in blocks. |
| `worldguard.deny-build` | boolean | `true` | Deny block placement/breaking inside the region. |
| `worldguard.deny-pvp` | boolean | `false` | Deny PvP inside the region. |
| `worldguard.deny-mob-damage` | boolean | `false` | Deny mob damage inside the region. |
| `worldguard.deny-explosions` | boolean | `false` | Deny explosion damage inside the region. |

The region is removed automatically when the beacon is despawned.

---

### Countdown timer (`spawn.countdown`)

When EzCountdown is installed and `countdown.enabled: true`, a visible countdown is shown to all players from the moment the beacon is placed until it becomes _available_. If EzCountdown is absent, an internal Bukkit scheduler is used as a silent fallback.

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `countdown.enabled` | boolean | `true` | Show an EzCountdown timer. |
| `countdown.duration-seconds` | integer | `300` | Warm-up time in seconds before the beacon becomes interactive. |
| `countdown.display-types` | list | `ACTION_BAR`, `BOSS_BAR` | EzCountdown display modes. Accepted values: `ACTION_BAR`, `BOSS_BAR`, `CHAT`, `TITLE`, `SCOREBOARD`. |
| `countdown.format-message` | string | see config | Countdown text. Supports `{formatted}`, `{minutes}`, `{seconds}`, `{hours}`, `{days}`, `{name}`. |
| `countdown.boss-bar-color` | string | `PURPLE` | Boss bar colour: `BLUE`, `GREEN`, `PINK`, `PURPLE`, `RED`, `WHITE`, `YELLOW`. |
| `countdown.boss-bar-style` | string | `SEGMENTED_20` | Boss bar style: `SOLID`, `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12`, `SEGMENTED_20`. |
| `countdown.name-prefix` | string | `ezls-beacon-` | Prefix for the EzCountdown countdown ID (`<prefix><beacon-short-id>`). |
| `countdown.per-type-messages` | map | `{}` | Per-display-type message overrides. Keys match `display-types` values. Leave empty to use `format-message` for all types. |
| `countdown.start-message` | string | `""` | Server-wide broadcast sent when the countdown starts. Leave blank to disable. |
| `countdown.end-message` | string | `""` | Server-wide broadcast sent when the countdown ends. Leave blank to disable. |
| `countdown.end-commands` | list | `[]` | Console commands dispatched when the countdown ends. Supports `{name}` for the countdown ID. |
| `countdown.update-interval-seconds` | integer | `1` | How often EzCountdown refreshes the display. Increase to reduce tick load on busy servers. |
| `countdown.visibility-permission` | string | `""` | Permission node required to see the overlay. Leave blank to show to all players. |
| `countdown.ephemeral` | boolean | `true` | When `true`, the countdown is kept only in memory and never saved to EzCountdown's `countdowns.yml`. Recommended: keep `true` to avoid orphaned entries after a restart. |

---

### Random spawn bounds (`spawn.random-spawn`)

Used by the auto-schedule and by `/lifesteal beacon spawn` when no explicit coordinates are provided.

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `random-spawn.enabled` | boolean | `false` | Allow random coordinate selection. |
| `random-spawn.world` | string | `world` | World in which to spawn. |
| `random-spawn.min-x` | integer | `-1000` | Minimum X coordinate. |
| `random-spawn.max-x` | integer | `1000` | Maximum X coordinate. |
| `random-spawn.min-z` | integer | `-1000` | Minimum Z coordinate. |
| `random-spawn.max-z` | integer | `1000` | Maximum Z coordinate. |

The beacon is always placed at the highest solid block at the chosen x/z pair.

---

### Auto-schedule (`spawn.schedule`)

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `schedule.enabled` | boolean | `false` | Enable recurring auto-spawn. |
| `schedule.interval-minutes` | integer | `60` | How often a new beacon is spawned when the active count is below `max-concurrent`. |

---

### Expiry (`spawn.expiry-minutes`)

- Type: integer
- Default: `30`
- Minutes after which an _available_ but unused spawned beacon is automatically removed. Set to `0` to keep beacons indefinitely.

---

### Availability event (`spawn.availability-event`)

Fired when a spawned beacon finishes its countdown and becomes interactive.

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `availability-event.broadcast.enabled` | boolean | `true` | Server-wide broadcast when the beacon is ready. |
| `availability-event.broadcast.message-key` | string | `beacon-spawn-available-broadcast` | Language key for the broadcast message. |
| `availability-event.title.enabled` | boolean | `true` | Show a title/subtitle overlay to all players. |
| `availability-event.title.title-key` | string | `beacon-spawn-available-title` | Language key for the title line. |
| `availability-event.title.subtitle-key` | string | `beacon-spawn-available-subtitle` | Language key for the subtitle line. |
| `availability-event.particles.enabled` | boolean | `true` | Spawn a particle burst at the beacon location. |
| `availability-event.fireworks.enabled` | boolean | `true` | Launch fireworks at the beacon location. |

---

### Full spawn example

```yaml
spawn:
  enabled: true
  max-concurrent: 1

  worldguard:
    enabled: true
    radius: 10
    deny-build: true
    deny-pvp: false
    deny-mob-damage: false
    deny-explosions: false

  countdown:
    enabled: true
    duration-seconds: 300
    display-types:
      - ACTION_BAR
      - BOSS_BAR
    format-message: "&5☠ &d&lRevive Beacon &5☠ &r&7 {formatted} until active"
    boss-bar-color: PURPLE
    boss-bar-style: SEGMENTED_20
    name-prefix: "ezls-beacon-"
    per-type-messages: {}
    start-message: ""
    end-message: ""
    end-commands: []
    update-interval-seconds: 1
    visibility-permission: ""
    ephemeral: true

  random-spawn:
    enabled: true
    world: world
    min-x: -2000
    max-x: 2000
    min-z: -2000
    max-z: 2000

  schedule:
    enabled: true
    interval-minutes: 60

  expiry-minutes: 30

  availability-event:
    broadcast:
      enabled: true
      message-key: beacon-spawn-available-broadcast
    title:
      enabled: true
      title-key: beacon-spawn-available-title
      subtitle-key: beacon-spawn-available-subtitle
    particles:
      enabled: true
    fireworks:
      enabled: true
```
