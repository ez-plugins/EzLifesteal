---
title: EzSeasons
nav_order: 3
parent: Integrations
description: "EzSeasons integration — automatically resets all player hearts when a season ends"
---

# EzSeasons
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

[EzSeasons](https://github.com/gyvex/EzSeasons) is a companion plugin that manages periodic server seasons. When installed alongside EzLifesteal, season-end events automatically trigger a full heart reset for all players — no manual admin command required.

---

## What it enables

- **Automatic heart reset on season end** — when EzSeasons fires a `SeasonEndEvent`, EzLifesteal resets every stored player profile back to the configured `default-hearts` value.

---

## How it works

EzLifesteal registers a Bukkit event listener for `SeasonEndEvent`. When the event fires:

1. EzLifesteal iterates over all stored player profiles.
2. Each profile's heart count is set to the `default-hearts` value defined in `lifesteal-core.yml`.
3. Changes are persisted to the active storage backend (YAML or MySQL).

The reset is equivalent to running `/lifesteal resetall` — it affects every player, including offline ones.

---

## When absent

Season resets must be triggered manually by an admin:

```text
/lifesteal resetall
```

No warnings or errors are logged at startup if EzSeasons is not installed. The integration check is silent.

---

## Setup

1. Install **EzSeasons** on the same server.
2. Configure seasons in EzSeasons as normal.
3. No additional configuration is needed in EzLifesteal. The integration activates automatically when EzSeasons is detected at startup.

---

## Related pages

- [Lifesteal](../features/lifesteal) — heart mechanics and `default-hearts` setting
- [Storage](../features/storage) — how player profiles are persisted
- [Lifesteal core config (`lifesteal-core.yml`)](../config/lifesteal) — `default-hearts` and related keys
