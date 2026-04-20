---
title: Shop
nav_order: 6
parent: Configuration
description: "Reference for shop.yml — GUI layout, item entries, pricing, and purchase actions"
---

# Shop Configuration (`shop.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The shop GUI opens when a player runs `/lifesteal shop` or `/hearts`. Items are laid out by slot number in a chest inventory.

**Vault and an economy provider are required** to use the `price` field. Items without a price (or with `price: 0`) are free.

---

## GUI Container

### `title`
- Type: string
- Display name of the inventory window. Supports `&` colour codes.

### `size`
- Type: integer
- Number of inventory slots. Must be a multiple of 9 (9, 18, 27, 36, 45, or 54).

---

## Item Entry Fields

Each entry in the `items:` list represents one purchasable slot.

| Field | Type | Required | Description |
|---|---|---|---|
| `slot` | integer | yes | Inventory slot index (0-based, left-to-right, top-to-bottom). Max slot is `size - 1`. |
| `heart` | string | yes | The heart ID to give on purchase. Must match a key defined in `hearts.yml`. |
| `price` | number | no | Cost in the server's Vault economy. `0` or omitted means free. |
| `display-name` | string | no | Override for the item's display name. Supports `&` colour codes. |
| `quantity` | integer | no | Number of heart vouchers given per purchase. Defaults to `1`. |
| `icon` | string | no | Bukkit material for the inventory icon (e.g. `PAPER`, `GOLD_INGOT`, `EMERALD`). Overrides the material from `hearts.yml`. |
| `lore` | list of strings | no | Description lines shown on the item. Supports `&` colour codes. |
| `commands` | list of strings | no | Console commands run after a successful purchase. Use `{player}` for the buyer's name. |

---

## Full Example

```yaml
title: "&cHeart Shop"
size: 54

items:
  - slot: 10
    heart: basic
    price: 10000000.0
    display-name: "&aBasic Heart Voucher"
    quantity: 1
    icon: PAPER
    lore:
      - "&7Gives 1 heart voucher"
    commands:
      - "say {player} bought a basic heart"

  - slot: 11
    heart: silver
    price: 25000000.0
    display-name: "&bSilver Heart Voucher"
    quantity: 2
    icon: PAPER
    lore:
      - "&7Gives 2 heart vouchers"
    commands:
      - "say {player} bought a silver heart"

  - slot: 12
    heart: gold
    price: 50000000.0
    display-name: "&6Golden Heart Voucher"
    quantity: 4
    icon: GOLD_INGOT
    lore:
      - "&7Gives 4 heart vouchers"
    commands:
      - "say {player} bought a golden heart"

  - slot: 13
    heart: revive
    price: 75000000.0
    display-name: "&aRevive Voucher"
    quantity: 1
    icon: EMERALD
    lore:
      - "&7Revives a banned player at a beacon"
    commands:
      - "say {player} bought a revive voucher"
```
