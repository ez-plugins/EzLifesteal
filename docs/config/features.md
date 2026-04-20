---
title: Feature Display
nav_order: 2
parent: Configuration
description: "Reference for features.yml — action bar, boss bar, and hologram display settings"
---

# Feature Display Configuration (`features.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Action Bar / Boss Bar

Controls the live heart count overlay shown to players.

### `action-bar.enabled`
- Type: boolean
- Default: `true`
- Toggle the overlay on or off globally.

### `action-bar.mode`
- Type: string
- Default: `ACTION_BAR`
- Controls display style. Accepted values:
  - `ACTION_BAR` — displays a text message above the hotbar.
  - `BOSS_BAR` — displays a progress bar at the top of the screen. Requires the `boss-bar` sub-section.

### `action-bar.update-interval-ticks`
- Type: integer
- Default: `40`
- How often (in server ticks, 20 ticks = 1 second) the display refreshes. Lower values are smoother but add overhead.

### `action-bar.message`
- Type: string
- Default: `"&c❤ %hearts% &7hearts"`
- Text shown when `mode` is `ACTION_BAR`. Supports `&` colour codes.
- Available placeholder: `%hearts%` — the player's current heart count.

### `action-bar.enabled-worlds` / `action-bar.disabled-worlds`
- Type: list of world names
- Default: `[]` (empty)
- Leave both lists empty to show the overlay in all worlds.
- If `enabled-worlds` is non-empty, only worlds in that list receive the overlay.
- If `disabled-worlds` is non-empty, worlds in that list are excluded.
- `enabled-worlds` takes priority over `disabled-worlds` if both are set.

### Boss bar options

Only read when `action-bar.mode` is `BOSS_BAR`.

#### `action-bar.boss-bar.color`
- Type: string
- Default: `RED`
- Controls bar colour. Accepted values: `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`.

#### `action-bar.boss-bar.overlay`
- Type: string
- Default: `PROGRESS`
- Controls bar segment style. Accepted values:
  - `PROGRESS` — smooth fill bar (no segments)
  - `NOTCHED_6` — 6 segments
  - `NOTCHED_10` — 10 segments
  - `NOTCHED_12` — 12 segments
  - `NOTCHED_20` — 20 segments

### Annotated example

```yaml
action-bar:
  enabled: true
  mode: BOSS_BAR
  update-interval-ticks: 40
  message: "&c❤ %hearts% &7hearts"
  enabled-worlds: []
  disabled-worlds:
    - world_nether
  boss-bar:
    color: RED
    overlay: NOTCHED_20
```

---

## Hologram Leaderboard

A floating text display showing the top-hearts ranking. Requires the hologram to be placed via `/lifesteal hologram place` before it appears.

### `hologram.update-interval-ticks`
- Type: integer
- Default: `600`
- How often the ranking text refreshes (600 ticks = 30 seconds).

### `hologram.max-entries`
- Type: integer
- Default: `10`
- Maximum number of players shown in the leaderboard.

### `hologram.location`
- Type: map
- Default: `{}` (not placed)
- Set automatically when a staff member runs `/lifesteal hologram place`. You can also set it manually:

```yaml
hologram:
  update-interval-ticks: 600
  max-entries: 10
  location:
    world: world
    x: 0.5
    y: 64.0
    z: 0.5
    yaw: 0.0
    pitch: 0.0
```

Leave `location: {}` to have no hologram until the place command is run.
