---
title: Heart Items
nav_order: 3
parent: Configuration
description: "Reference for hearts.yml — heart item definitions, field reference, and crafting recipes"
---

# Heart Item Configuration (`hearts.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

Heart items are the in-game representation of hearts. They can be given to players directly, dropped on kill, sold in the shop, and used as revive vouchers. Each heart type is defined as an entry under the top-level `hearts:` key using a unique string ID.

---

## Field Reference

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `display` | string | yes | Display name shown on the item. Supports `&` colour codes. |
| `tier` | integer | yes | Numeric tier used for ordering and comparison (e.g. in the shop). Higher = rarer. |
| `hearts` | number | yes | Heart count granted when this item is consumed. Supports decimals (e.g. `0.5`). |
| `material` | string | yes | Bukkit material name for the item (e.g. `NETHER_STAR`, `EMERALD`). |
| `texture` | string | no | Base64-encoded player skull texture URL. Only used when `material` is `PLAYER_HEAD`. Leave empty to use the plain material. |

### `texture` format

To use a custom skull texture, set `material: PLAYER_HEAD` and provide the Base64-encoded value from a skin database (e.g. minecraft-heads.com). The value is the full Base64 string, **not** a URL:

```yaml
material: PLAYER_HEAD
texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWJjMTIzIn19fQ=="
```

Leave `texture: ""` (or omit it) to use any other material without a custom head.

---

## Default Heart Definitions

The plugin ships with four heart types that demonstrate the full range of configurations:

```yaml
hearts:
  basic:
    display: "&cBasic Heart"
    tier: 1
    hearts: 1.0
    material: NETHER_STAR
    texture: ""

  silver:
    display: "&fSilver Heart"
    tier: 2
    hearts: 2.0
    material: NETHER_STAR
    texture: ""

  gold:
    display: "&6Golden Heart"
    tier: 3
    hearts: 4.0
    material: NETHER_STAR
    texture: ""

  revive:
    display: "&aRevive Voucher"
    tier: 4
    hearts: 1.0
    material: EMERALD
    texture: ""
```

Heart IDs (`basic`, `silver`, `gold`, `revive`) are referenced by `lifesteal-drops.yml`, `shop.yml`, and `revive-beacon.yml`. Renaming an ID requires updating all referencing files.

---

## Recipes

Optional crafting recipes can be defined under a `recipes:` section, keyed by the same heart ID. Each recipe requires a `type` of either `shaped` or `shapeless`.

### Shaped recipe

A shaped recipe arranges ingredients in a specific grid pattern. Use up to 3 rows of 3 characters. Each letter maps to a Bukkit material in the `ingredients` map.

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

### Shapeless recipe

A shapeless recipe accepts any arrangement of the listed ingredients.

```yaml
recipes:
  basic:
    type: shapeless
    amount: 1
    ingredients:
      - IRON_INGOT
      - NETHER_STAR
```

`amount` controls how many heart items are produced by the recipe.
