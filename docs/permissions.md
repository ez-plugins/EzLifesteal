---
title: Permissions
nav_order: 5
description: "Permission node reference for EzLifesteal"
---

# EzLifesteal Permissions
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

This file documents every permission node declared by EzLifesteal and how inheritance works.

## Wildcard / Group Permissions

| Node | Default | Includes |
|---|---|---|
| `lifesteal.*` | op | `lifesteal.admin`, `lifesteal.mod`, `lifesteal.player` |
| `lifesteal.player` | true | `lifesteal.command.base`, `lifesteal.top`, `lifesteal.transfer`, `lifesteal.withdraw` |
| `lifesteal.mod` | op | `lifesteal.player`, `lifesteal.alert`, `lifesteal.manage.view`, `lifesteal.smurf.manage`, `lifesteal.scoreboard.*` |
| `lifesteal.admin` | op | `lifesteal.mod`, `lifesteal.manage.*`, `lifesteal.admin.banlist`, `lifesteal.reload`, `lifesteal.test` |
| `lifesteal.manage.*` | op | `lifesteal.manage.view`, `lifesteal.manage.modify`, `lifesteal.manage.resetall` |
| `lifesteal.scoreboard.*` | op | `lifesteal.scoreboard.place`, `lifesteal.scoreboard.remove` |

## Core Permissions

| Node | Default | Purpose |
|---|---|---|
| `lifesteal.command.base` | true | Use `/lifesteal` and basic command entrypoints. |
| `lifesteal.top` | true | View `/lifesteal top` leaderboard. |
| `lifesteal.transfer` | true | Transfer hearts using `/lifesteal transfer`. |
| `lifesteal.withdraw` | true | Withdraw hearts as a voucher using `/lifesteal withdraw`. |

## Management Permissions

| Node | Default | Purpose |
|---|---|---|
| `lifesteal.manage.view` | false | View other players' stored hearts. |
| `lifesteal.manage.modify` | false | Set/add/remove/reset/revive/giveheart operations. |
| `lifesteal.manage.resetall` | false | Reset all stored player profiles. |

## Admin / Feature Permissions

| Node | Default | Purpose |
|---|---|---|
| `lifesteal.reload` | false | Reload plugin config/services. |
| `lifesteal.test` | false | Use `/lifesteal test` simulation commands. |
| `lifesteal.alert` | false | Receive smurf behavior alerts. |
| `lifesteal.smurf.manage` | false | Open/manage smurf GUI workflows. |
| `lifesteal.scoreboard.place` | false | Place leaderboard hologram. |
| `lifesteal.scoreboard.remove` | false | Remove leaderboard hologram. |
| `lifesteal.hologram` | op | Legacy alias for place/remove hologram permissions. |
| `lifesteal.admin.banlist` | false | View the list of banned (zero-heart) players. Included in `lifesteal.admin`. |

## Recommended Role Mapping

- **Players:** `lifesteal.player`
- **Moderators:** `lifesteal.mod`
- **Administrators:** `lifesteal.admin`

If you use a permissions plugin, prefer group-based assignment over direct per-user nodes.
