---
title: Heart Items
nav_order: 2
parent: Features
description: "Defining heart voucher items, heart drops on PvP kill, and optional crafting recipes"
---

# Heart Items
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

Heart items are physical inventory items that represent hearts. A player right-clicks one to consume it and gain the configured heart count. The plugin identifies items by a custom PDC tag (`heart-id`) so renamed or otherwise modified item stacks do not bypass restrictions.

Items are referenced by their string ID across `lifesteal-drops.yml`, `shop.yml`, `revive-beacon.yml`, and the `/lifesteal giveheart` command.

---

## Defining heart types

All heart types are declared in `hearts.yml` under the top-level `hearts:` key:

```yaml
hearts:
  basic:
    display: "&cBasic Heart"
    tier: 1
    hearts: 1.0
    material: NETHER_STAR
    texture: ""

  revive:
    display: "&aRevive Voucher"
    tier: 4
    hearts: 1.0
    material: EMERALD
    texture: ""
```

| Field | Required | Description |
| --- | --- | --- |
| `display` | yes | Item display name. Supports `&` colour codes. |
| `tier` | yes | Numeric tier for ordering (e.g. in the shop). |
| `hearts` | yes | Hearts granted on consumption. Supports decimals. |
| `material` | yes | Bukkit material name. |
| `texture` | no | Base64 skull texture string. Only used when `material: PLAYER_HEAD`. |

Rename or remove a heart ID by updating all files that reference that ID (`drops`, `shop`, `revive-beacon`).

---

## Giving heart items

Use the management command to give items directly to a player:

```text
/lifesteal giveheart <player> <heartId|tier> [amount]
```

Examples:

- `/lifesteal giveheart Alex basic 3` — give Alex 3 basic heart vouchers.
- `/lifesteal giveheart Alex 2` — give Alex a heart voucher whose tier is 2.

---

## Heart drops on kill

Configure `lifesteal-drops.yml` to drop a physical heart item when a player is killed in PvP:

```yaml
drop-heart-on-kill: true
drop-heart-id: basic      # must match a key in hearts.yml
drop-heart-amount: 1
drop-heart-naturally: true  # false = put directly into killer's inventory
```

When `drop-heart-naturally: true`, the item spawns at the victim's death location as a world entity — any player nearby can pick it up, not just the killer.

---

## Crafting recipes

Optional recipes can be defined in `hearts.yml` for any heart ID. Both shaped and shapeless recipes are supported:

```yaml
recipes:
  gold:
    type: shaped
    amount: 1
    pattern:
      - "ABA"
      - "BCB"
      - "ABA"
    ingredients:
      A: GOLD_INGOT
      B: NETHER_STAR
      C: DIAMOND
```

Shapeless example:

```yaml
recipes:
  basic:
    type: shapeless
    amount: 1
    ingredients:
      - IRON_INGOT
      - NETHER_STAR
```

`amount` controls how many heart vouchers the recipe produces.

---

## Config references

- [Heart Items (`hearts.yml`)](../config/hearts) — exhaustive field reference
- [Heart Drops (`lifesteal-drops.yml`)](../config/drops)
