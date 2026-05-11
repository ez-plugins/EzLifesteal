---
title: Shop
nav_order: 3
parent: Features
description: "Heart voucher shop — enabling the GUI, requiring Vault, and configuring purchasable slots"
---

# Shop
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The heart shop is a chest-based GUI where players spend in-game currency (via Vault) on heart voucher items. It opens with `/lifesteal shop` or `/hearts`.

---

## Requirements

- **Vault** and a compatible economy plugin are required when any shop item has `price > 0`.
- If Vault is absent, the shop still opens but all `price` values are effectively ignored (items are free). Install Vault if you want paid purchases.

---

## Quick setup

1. Define at least one heart type in `hearts.yml` (see [Heart Items](heart-items)).
2. Open `shop.yml` and configure the GUI title, size, and item slots:

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
      - "&7Grants &c1 heart&7."
      - "&7Cost: &e$10,000,000"

  - slot: 11
    heart: gold
    price: 50000000.0
    display-name: "&6Golden Heart Voucher"
    quantity: 1
    icon: GOLD_INGOT
    lore:
      - "&7Grants &c4 hearts&7."
      - "&7Cost: &e$50,000,000"
```

---

## Purchase flow

1. Player opens the shop.
2. Player left-clicks a slot.
3. Plugin checks the player's Vault balance if `price > 0`.
4. On success, the heart vouchers are added to the player's inventory (or dropped at their feet if full).
5. Any configured `commands` entries execute as console commands with `{player}` replaced.
6. The GUI stays open for follow-up purchases.

---

## Slot layout reference

`size` must be a multiple of 9 (9–54). Slots are numbered 0-based, left-to-right, top-to-bottom:

```
 0  1  2  3  4  5  6  7  8
 9 10 11 12 13 14 15 16 17
18 ...
```

Decorative filler items (no `heart` field) are not supported natively; use a filler heart definition with `hearts: 0` if you need visual spacers.

---

## Post-purchase commands

Run arbitrary console commands after a successful purchase:

```yaml
  - slot: 12
    heart: silver
    price: 25000000.0
    commands:
      - "broadcast {player} bought a Silver Heart!"
      - "eco give {player} 1000"
```

Use `{player}` for the buyer's in-game name.

---

## Config reference

- [Shop (`shop.yml`)](../config/shop) — exhaustive field reference
