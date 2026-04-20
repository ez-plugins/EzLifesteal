---
title: Lifesteal Core
nav_order: 5
parent: Configuration
description: "Reference for lifesteal.yml — core bounds, drop settings, mob rules, and world filters"
---

# Lifesteal Core Configuration (`lifesteal.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Core Bounds

- `default-hearts`: starting/reset value.
- `min-hearts`: lower clamp.
- `max-hearts`: upper clamp.

## Gain/Loss Math

- `hearts-per-kill`: gain for killer.
- `hearts-lost-on-death`: loss for victim.

## Zero-heart Policy

- `ban-when-zero-hearts`
- `zero-heart-ban-message`
- `zero-heart-kick-message`
- optional `zero-heart-commands`

## World Scoping

- `enabled-worlds`
- `disabled-worlds`
- `world-overrides` for per-world math differences.

## Additional Controls

- `drop-heart-on-kill`
- `drop-heart-id`
- `drop-heart-amount`
- `drop-heart-naturally`
- `combat-logout-protection.*`
- `heart-consumption-effects.*`
- `mob-rewards`
- `kill-streaks` (see `killstreaks.md`)

## Beacon Revive (`revive-beacon`)

Use a configured heart voucher on a beacon to revive the nearest banned player within range.

```yaml
revive-beacon:
  enabled: true
  voucher-heart-id: revive
  require-sneak: true
  max-distance: 8.0
  consume-on-fail: false
```

Behavior summary:

- If `enabled` is `false`, beacon interactions do not trigger revive logic.
- The held item must be a heart voucher with an id matching `voucher-heart-id`.
- The plugin scans nearby online players and revives the nearest valid banned target.
- On success, default hearts are restored, persisted ban state is removed, and Bukkit bans are pardoned.
- If no valid target is found, no revive is applied and the configured failure path is used.
- If `voucher-heart-id` is missing from `HeartRegistry`, the plugin logs a warning and safely no-ops.
