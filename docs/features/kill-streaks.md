---
title: Kill Streaks
nav_order: 4
parent: Features
description: "Threshold-based kill streak rewards — items, money, commands, and broadcasts on consecutive PvP kills"
---

# Kill Streaks
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The kill streak system tracks each player's consecutive PvP kill count and fires a configurable reward when the count hits a defined threshold. Rewards can include items, in-game currency (via Vault), console commands, personal messages, and server broadcasts.

---

## How the counter works

1. The counter starts at `0` per player.
2. Every PvP kill adds `1`.
3. When the counter **exactly** matches a key in `rewards`, that reward fires.
4. If `reset-on-death: true`, any death resets the counter back to `0`.
5. **Skipped thresholds never fire.** A player who jumps from 4 to 6 in one kill will not trigger a reward defined at `5`.

---

## Enabling kill streaks

In `lifesteal-killstreaks.yml`:

```yaml
kill-streaks:
  enabled: true
  reset-on-death: true
  rewards: {}
```

---

## Defining rewards

Add entries under `rewards` keyed by streak threshold:

```yaml
kill-streaks:
  enabled: true
  reset-on-death: true
  rewards:
    3:
      messages:
        - "&6You're on a &e3-kill streak!"
    5:
      money: 500.0
      items:
        - material: GOLDEN_APPLE
          amount: 2
      broadcast-message: "&6%player% &7is on a &e5-kill streak!"
    10:
      money: 2000.0
      commands:
        - "broadcast &c%player% hit a &e10&c-kill streak!"
      messages:
        - "&aStreak bonus: $2,000!"
```

---

## Available reward types

| Field | Requires | Description |
| --- | --- | --- |
| `money` | Vault + economy plugin | Currency added to the player's balance. |
| `items` | — | Item stacks given to the player (drop at feet if inventory full). Each entry needs `material` and optionally `amount`. |
| `commands` | — | Console commands. Supports `%player%` and `%streak%`. |
| `messages` | — | Private chat messages sent to the streaking player. Supports `&` colour codes. |
| `broadcast-message` | — | Server-wide chat message. Supports `&` colour codes and `%player%` / `%streak%`. |

`money` is the only field that requires Vault. All other reward types work without an economy plugin.

---

## Testing

Use `/lifesteal test kill` on a staging server to simulate PvP kills without needing actual combat. Run it repeatedly with `reset-on-death: false` to walk through your full threshold chain.

---

## Config reference

- [Kill Streaks (`lifesteal-killstreaks.yml`)](../config/killstreaks) — exhaustive key reference with tuning tips
