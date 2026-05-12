---
title: World Scoping
nav_order: 8
parent: Configuration
description: "Reference for lifesteal-worlds.yml — per-world enable/disable rules and gain/loss overrides"
---

# World Scoping Configuration (`lifesteal-worlds.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

`lifesteal-worlds.yml` lets you restrict lifesteal processing to specific worlds and apply different gain/loss values per world.

---

## Options

### `enabled-worlds`

- Type: list of world names
- Default: `[]` (empty)
- If **non-empty**, lifesteal only runs in the listed worlds. All other worlds are silently ignored.
- If **empty**, the `disabled-worlds` list is consulted instead.

### `disabled-worlds`

- Type: list of world names
- Default: `[]` (empty)
- Worlds explicitly excluded from lifesteal processing.
- Only consulted when `enabled-worlds` is empty.

**Priority summary:**

| `enabled-worlds` | `disabled-worlds` | Result |
| --- | --- | --- |
| empty | empty | Lifesteal runs in all worlds |
| non-empty | (ignored) | Only listed worlds run lifesteal |
| empty | non-empty | All worlds except listed ones run lifesteal |

### `world-overrides`

- Type: map keyed by world name
- Default: `{}` (no overrides)
- Each key is a world name. The value is a partial settings map that overrides the global values from `lifesteal-core.yml` for that world only. Unspecified keys fall back to the global value.

Supported override keys:

| Key | Type | Description |
| --- | --- | --- |
| `hearts-per-kill` | number | Overrides global `hearts-per-kill` for this world. |
| `hearts-lost-on-death` | number | Overrides global `hearts-lost-on-death` for this world. |
| `ban-when-zero` | boolean | Overrides global `ban-when-zero-hearts` for this world. |

---

## Example

```yaml
enabled-worlds: []
disabled-worlds:
  - world_nether

world-overrides:
  world_pvp_arena:
    hearts-per-kill: 0.5
    hearts-lost-on-death: 0.5
    ban-when-zero: false
  world_hardcore:
    hearts-per-kill: 2.0
    hearts-lost-on-death: 2.0
    ban-when-zero: true
```

In this example, the Nether is excluded from lifesteal entirely, the PvP arena uses halved rates without banning, and the hardcore world doubles rates and bans at zero.
