---
title: EzCountdown
nav_order: 5
parent: Integrations
description: "EzCountdown integration — shows a visible countdown overlay during beacon spawn warm-up"
---

# EzCountdown
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

[EzCountdown](https://github.com/gyvex/EzCountdown) is a companion plugin that provides configurable countdown overlays (action bar, boss bar, title screens, etc.). When installed alongside EzLifesteal, the beacon spawn warm-up phase gains a visible in-game countdown for all nearby players.

---

## What it enables

- **Visible countdown overlay during beacon warm-up** — when a beacon enters the `SPAWNED` state, EzLifesteal registers a countdown via `EzCountdownApi`. Players see a configurable action bar, boss bar, or title counting down to the moment the beacon becomes active.

---

## How it works

When a beacon is spawned:

1. EzLifesteal calls `EzCountdownApi.createCountdown(...)` with the duration from `spawn.countdown.duration-seconds`.
2. The countdown name is derived from the beacon's internal ID so it is unique and retrievable.
3. When EzCountdown fires `CountdownEndEvent`, EzLifesteal's listener transitions the beacon from `SPAWNED` to `AVAILABLE`, making it interactable.

The countdown display type (action bar, boss bar, etc.) is configured entirely within EzCountdown, not in EzLifesteal.

---

## When absent

EzLifesteal falls back to an internal Bukkit `BukkitRunnable` that fires after `spawn.countdown.duration-seconds`. The beacon transitions to `AVAILABLE` after the correct delay, but **no visible countdown is shown to players**.

No errors are logged. All other beacon spawn behaviour continues normally.

---

## Setup

1. Install **EzCountdown** on the same server.
2. Configure the duration in `revive-beacon.yml`:

   ```yaml
   spawn:
     countdown:
       enabled: true
       duration-seconds: 60
   ```

3. Configure how the countdown is displayed in EzCountdown's own configuration (display type, format, etc.).

---

## Related pages

- [Beacon Spawn](../features/beacon-spawn) — full beacon lifecycle guide
- [Beacon Spawn config (`revive-beacon.yml`)](../config/revive-beacon#beacon-spawn) — `spawn.countdown` key reference
