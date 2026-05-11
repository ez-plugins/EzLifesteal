---
title: Lifesteal
nav_order: 1
parent: Features
description: "Core lifesteal mechanics — heart gain/loss, per-world rules, zero-heart policy, mob deaths, and combat-logout protection"
---

# Lifesteal
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The lifesteal mechanic awards the killer hearts and deducts them from the victim on every player-versus-player death. All behavior described here is driven by `lifesteal-core.yml` and `lifesteal-worlds.yml`.

---

## How it works

1. Player A kills Player B in PvP.
2. Player A receives `hearts-per-kill` additional hearts (clamped by `max-hearts`).
3. Player B loses `hearts-lost-on-death` hearts.
4. If Player B's hearts reach zero, they are either banned or kicked depending on `ban-when-zero-hearts`.

Heart amounts support decimals for fine-grained balancing (e.g. `0.5` hearts per kill in a soft season).

---

## Enabling lifesteal

Set the master switch in `lifesteal-core.yml`:

```yaml
global-enabled: true
default-hearts: 10
max-hearts: 20
min-hearts: 0
hearts-per-kill: 1.0
hearts-lost-on-death: 1.0
```

`default-hearts` is the starting value for new players and the reset target for `/lifesteal reset <player>`.

---

## Zero-heart policy

When a player's hearts hit zero they can be banned (standard lifesteal) or merely kicked:

```yaml
ban-when-zero-hearts: true
zero-heart-ban-message: "You have run out of hearts."
# Console commands run before the ban/kick — %player%, %killer%, %hearts% supported
zero-heart-commands:
  - "broadcast &c%player% has been eliminated by %killer%!"
```

Set `ban-when-zero-hearts: false` to kick without a game ban (softer season style).

---

## Per-world overrides

Restrict lifesteal to specific worlds or apply different rates per world using `lifesteal-worlds.yml`:

```yaml
# Lifesteal only runs in these worlds
enabled-worlds:
  - world
  - world_nether

# Override rates for a specific world
world-overrides:
  world_nether:
    hearts-per-kill: 2.0
    hearts-lost-on-death: 2.0
```

Leave both `enabled-worlds` and `disabled-worlds` empty to run lifesteal in every world with the global rates.

---

## Mob deaths

By default, dying to a mob does **not** cost hearts — only PvP deaths do. You can change this and optionally protect low-health players:

```yaml
# lifesteal-mobs.yml
dont-remove-hearts-from-mobs: true   # set false to enable mob heart loss
mob-remove-hearts-greater-than: -1   # protect players at or below this value; -1 = off
```

Mob kill rewards (hearts given for killing specific mob types) are configured in the same file. See the [Mob Rewards config reference](../config/mobs) for details.

---

## Combat-logout protection

Prevent players from logging out to dodge heart loss:

```yaml
combat-logout-protection:
  enabled: true
  tag-duration-seconds: 15.0
```

A player who disconnects while combat-tagged will still lose hearts as if they died.

---

## Health scaling

If you want the on-screen heart row to always display at a fixed size regardless of actual max health, enable health scaling:

```yaml
apply-health-scale: true
health-scale: 20.0
```

---

## Config references

- [Lifesteal Core (`lifesteal-core.yml`)](../config/lifesteal) — exhaustive key reference
- [World Scoping (`lifesteal-worlds.yml`)](../config/worlds)
- [Mob Rewards (`lifesteal-mobs.yml`)](../config/mobs)
