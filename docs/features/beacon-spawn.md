---
title: Beacon Spawn
nav_order: 7
parent: Features
description: "Plugin-managed beacon placement with countdown timer, WorldGuard region protection, random spawn bounds, and auto-schedule"
---

# Beacon Spawn
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The beacon spawn feature places BEACON blocks in the world on a schedule or on demand. A placed beacon goes through a warm-up (countdown) phase before it becomes interactive. When it becomes available, a server-wide availability event fires (broadcast, title, particles, fireworks). Players then use the beacon as a [Revive Beacon](revive-beacon).

The subsystem lives under `revive-beacon.yml → spawn:` and requires `revive-beacon.enabled: true`.

---

## Prerequisites

- `revive-beacon.enabled: true`
- `spawn.enabled: true`
- *(Optional)* **WorldGuard** for automatic region protection.
- *(Optional)* **EzCountdown** for a visible countdown overlay. A silent Bukkit fallback is used when EzCountdown is absent.

---

## Quick setup

```yaml
revive-beacon:
  enabled: true
  spawn:
    enabled: true
    max-concurrent: 1

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

    countdown:
      enabled: true
      duration-seconds: 300

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

---

## Beacon lifecycle

```text
SPAWNED → (countdown) → AVAILABLE → (used or expired) → removed
```

1. **SPAWNED** — beacon block is placed in the world. The countdown starts, and a WorldGuard region is created (if enabled).
2. **AVAILABLE** — countdown ends. The availability event fires. Players can now use the beacon for a revive.
3. **Expired / used** — the beacon is removed after `expiry-minutes` of inactivity, or immediately after a successful revive.

---

## Manual commands

| Command | Description |
| --- | --- |
| `/lifesteal beacon spawn [x y z]` | Spawn a beacon. Uses random-spawn bounds if no coordinates given. |
| `/lifesteal beacon despawn <id\|all>` | Despawn a specific beacon by ID, or all active beacons. |
| `/lifesteal beacon spawns` | List all active plugin-spawned beacons and their current status. |

These commands require `lifesteal.manage.modify` or `lifesteal.admin`.

---

## WorldGuard region protection

When WorldGuard is installed and `worldguard.enabled: true`, a cuboid region is created around the beacon on spawn and deleted on despawn:

```yaml
worldguard:
  enabled: true
  radius: 10
  deny-build: true
  deny-pvp: false
  deny-mob-damage: false
  deny-explosions: false
```

The protection radius is in blocks. Players with admin permissions bypass region flags as usual.

---

## Countdown timer (EzCountdown)

With EzCountdown installed and `countdown.enabled: true`, a live countdown is displayed to all players until the beacon becomes available:

```yaml
countdown:
  enabled: true
  duration-seconds: 300
  display-types:
    - ACTION_BAR
    - BOSS_BAR
```

Accepted display types: `ACTION_BAR`, `BOSS_BAR`, `CHAT`, `TITLE`, `SCOREBOARD`. Falls back silently to an internal Bukkit timer when EzCountdown is absent.

---

## Expiry and concurrent limit

- `expiry-minutes: 30` — removes an available beacon after 30 minutes without a successful revive. Set to `0` to keep indefinitely.
- `max-concurrent: 1` — prevents a new spawn while the active count is at or above this value.

---

## Config reference

- [Revive Beacon (`revive-beacon.yml`) — Beacon Spawn section](../config/revive-beacon#beacon-spawn) — exhaustive per-key reference
