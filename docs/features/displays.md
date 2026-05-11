---
title: Displays
nav_order: 5
parent: Features
description: "Live heart display overlays (action bar and boss bar) and the floating leaderboard hologram"
---

# Displays
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

EzLifesteal provides two types of real-time heart display:

- **Action bar / boss bar** — shows every player their own current heart count.
- **Leaderboard hologram** — a floating text display listing the top-heart players, placed by an admin.

Both are configured in `features.yml`.

---

## Action bar

The action bar overlay sits above the player's hotbar. It refreshes at a configurable interval.

```yaml
action-bar:
  enabled: true
  mode: ACTION_BAR          # ACTION_BAR or BOSS_BAR
  update-interval-ticks: 40 # 20 ticks = 1 second
  message: "&c❤ %hearts% &7hearts"
```

`%hearts%` in the message is replaced with the player's current heart count.

### World filtering

Show the overlay only in certain worlds, or exclude specific worlds:

```yaml
action-bar:
  enabled-worlds:
    - world
    - world_the_end
  disabled-worlds: []
```

`enabled-worlds` takes priority. Leave both lists empty to apply to all worlds.

---

## Boss bar

Switch `mode` to `BOSS_BAR` to display a coloured progress bar at the top of the screen instead of action bar text:

```yaml
action-bar:
  enabled: true
  mode: BOSS_BAR
  update-interval-ticks: 40
  message: "&c❤ %hearts% &7hearts"
  boss-bar:
    color: RED       # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    overlay: NOTCHED_20  # PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20
```

The `message` field is still used as the boss bar title even in `BOSS_BAR` mode.

---

## Leaderboard hologram

The leaderboard hologram displays a live ranking of players by heart count. It floats at a fixed location in a world and updates automatically.

### Placing and removing

The hologram must be placed by an admin at runtime:

```
/lifesteal hologram place    # places at your current location
/lifesteal hologram remove   # removes the hologram
```

These commands require the `lifesteal.scoreboard.place` / `lifesteal.scoreboard.remove` permissions.

The placement location is saved to `features.yml` and persists across reloads. The hologram is recreated on the next server start or `/lifesteal reload`.

### Hologram content

The hologram content and update frequency are configured in `features.yml` under `hologram:`. See the [Feature Display config reference](../config/features) for the full field list.

---

## Config reference

- [Feature Display (`features.yml`)](../config/features) — exhaustive key reference for action bar, boss bar, and hologram
