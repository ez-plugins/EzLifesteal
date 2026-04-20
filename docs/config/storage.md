---
title: Storage
nav_order: 8
parent: Configuration
description: "Reference for storage.yml — YAML and MySQL backend configuration"
---

# Storage Configuration (`storage.yml`)
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Backend Selection

```yaml
type: YAML # or MYSQL
```

- `YAML`: stores profile and ban records in plugin data files.
- `MYSQL`: stores profile and ban records in configured SQL tables.

## YAML Layout

When `type: YAML` is enabled, storage is split into per-player files:

- `data/players/<uuid>.yml`: profile-only data (currently `hearts`).
- `data/bans/<uuid>.yml`: ban persistence with the following keys:
  - `name`
  - `reason`
  - `source`
  - `created-at`
  - `expires-at`
  - `active`

Ban files are independent from profile files so heart/profile migrations do not modify ban state.

## MySQL Block

```yaml
mysql:
  host: localhost
  port: 3306
  database: lifesteal
  username: root
  password: password
  use-ssl: false
  table: lifesteal_players
```

With `table: lifesteal_players`, the plugin uses:

- `lifesteal_players`: profile records.
- `lifesteal_players_bans`: ban records.

Ban table schema:

- `uuid CHAR(36) PRIMARY KEY`
- `player_name VARCHAR(16)`
- `reason TEXT`
- `source VARCHAR(64)`
- `created_at TIMESTAMP`
- `expires_at TIMESTAMP NULL`
- `active BOOLEAN NOT NULL`
- Index on `(active, created_at)` for admin listing / reconciliation queries.

## Startup Reconciliation & Migration Safety

- Existing hearts data remains unchanged during ban-storage upgrades.
- On startup, active repository bans are applied to Bukkit's runtime ban list.
- If Bukkit already has runtime bans that are missing from storage, they are imported once at startup (only when no repository record exists for that UUID).
- `/lifesteal revive <player>` clears both persisted ban storage and Bukkit ban state to prevent stale re-bans after restart.

Operator notes:

- Keep storage backend consistent during migration windows (avoid switching backend repeatedly while bans are changing).
- After changing backends, restart once and verify ban counts with `/lifesteal banlist`.
- Keep database users scoped to least privilege and back up `data/` or SQL tables before manual edits.
