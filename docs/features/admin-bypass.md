---
title: Admin Bypass
nav_order: 9
parent: Features
description: "Admin detection and heart gain/loss bypass — protecting staff from accidental lifesteal changes during testing or moderation"
---

# Admin Bypass
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The admin bypass feature detects staff members and optionally excludes them from heart gain and heart loss. This prevents administrators from accidentally boosting or penalising their own heart count while testing combat, moderating, or running server events.

---

## How admin detection works

A player is treated as an admin if **any** of the following is true:

1. They have operator status and `treat-ops-as-admin: true`.
2. They hold the configured `permission-node` (default: `lifesteal.admin`).
3. Their UUID is in the `allowed-uuids` list.
4. Their name (case-insensitive) is in the `allowed-names` list.

Detection is checked at login and cached for the session. Use `/lifesteal reload` after changing the allow-lists.

---

## Quick setup

In `admin.yml`:

```yaml
enabled: true
treat-ops-as-admin: true
permission-node: "lifesteal.admin"
allowed-uuids: []
allowed-names: []
bypass-heart-loss: true
bypass-heart-gain: true
```

With these defaults, any operator is exempt from both heart gain and loss.

---

## Asymmetric bypass

You can bypass only loss (so admins test freely without getting punished) while still allowing gains:

```yaml
bypass-heart-loss: true
bypass-heart-gain: false
```

Or bypass neither (admins play at full lifesteal risk, detection only used for smurf alert routing):

```yaml
bypass-heart-loss: false
bypass-heart-gain: false
```

---

## Smurf alert routing

Set `restrict-smurf-alerts-to-admins: true` to send smurf detection alerts only to detected admins rather than to every player holding `lifesteal.alert`:

```yaml
restrict-smurf-alerts-to-admins: true
```

When `false` (default), all players with `lifesteal.alert` receive live smurf notifications regardless of admin status.

---

## Config reference

- [Admin Detection (`admin.yml`)](../config/admin) — exhaustive key reference
