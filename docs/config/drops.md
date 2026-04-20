---
title: Heart Drops
nav_order: 4
parent: Configuration
description: "Reference for lifesteal-drops.yml — heart item drop behavior on player kill"
---

# Heart Drop Configuration (`lifesteal-drops.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

`lifesteal-drops.yml` controls whether a physical heart item is dropped into the world (or given directly to the killer) when a player is killed in PvP. This is separate from the gain/loss math in `lifesteal-core.yml`.

---

## Options

### `drop-heart-on-kill`
- Type: boolean
- Default: `true`
- When `true`, a heart voucher item is produced on a PvP kill. When `false`, no item is dropped or given and the section below has no effect.

### `drop-heart-id`
- Type: string
- Default: `"basic"`
- The heart ID that is produced as the dropped item. Must match a key defined in `hearts.yml`.

### `drop-heart-amount`
- Type: integer
- Default: `1`
- Number of heart vouchers produced per kill.

### `drop-heart-naturally`
- Type: boolean
- Default: `true`
- Controls **how** the heart item is delivered:
  - `true` — the item is spawned as a world entity at the victim's death location, like a natural mob drop. Any nearby player (not just the killer) can pick it up.
  - `false` — the item is added directly to the killer's inventory, bypassing the world drop entirely.

---

## Example

```yaml
drop-heart-on-kill: true
drop-heart-id: basic
drop-heart-amount: 1
drop-heart-naturally: true
```
