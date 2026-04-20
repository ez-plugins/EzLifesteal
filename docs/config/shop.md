---
title: Shop
nav_order: 6
parent: Configuration
description: "Reference for shop.yml — GUI layout, categories, and item entries"
---

# Shop Configuration (`shop.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## GUI Container

- `title`: inventory title
- `size`: inventory slots

## Items List

Each entry in `items:` supports:
- `slot` (required)
- `heart` (required)
- `price`
- `display-name`
- `quantity`
- `icon`
- `lore`
- `commands`

`heart` must match an ID from `hearts.yml`.
