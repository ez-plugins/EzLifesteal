---
title: Configuration
nav_order: 4
description: "Overview of all EzLifesteal YAML configuration files"
---

# EzLifesteal Configuration Guide
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

This is the high-level map for all EzLifesteal YAML files.

## File Layout

- `config.yml` — language selection and basic global options.
- `storage.yml` — YAML/MySQL backend and MySQL credentials.
- `lifesteal.yml` — core heart gameplay logic and world overrides.
- `features.yml` — action bar / boss bar / hologram display settings.
- `hearts.yml` — heart item definitions and optional crafting recipes.
- `shop.yml` — in-game heart shop inventory layout and purchase actions.
- `admin.yml` — admin detection and bypass toggles.
- `smurf.yml` — suspicious farming detection thresholds/history.
- `languages/*.yml` — text and localization.

## Dedicated Option References

- [Kill streaks reference](./config/killstreaks.md)
- [Lifesteal core reference](./config/lifesteal.md)
- [Storage reference](./config/storage.md)
- [Features reference](./config/features.md)
- [Hearts reference](./config/hearts.md)
- [Shop reference](./config/shop.md)
- [Admin detection reference](./config/admin.md)
- [Smurf detection reference](./config/smurf.md)

## Reload Workflow

1. Edit config files.
2. Save YAML with valid indentation/spaces.
3. Run `/lifesteal reload`.
4. Validate behavior with `/lifesteal test kill` and `/lifesteal test death` in staging.
