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
- **Start and end broadcasts** — optional server-wide messages sent automatically when the countdown begins and when it finishes.
- **End commands** — a list of console commands dispatched the moment the countdown expires.
- **Visibility scoping** — restrict who sees the overlay via a permission node.
- **Ephemeral mode** — the countdown lives only in memory; no entry is written to EzCountdown's `countdowns.yml`, so server restarts leave no orphaned data.

---

## How it works

When a beacon is spawned:

1. EzLifesteal builds a `CountdownBuilder` from `spawn.countdown` settings (duration, display types, format, boss-bar style, optional messages, commands, visibility permission, and ephemeral flag).
2. The countdown name is derived from the beacon's internal ID (`<name-prefix><beacon-short-id>`) so it is unique and retrievable.
3. When EzCountdown fires `CountdownEndEvent`, EzLifesteal's listener transitions the beacon from `SPAWNED` to `AVAILABLE`, making it interactable.
4. If a duplicate name collision occurs (e.g. from a crash recovery), EzLifesteal logs a warning and reuses the existing countdown by name.

Display type (action bar, boss bar, etc.) and format are configured in `revive-beacon.yml` — EzCountdown forwards them as-is.

---

## When absent

EzLifesteal falls back to an internal Bukkit `BukkitRunnable` that fires after `spawn.countdown.duration-seconds`. The beacon transitions to `AVAILABLE` after the correct delay, but **no visible countdown is shown to players**.

No errors are logged. All other beacon spawn behaviour continues normally.

---

## Setup

1. Install **EzCountdown** on the same server.
2. Configure the countdown in `revive-beacon.yml` under `spawn.countdown`:

   ```yaml
   spawn:
     countdown:
       enabled: true
       duration-seconds: 300
       display-types:
         - ACTION_BAR
         - BOSS_BAR
       format-message: "&5☠ &d&lRevive Beacon &5☠ &r&7 {formatted} until active"
       boss-bar-color: PURPLE
       boss-bar-style: SEGMENTED_20

       # Optional — leave blank to disable
       start-message: "&d☠ A revive beacon has appeared! It will be active in {formatted}."
       end-message: "&d☠ The revive beacon is now active!"
       end-commands: []

       # EzCountdown 2.0.x fields
       update-interval-seconds: 1
       visibility-permission: ""
       ephemeral: true
   ```

---

## Configuration reference

All keys live under `spawn.countdown` in `revive-beacon.yml`.

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | Show an EzCountdown timer during warm-up. |
| `duration-seconds` | integer | `300` | Warm-up time before the beacon becomes interactive. |
| `display-types` | list | `ACTION_BAR`, `BOSS_BAR` | Overlay types. Accepted: `ACTION_BAR`, `BOSS_BAR`, `CHAT`, `TITLE`, `SCOREBOARD`. |
| `format-message` | string | see config | Countdown text. Supports `{formatted}`, `{minutes}`, `{seconds}`, `{hours}`, `{days}`, `{name}`. |
| `boss-bar-color` | string | `PURPLE` | Boss bar colour: `BLUE`, `GREEN`, `PINK`, `PURPLE`, `RED`, `WHITE`, `YELLOW`. |
| `boss-bar-style` | string | `SEGMENTED_20` | Boss bar style: `SOLID`, `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12`, `SEGMENTED_20`. |
| `name-prefix` | string | `ezls-beacon-` | Prefix for the EzCountdown countdown ID. |
| `per-type-messages` | map | `{}` | Per-display-type message overrides (e.g. `ACTION_BAR: "..."` overrides `format-message` for that type only). |
| `start-message` | string | `""` | Server-wide broadcast when the countdown starts. Leave blank to disable. |
| `end-message` | string | `""` | Server-wide broadcast when the countdown ends. Leave blank to disable. |
| `end-commands` | list | `[]` | Console commands dispatched when the countdown ends. `{name}` is replaced with the countdown ID. |
| `update-interval-seconds` | integer | `1` | How often EzCountdown refreshes the display. Increase to reduce tick load on busy servers. |
| `visibility-permission` | string | `""` | Permission node required to see the overlay. Leave blank to show to everyone. |
| `ephemeral` | boolean | `true` | When `true`, the countdown is kept only in memory and never saved to `countdowns.yml`. Recommended for beacon timers. |

---

## Related pages

- [Beacon Spawn](../features/beacon-spawn) — full beacon lifecycle guide
- [Beacon Spawn config (`revive-beacon.yml`)](../config/revive-beacon#beacon-spawn) — `spawn.countdown` key reference
