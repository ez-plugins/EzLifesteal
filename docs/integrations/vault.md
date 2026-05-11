---
title: Vault
nav_order: 1
parent: Integrations
description: "Vault economy integration — enables shop pricing and kill streak money rewards"
---

# Vault
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

[Vault](https://www.spigotmc.org/resources/vault.34315/) is an economy, permissions, and chat API for Bukkit plugins. EzLifesteal uses Vault's economy layer to support real in-game currency for the shop and kill streak money rewards.

---

## What it enables

- **Paid shop items** — set a `price` on any [Shop](../features/shop) entry. Without Vault, setting a price has no effect and all items are offered for free.
- **Money kill streak rewards** — award in-game currency through the `money` field in a [Kill Streak](../features/kill-streaks) reward entry.

---

## How it works

On server start EzLifesteal queries the Vault service registry for an `Economy` provider. Any economy plugin that ships a Vault adapter is compatible, including:

- EssentialsX
- CMI
- TheNewEconomy (TNE)
- GoldAndEconomy, and others

EzLifesteal does not depend on a specific economy plugin — only on the Vault API layer.

---

## When absent

| Feature | Fallback behaviour |
|---|---|
| Shop item with `price` set | Item is offered for free |
| Kill streak `money` reward | Reward entry is silently ignored |

No errors are logged. The shop and kill streak features continue to work; only the economy-dependent parts are disabled.

---

## Setup

1. Download and install **Vault** on your server.
2. Install a Vault-compatible economy plugin (e.g. EssentialsX).
3. Add a `price` to one or more shop entries in `shop.yml`:

```yaml
slots:
  - slot: 11
    heart: standard
    price: 500.0
    amount: 1
```

4. Or add a `money` reward in `lifesteal-killstreaks.yml`:

```yaml
streaks:
  - threshold: 5
    rewards:
      - type: money
        amount: 250.0
```

---

## Related pages

- [Shop](../features/shop) — full shop setup guide
- [Kill Streaks](../features/kill-streaks) — reward type reference
- [Shop config (`shop.yml`)](../config/shop) — exhaustive key reference
- [Kill Streak config (`lifesteal-killstreaks.yml`)](../config/killstreaks) — full reward-type list
