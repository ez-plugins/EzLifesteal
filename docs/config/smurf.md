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

### What happens when an alert fires

When the threshold is exceeded:

1. All online players holding `lifesteal.alert` are sent a chat notification naming the suspect, the victim, and the current kill count. If `restrict-smurf-alerts-to-admins: true` is set in `admin.yml`, only players who also qualify as admins receive the notification.
2. The event is written to the alert history, accessible via `/lifesteal smurf`.
3. No duplicate alert fires for the same killer/victim pair until the time window resets and kills accumulate past the threshold again.

---

## Smurf Review GUI (`/lifesteal smurf`)

Run `/lifesteal smurf` (requires `lifesteal.smurf.manage`) to open a paginated inventory list of recorded alerts. Each entry shows the suspect, the victim, the kill count at alert time, and the timestamp when it was triggered.

Clicking an entry opens a detail view with the full recorded kill history between the pair, up to `kill-history-limit` entries. Use this to distinguish legitimate repetitive duels (both players have kills against each other) from one-sided farming.

**Investigation checklist:**

- Is the kill pattern one-sided (all kills A → B, none B → A)? Strong farming signal.
- Are kills happening in tight bursts within seconds? Could indicate teleport or exploit abuse.
- Have kills occurred across multiple sessions or days? May be legitimate rivalry rather than deliberate farming.

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

- **General SMP:** keep the defaults (`same-victim-threshold: 3`, `time-window-minutes: 15`). They catch repeated farming without alerting on normal back-and-forth duels.
- **Duel servers / frequent 1v1 play:** raise `same-victim-threshold` to `5`–`10` and `time-window-minutes` to `30`–`60` to avoid alert floods from organised matches.
- **Tight competitive seasons:** lower `time-window-minutes` to `10` and keep the threshold at `3` for stricter farming protection.
- Always keep `kill-history-limit` higher than `history-limit` so staff have full kill context when reviewing an alert.
- Give moderators both `lifesteal.alert` and `lifesteal.smurf.manage` so they receive live alerts and can investigate via the GUI.

---

## Common False-Positive Scenarios

| Scenario | Symptom | Recommended fix |
| --- | --- | --- |
| Organised duels / duel arena | Frequent alerts between the same two players | Raise `same-victim-threshold` to 5+ or add participant UUIDs to `exempted-players` |
| Tournament or scheduled server event | Alert storm during the event | Add all participants to `exempted-players` for the duration |
| Very busy server with low threshold | Too many alerts to act on meaningfully | Raise `same-victim-threshold` and/or increase `time-window-minutes` |
| Automated test accounts | Alerts generated by scripted test kills | Add the test account UUIDs to `exempted-players` |
| Known friendly rivalry | Two regulars who always fight each other | Add both UUIDs to `exempted-players` if both players consent |
