---
title: Smurf Detection
nav_order: 8
parent: Features
description: "Kill-farming detection — rolling kill windows, staff alerts, and the smurf review GUI"
---

# Smurf Detection
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

Smurf detection flags players who repeatedly kill the same victim to farm hearts. When a threshold is crossed, online staff with `lifesteal.alert` receive a live chat notification and the event is recorded in a review GUI accessible via `/lifesteal smurf`.

---

## How detection works

1. Every PvP kill is logged with a timestamp.
2. The plugin counts how many times Player A has killed Player B within the rolling `time-window-minutes`.
3. If that count reaches `same-victim-threshold`, an alert fires and is written to the history.
4. Further kills within the same window between the same pair **do not** generate duplicate alerts.

---

## Enabling smurf detection

In `smurf.yml`:

```yaml
enabled: true
same-victim-threshold: 3    # kills before alert fires
time-window-minutes: 15     # rolling window length
notify-permission: lifesteal.alert
```

Assign `lifesteal.alert` to moderators (or include it via `lifesteal.mod`) to receive live notifications.

---

## Smurf review GUI

Run `/lifesteal smurf` (requires `lifesteal.smurf.manage`) to open a paginated list of recorded alerts. Each entry shows:

- Suspect and victim names
- Kill count at alert time
- Timestamp

Clicking an entry opens a detail view with the full kill history between the pair, up to `kill-history-limit` entries. Use this to distinguish organised duels (mutual kills) from one-sided farming (all kills one direction).

---

## History limits

```yaml
history-limit: 50       # max alert entries in the GUI
kill-history-limit: 120 # max raw kill logs per player for detail view
```

Keep `kill-history-limit` higher than `history-limit` so detail views always have full context.

---

## Exemptions

Add trusted players to skip detection entirely:

```yaml
exempted-players:
  - "TrustedPlayer"
  - "a1b2c3d4-e5f6-7890-abcd-ef1234567890"  # UUID preferred
```

Useful for duel-arena regulars, automated test accounts, or event participants.

---

## Tuning guidance

| Server type | Recommended settings |
| --- | --- |
| General SMP | `threshold: 3`, `window: 15` (defaults) |
| Active duel servers | Raise `threshold` to 5–10, `window` to 30–60 |
| Tight competitive seasons | Lower `window` to 10, keep `threshold` at 3 |

---

## Config reference

- [Smurf Detection (`smurf.yml`)](../config/smurf) — exhaustive key reference with full tuning tips and false-positive scenarios
