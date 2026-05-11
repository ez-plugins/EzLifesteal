---
title: Commands
nav_order: 5
description: "Reference for all EzLifesteal commands and subcommands"
---

# EzLifesteal Commands
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

This page lists all player, moderator, and admin commands exposed by EzLifesteal.

## Base Commands

| Command | Description | Permission |
|---|---|---|
| `/lifesteal` | Opens the main command and shows your current heart status context. | `lifesteal.command.base` |
| `/hearts` | Opens the heart voucher shop GUI. | `lifesteal.command.base` |

## Player Commands

| Command | Description | Permission |
|---|---|---|
| `/lifesteal top [page]` | Shows the heart leaderboard with pagination. | `lifesteal.top` |
| `/lifesteal transfer <player> <amount>` | Transfers hearts to another player (respecting minimum constraints). | `lifesteal.transfer` |
| `/lifesteal withdraw <amount>` | Withdraws hearts from your own count as a heart voucher item. | `lifesteal.withdraw` |
| `/lifesteal teambank balance` | Shows your current team heart bank balance. | `lifesteal.teambank.balance` |
| `/lifesteal teambank deposit <amount>` | Deposits hearts from your profile into your team's shared bank. | `lifesteal.teambank.deposit` |
| `/lifesteal teambank withdraw <amount>` | Withdraws hearts from your team's shared bank into your profile. | `lifesteal.teambank.withdraw` |
| `/lifesteal shop` | Opens the heart voucher shop GUI (same as `/hearts`). | `lifesteal.command.base` |

## Management Commands

| Command | Description | Permission |
|---|---|---|
| `/lifesteal hearts <player>` | View stored hearts for any player (online/offline). | `lifesteal.manage.view` |
| `/lifesteal set <player> <hearts>` | Set exact heart amount for a player. | `lifesteal.manage.modify` |
| `/lifesteal add <player> <hearts>` | Add hearts to a player. | `lifesteal.manage.modify` |
| `/lifesteal remove <player> <hearts>` | Remove hearts from a player. | `lifesteal.manage.modify` |
| `/lifesteal reset <player>` | Reset one player to default hearts. | `lifesteal.manage.modify` |
| `/lifesteal revive <player>` | Revive a player and restore default heart state. | `lifesteal.manage.modify` |
| `/lifesteal giveheart <player> <heartId\|tier> [amount]` | Give configured heart items directly. | `lifesteal.manage.modify` |
| `/lifesteal resetall` | Reset all stored profiles to default hearts. | `lifesteal.manage.resetall` |

## Moderation / Staff Commands

| Command | Description | Permission |
|---|---|---|
| `/lifesteal smurf` | Opens the smurf detection review GUI. | `lifesteal.smurf.manage` |
| `/lifesteal hologram place` | Places the top-heart hologram at your location. | `lifesteal.scoreboard.place` |
| `/lifesteal hologram remove` | Removes the top-heart hologram. | `lifesteal.scoreboard.remove` |

## Beacon Commands

These subcommands manage both whitelisted beacon blocks and plugin-spawned beacons.

| Command | Description | Permission |
|---|---|---|
| `/lifesteal beacon add` | Whitelist the beacon block you are looking at. | `lifesteal.manage.modify` |
| `/lifesteal beacon remove` | Remove the beacon block you are looking at from the whitelist. | `lifesteal.manage.modify` |
| `/lifesteal beacon list` | List all whitelisted beacon blocks and revive beacon settings. | `lifesteal.manage.modify` |
| `/lifesteal beacon clear` | Clear all whitelisted beacon blocks. | `lifesteal.manage.modify` |
| `/lifesteal beacon spawn [x y z]` | Spawn a beacon block. Uses `random-spawn` bounds from config if no coordinates are given. | `lifesteal.manage.modify` |
| `/lifesteal beacon despawn <id\|all>` | Despawn a tracked spawned beacon by ID, or `all` to remove every active beacon. | `lifesteal.manage.modify` |
| `/lifesteal beacon spawns` | List all currently active plugin-spawned beacons and their status. | `lifesteal.manage.modify` |

`/lifesteal beacon spawn` / `despawn` / `spawns` require `spawn.enabled: true` in `revive-beacon.yml`.

## Admin / Maintenance Commands

| Command | Description | Permission |
|---|---|---|
| `/lifesteal reload` | Reloads plugin config/runtime services. | `lifesteal.reload` |
| `/lifesteal test <kill\|death>` | Simulates kill/death lifesteal flows for validation. | `lifesteal.test` |

## Standalone Commands

| Command | Description | Permission |
|---|---|---|
| `/revive <player>` | Pre-selects a revive target for the next beacon interaction. Required when `target-strategy` is set to `COMMAND_SELECTION` in `revive-beacon.yml`. | `lifesteal.command.base` |

## Usage Notes

- Assign `lifesteal.player` to normal players.
- Assign `lifesteal.mod` to moderators.
- Assign `lifesteal.admin` to server administrators.
- Use `/lifesteal reload` after editing YAML files.
- Team bank commands require `team-bank.enabled: true` and TeamsAPI availability.
