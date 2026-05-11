---
title: Configuration
nav_order: 6
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
- `lifesteal-core.yml` — core heart bounds, gain/loss math, team-kill behavior, team heart bank settings, ban policy, and combat-logout protection.
- `lifesteal-drops.yml` — heart item drop behavior on player kill.
- `lifesteal-worlds.yml` — per-world enable/disable rules and per-world gain/loss overrides.
- `lifesteal-mobs.yml` — mob death interaction settings and per-mob heart rewards.
- `lifesteal-killstreaks.yml` — kill streak thresholds, rewards, and broadcast messages.
- `features.yml` — action bar / boss bar / hologram display settings.
- `hearts.yml` — heart item definitions and optional crafting recipes.
- `shop.yml` — in-game heart shop inventory layout and purchase actions.
- `admin.yml` — admin detection and bypass toggles.
- `smurf.yml` — suspicious farming detection thresholds/history.
- `revive-beacon.yml` — beacon-based player revive settings (voucher, distance, animation, whitelist, and beacon spawn subsystem).
- `revive-beacon-whitelist.yml` — whitelisted beacon locations (managed at runtime via commands).
- `languages/*.yml` — text and localization.

## Dedicated Option References

- [Lifesteal core reference](./config/lifesteal.md)
- [Heart drops reference](./config/drops.md)
- [World scoping reference](./config/worlds.md)
- [Mob rewards reference](./config/mobs.md)
- [Kill streaks reference](./config/killstreaks.md)
- [Revive beacon reference](./config/revive-beacon.md)
- [Features reference](./config/features.md)
- [Hearts reference](./config/hearts.md)
- [Shop reference](./config/shop.md)
- [Admin detection reference](./config/admin.md)
- [Smurf detection reference](./config/smurf.md)
- [Storage reference](./config/storage.md)

## Reload Workflow

1. Edit config files.
2. Save YAML with valid indentation/spaces.
3. Run `/lifesteal reload`.
4. Validate behavior with `/lifesteal test kill` and `/lifesteal test death` in staging.
