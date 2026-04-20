---
title: Kill Streaks
nav_order: 4
parent: Configuration
description: "Reference for lifesteal.yml kill-streaks section — reward definitions and limits"
---

# Kill Streaks Configuration (`lifesteal.yml` → `kill-streaks`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

Kill streak rewards are configured under `kill-streaks`.

## Structure

```yaml
kill-streaks:
  enabled: false
  reset-on-death: true
  rewards: {}
```

## Options

### `enabled`
- Type: boolean
- Enables or disables streak tracking and rewards.

### `reset-on-death`
- Type: boolean
- If `true`, a player's current streak resets on death.

### `rewards`
- Type: map keyed by streak threshold (`3`, `5`, `10`, etc.)
- Each threshold can define one or more reward types.

## Reward Entry Fields

```yaml
kill-streaks:
  enabled: true
  reset-on-death: true
  rewards:
    3:
      money: 100.0
      commands:
        - "broadcast &6%player% &7reached a &e3&7 kill streak!"
      items:
        - material: GOLDEN_APPLE
          amount: 2
      messages:
        - "&aKill streak %streak%! Enjoy the bonus."
      broadcast-message: "&6%player% &7is on a &e%streak% &7kill streak!"
```

Supported reward fields:

- `money` (number)
  - Requires Vault + economy provider.
- `commands` (list of strings)
  - Executed as console.
- `items` (list)
  - Each item supports `material`, `amount`.
- `messages` (list of strings)
  - Sent to the streaking player.
- `broadcast-message` (string)
  - Server-wide announcement.

## Placeholders

Common placeholders for reward text/commands include:
- `%player%`
- `%streak%`

## Tuning Tips

- Start with low thresholds (3/5/10) and increase only if PvP is frequent.
- Keep money rewards modest to avoid inflation.
- Combine one personal reward (`messages`/`items`) with one server signal (`broadcast-message`).
