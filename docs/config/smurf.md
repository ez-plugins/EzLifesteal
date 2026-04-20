---
title: Smurf Detection
nav_order: 7
parent: Configuration
description: "Reference for smurf.yml — kill-farming detection, alert thresholds, and history controls"
---

# Smurf Detection Configuration (`smurf.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

Smurf detection flags players who repeatedly kill the same victim to farm hearts. When a threshold is exceeded, an alert is sent to staff with `lifesteal.alert` permission and an entry is added to the smurf review GUI (`/lifesteal smurf`).

## How detection works

1. Every PvP kill is logged with a timestamp.
2. The plugin counts how many times player A has killed player B within the **rolling** `time-window-minutes`.
3. If that count reaches `same-victim-threshold`, an alert fires and is recorded in the history GUI.
4. Further kills within the same window **do not** generate duplicate alerts for the same victim pair.

---

## Detection Controls

### `enabled`
- Type: boolean
- Default: `true`
- Master switch for smurf detection.

### `same-victim-threshold`
- Type: integer
- Default: `3`
- Number of kills against the same victim inside the time window before an alert is triggered. Raise this value on servers with frequent organised duels to reduce false positives.

### `time-window-minutes`
- Type: integer
- Default: `15`
- Length of the rolling detection window in minutes. Kills older than this window do not count toward the threshold.

### `notify-permission`
- Type: string
- Default: `"lifesteal.alert"`
- Permission node that staff need to receive live smurf alerts.

---

## History Controls

### `history-limit`
- Type: integer
- Default: `50`
- Maximum number of **alert entries** retained in the smurf review GUI (`/lifesteal smurf`). When the cap is reached, the oldest entry is evicted.

### `kill-history-limit`
- Type: integer
- Default: `120`
- Maximum number of **raw kill log entries** retained per player for staff review. This is a higher-resolution log used inside the smurf detail view to show each individual kill in context.

---

## Trust / Exemptions

### `exempted-players`
- Type: list of player names or UUIDs
- Default: `[]`
- Players in this list are completely ignored by smurf detection — neither as suspects nor as victims. Useful for trusted duel partners or automated test accounts.

```yaml
exempted-players:
  - "TrustedFighter"
  - "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

---

## Tuning Tips

- For servers with regular organised PvP events or duel arenas, raise `same-victim-threshold` to `5`–10 and increase `time-window-minutes` to `30`.
- Keep `kill-history-limit` higher than `history-limit` so staff always have detailed context available when reviewing an alert.
