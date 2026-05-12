---
title: Kill Streaks
nav_order: 4
parent: Configuration
description: "Reference for lifesteal-killstreaks.yml — kill streak reward definitions and limits"
---

# Kill Streaks Configuration (`lifesteal-killstreaks.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

Kill streak rewards are configured in `lifesteal-killstreaks.yml` under the `kill-streaks` root key.

**Vault and an economy provider are required** if you use the `money` reward field.

---

## How Streaks Are Counted

EzLifesteal tracks each player's current kill streak as an in-memory counter:

1. The counter starts at `0`.
2. Every PvP kill increments it by one.
3. When the counter value exactly matches a key in `rewards`, that reward fires.
4. If `reset-on-death: true`, any death (PvP or otherwise) resets the counter to `0`.
5. Thresholds that are **skipped** do not fire. If rewards are defined at `5` and `10` and a player's counter jumps from `4` to `6` in one kill, neither `5` nor any intermediate value fires — only an entry at `6` would trigger.

> Use `/lifesteal test kill` on a staging server to simulate kills and verify each threshold triggers correctly before going live.

---

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

Each threshold entry in `rewards` can include any combination of the following fields. All fields are optional — define only what you need.

### `money`

- Type: number
- Requires: Vault and a compatible economy plugin
- Amount of in-game currency transferred to the player’s Vault account when the streak threshold is reached.

### `commands`

- Type: list of strings
- Each string is run as a console command when the reward fires. Supports `%player%` and `%streak%` placeholders.

### `items`

- Type: list of item maps
- Items are delivered directly to the player’s inventory. If the inventory is full, items drop at the player’s feet.
- Each entry supports:
  - `material` — Bukkit material name (e.g. `GOLDEN_APPLE`, `DIAMOND`, `EMERALD`)
  - `amount` — number of items in the stack (defaults to `1`)

### `messages`

- Type: list of strings
- Sent as chat messages to the streaking player only. Supports `&` colour codes.

### `broadcast-message`

- Type: string
- Sent as a server-wide chat message visible to all players. Supports `&` colour codes.

### Full Example

```yaml
kill-streaks:
  enabled: true
  reset-on-death: true
  rewards:
    3:
      messages:
        - "&6You're on a &e3 &6kill streak!"
      broadcast-message: "&6%player% &7is warming up..."
    5:
      money: 500.0
      items:
        - material: GOLDEN_APPLE
          amount: 2
      commands:
        - "broadcast &6%player% &7is on a &e%streak%&7 kill streak!"
      messages:
        - "&aStreak bonus: $500 and 2 golden apples!"
    10:
      money: 2000.0
      broadcast-message: "&c%player% &7reached a &e10 &7kill streak!"
      commands:
        - "broadcast &cFirst to stop %player%'s streak earns a bonus!"
```

## Placeholders

Placeholders can be used in `commands`, `messages`, and `broadcast-message`:

| Placeholder | Resolves to |
| --- | --- |
| `%player%` | Display name of the streaking player |
| `%streak%` | Current streak count at the time the reward fires |

## Tuning Tips

- **Start small:** define thresholds at 3, 5, and 10 before adding more. Expand later based on how active PvP is on your server.
- **Economy balance:** keep `money` values proportional to your economy. A streak-3 reward should be clearly worth less than a streak-10 reward to maintain progression incentive.
- **Pair personal and public rewards:** give the streaking player a `messages` line and back it with a `broadcast-message` — but only for noteworthy thresholds (5+). Frequent announcements at low thresholds become noise.
- **Test before going live:** run `/lifesteal test kill` repeatedly on a staging server (with `reset-on-death: false`) to walk through your full threshold chain and confirm every reward fires at the right count.
- **Skipped thresholds fire no reward:** only define reward keys at thresholds you actively want to trigger. If you define `5` and `10` but not `7`, a player at streak 7 receives no reward until they reach 10.
- **Vault is optional:** omitting all `money` fields means Vault is not required at all — items, commands, messages, and broadcasts work without an economy plugin.
