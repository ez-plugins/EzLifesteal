---
title: Integrations
nav_order: 4
has_children: true
description: "Optional plugin integrations — Vault, PlaceholderAPI, EzSeasons, WorldGuard, and EzCountdown"
---

# Integrations
{: .no_toc }

---

All EzLifesteal integrations are **optional**. The plugin starts and runs normally even if none of the integrated plugins are installed. Missing dependencies disable only the functionality that depends on them; all other features continue to work.

Install any of the following plugins alongside EzLifesteal to unlock the corresponding features.

---

## Integration overview

| Integration | Enables | Behaviour when absent |
|---|---|---|
| [Vault](vault) | Shop pricing, kill streak money rewards | Items are free; money rewards ignored |
| [PlaceholderAPI](placeholderapi) | Heart placeholders in other plugins | Placeholders remain unresolved |
| [EzSeasons](ezseasons) | Automatic heart reset on season end | Manual `/lifesteal resetall` required |
| [WorldGuard](worldguard) | Region protection around spawned beacons | No region created (no protection) |
| [EzCountdown](ezcountdown) | Visible countdown for beacon warm-up | Silent internal timer used |
