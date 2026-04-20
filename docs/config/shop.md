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

## Purchase Flow

1. Player opens the shop via `/lifesteal shop` or `/hearts`.
2. The GUI renders item icons at the configured slots with their display names and lore.
3. Player left-clicks a slot:
   - If `price > 0`, the plugin checks the player’s Vault balance. If they cannot afford it, the purchase is cancelled and they receive a feedback message.
   - If affordable (or free), `quantity` heart vouchers are added to their inventory. If the inventory is full, the vouchers drop at their feet.
4. `commands` entries execute as console commands, with `{player}` replaced by the buyer’s in-game name.
5. The GUI stays open after a purchase so players can buy multiple items in one session.

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

---

## Tips

- **Slot numbering:** slots are numbered left-to-right, top-to-bottom starting at `0`. In a `size: 54` chest, slot `4` is the centre of the top row and slot `49` the centre of the bottom row. A common pattern puts purchasable items in rows 2–4 with the outer border left empty.
- **Free items:** set `price: 0` or omit `price` entirely to make an item cost nothing. Useful for event giveaways or starter kits delivered via `commands`.
- **Post-purchase hooks:** use `commands` to send purchase announcements, trigger economy logging via a bridge plugin, or run any other console action. The `{player}` token is replaced with the buyer’s name at execution time.
- **Per-item icons:** use `icon` to override the visual displayed in the GUI without affecting which heart type is delivered. This lets you display a `GOLD_INGOT` for a gold-tier heart even if the heart item itself uses a skull texture.
- **Testing prices:** temporarily set `price: 0` on an item to verify the purchase flow and inventory delivery before committing to final economy values.
