---
title: Revive Beacon
nav_order: 6
parent: Features
description: "Beacon-based revive ritual — voucher requirements, target selection, whitelist, broadcast, and animation"
---

# Revive Beacon
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The revive beacon feature lets a player revive a banned (zero-heart) teammate by holding a special revive voucher item near a placed BEACON block. The revive succeeds when the player completes a timed hold interaction at the beacon.

All settings live in `revive-beacon.yml` under the `revive-beacon:` key. For the companion *beacon spawn* feature (plugin-managed beacon placement), see [Beacon Spawn](beacon-spawn).

---

## Requirements

- `revive-beacon.enabled: true`
- At least one heart type in `hearts.yml` designated as the revive voucher (e.g. `id: revive`).
- A BEACON block placed in the world (manually or via the [Beacon Spawn](beacon-spawn) feature).

---

## Quick setup

```yaml
revive-beacon:
  enabled: true
  voucher-heart-id: revive      # ID from hearts.yml
  require-sneak: false          # shift + right-click to trigger
  max-distance: 8.0             # search radius in blocks
  consume-on-fail: false        # keep the voucher if no target found
  require-voucher-in-beacon: true
  voucher-hold-seconds: 300.0   # 5 minutes of holding to complete
  whitelist-enabled: false
  target-strategy: COMMAND_SELECTION
```

---

## Target strategies

| Strategy | How the target is chosen |
| --- | --- |
| `NEAREST_BANNED` | Picks the nearest banned player within `max-distance`. Ties broken by UUID lexical order. |
| `COMMAND_SELECTION` | The reviver first runs `/revive <player>` to nominate a target, then uses the beacon to confirm. |

`COMMAND_SELECTION` (the default) gives revivers explicit control over who they are reviving, and prevents accidental revives of the wrong player.

---

## Voucher hold mechanic

When `require-voucher-in-beacon: true`, the player must hold the interaction for `voucher-hold-seconds` seconds (displayed via an action bar timer). Progress resets if they move away. This creates a meaningful ritual moment rather than an instant revive.

Set `require-voucher-in-beacon: false` for an instant revive on first right-click.

---

## Beacon whitelist

When `whitelist-enabled: true`, only specific beacon blocks (stored in `revive-beacon-whitelist.yml`) can trigger revives. Manage whitelist entries with admin commands:

```text
/lifesteal beacon add          # whitelist the beacon you are looking at
/lifesteal beacon remove       # remove the beacon you are looking at
/lifesteal beacon list         # view all whitelisted beacons
/lifesteal beacon clear        # remove all whitelist entries
```

Leave `whitelist-enabled: false` to allow any placed beacon.

---

## Broadcast

A server-wide message fires at the start and completion of a revive attempt:

```yaml
broadcast:
  enabled: true
  hold-start-message-key: beacon-revive-broadcast-hold-start
  complete-message-key: beacon-revive-broadcast-complete
```

Message content is defined in `languages/<lang>.yml`.

---

## Revive animation

Visual and audio effects play on the reviver and at the beacon location during the hold and on completion:

```yaml
animation:
  enabled: true
  duration-ticks: 30
  spiral-steps: 10
  ring-count: 3
  vertical-lift-per-step: 0.08
  particles:
    spiral: { type: END_ROD, count: 2, speed: 0.0 }
    ring:   { type: ENCHANT, count: 2, speed: 0.01 }
    impact: { type: TOTEM_OF_UNDYING, count: 20, speed: 0.01 }
  sounds:
    loop:   { type: BLOCK_BEACON_AMBIENT, volume: 0.3, pitch: 1.2 }
    impact: { type: BLOCK_BEACON_ACTIVATE, volume: 1.0, pitch: 1.15 }
```

Set `animation.enabled: false` to skip all effects.

---

## Config reference

- [Revive Beacon (`revive-beacon.yml`)](../config/revive-beacon) — exhaustive key reference including all animation fields
