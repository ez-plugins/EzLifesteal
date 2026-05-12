---
title: Mob Rewards
nav_order: 9
parent: Configuration
description: "Reference for lifesteal-mobs.yml — mob death behavior and per-mob heart rewards"
---

# Mob Rewards Configuration (`lifesteal-mobs.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

`lifesteal-mobs.yml` controls two things: how mob deaths interact with the lifesteal heart system, and optional heart rewards for players who kill specific mob types.

---

## Mob Death Behavior

### `dont-remove-hearts-from-mobs`

- Type: boolean
- Default: `true`
- When `true`, dying to a mob (non-player) **does not** remove hearts from the victim. Only PvP deaths trigger heart loss.
- Set to `false` to make mob deaths also cost hearts.

### `mob-remove-hearts-greater-than`

- Type: integer
- Default: `-1` (disabled)
- Only used when `dont-remove-hearts-from-mobs` is `false`. When set to a positive integer, heart removal from mob deaths only applies to players whose heart count is **greater than** this value. For example, `mob-remove-hearts-greater-than: 5` means players with 5 or fewer hearts are protected from mob heart loss.
- Set to `-1` to disable the threshold (all players lose hearts from mobs equally).

---

## Mob Rewards

### `mob-rewards`

- Type: map keyed by entity type name
- Default: `{}` (no rewards)
- Award hearts to a player when they kill a specific mob type. Entity type names are Bukkit entity type strings (e.g. `zombie`, `skeleton`, `ender_dragon`).

Each entry supports:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `hearts` | number | yes | Hearts granted to the player on kill. |
| `permission` | string | no | Permission node required to receive the reward. Omit to grant to all players. |
| `worlds` | list of strings | no | Worlds where the reward applies. Empty = all worlds. |
| `blocked-worlds` | list of strings | no | Worlds where the reward is suppressed. |

---

## Example

```yaml
dont-remove-hearts-from-mobs: true
mob-remove-hearts-greater-than: -1

mob-rewards:
  zombie:
    hearts: 0.25
    permission: lifesteal.mob.zombie
    worlds:
      - world
    blocked-worlds:
      - world_nether
  ender_dragon:
    hearts: 5.0
```

In this example, killing a Zombie in `world` gives 0.25 hearts (requires the `lifesteal.mob.zombie` permission and does not trigger in the Nether). Killing the Ender Dragon gives 5 hearts in any world.
