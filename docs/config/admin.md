---
title: Admin Detection
nav_order: 1
parent: Configuration
description: "Reference for admin.yml — admin detection methods and heart gain/loss bypass controls"
---

# Admin Detection Configuration (`admin.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

`admin.yml` lets you prevent staff members from unintentionally affecting their lifesteal progression while testing or moderating. The plugin detects admins via a combination of operator status, a permission node, and explicit allow-lists.

---

## Options

### `enabled`

- Type: boolean
- Default: `true`
- Master switch. Set to `false` to disable all admin detection and bypass logic.

### `treat-ops-as-admin`

- Type: boolean
- Default: `true`
- When `true`, any player with server-operator status is automatically treated as an admin, regardless of permissions or allow-lists.

### `permission-node`

- Type: string
- Default: `"lifesteal.admin"`
- Players holding this permission are treated as admins. Set to an empty string to disable permission-based detection.

### `allowed-uuids`

- Type: list of UUID strings
- Default: `[]`
- Explicit UUID allow-list. These players are always treated as admins. UUIDs must be in the standard format `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`.

```yaml
allowed-uuids:
  - "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

### `allowed-names`

- Type: list of strings
- Default: `[]`
- Case-insensitive player name allow-list. Detection happens at login, so name changes require a restart or reload to take effect. Prefer UUIDs for reliability.

### `bypass-heart-loss`

- Type: boolean
- Default: `true`
- When `true`, detected admins do **not** lose hearts when killed in PvP. Useful for staff testing combat without penalising themselves.

### `bypass-heart-gain`

- Type: boolean
- Default: `true`
- When `true`, detected admins do **not** gain hearts when they kill another player. This prevents admins from boosting their count through test kills.
- You can set `bypass-heart-loss: true` and `bypass-heart-gain: false` independently if you want asymmetric behaviour (e.g. admins can gain but not lose).

### `restrict-smurf-alerts-to-admins`

- Type: boolean
- Default: `false`
- When `true`, smurf detection alerts are only sent to players who are detected as admins (and hold the notify permission). Moderators without admin status will not receive alerts.
- When `false`, the normal `lifesteal.alert` permission controls who receives alerts.
