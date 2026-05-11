---
title: Home
nav_order: 1
description: "EzLifesteal — heart-based lifesteal plugin for Minecraft servers"
permalink: /
---

# EzLifesteal
{: .no_toc }

EzLifesteal is a **heart-based lifesteal plugin** for Paper / Bukkit-compatible
Minecraft servers. Players gain or lose hearts when they kill or are killed by other
players. The plugin supports YAML and MySQL persistence, a configurable heart shop,
kill streaks, smurf detection, and optional EzSeasons integration for seasonal resets.

---

## Feature overview

| Feature | Details |
|---|---|
| Lifesteal on kill/death | Configurable hearts gained/lost per PvP interaction |
| Per-world overrides | Different lifesteal rates per world |
| Ban on zero hearts | Optionally bans players who reach zero hearts |
| Heart items & shop | Configurable heart voucher items and a GUI shop |
| Kill streaks | Rewards and broadcasts triggered by consecutive kills |
| Leaderboard hologram | Floating hologram displaying the top-heart players |
| Action bar / boss bar | Real-time heart display overlays |
| Revive beacon | Beacon-based ritual to revive banned players |
| Beacon spawn | Plugin places beacon blocks in the world on a schedule or manually, with countdown timer, WorldGuard region protection, and availability broadcast |
| Smurf detection | Flags suspicious kill-farming behavior with alerts and GUI review |
| Admin bypass | Optionally exempt admins from heart gain/loss |
| Multi-language support | `en`, `de`, `es`, `fr`, `nl`, `pt`, `ru`, `zh` |
| YAML / MySQL storage | Pluggable persistence with async I/O |
| EzSeasons integration | Automatically resets hearts when a season ends |

---

## Compatibility

| Requirement | Version |
|---|---|
| Minecraft / Paper | 26.1 or later |
| Java | 25 or later |
| Plugin version | 1.1.0 |

---

## Quick navigation

- [Getting started](getting-started) — install and first-time setup
- [Features](features) — per-feature guides (lifesteal, shop, kill streaks, revive beacon, and more)
- [Integrations](integrations) — optional hooks for Vault, PlaceholderAPI, EzSeasons, WorldGuard, and EzCountdown
- [Configuration](configuration) — exhaustive YAML file reference
- [Commands](commands) — all `/lifesteal` and `/hearts` commands
- [Permissions](permissions) — permission node reference
- [Developer guide](developer-guide) — architecture, contribution workflow, and API integration
