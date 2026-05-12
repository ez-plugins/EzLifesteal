---
title: Storage
nav_order: 10
parent: Features
description: "YAML and MySQL persistence backends for player profiles and ban records"
---

# Storage
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

EzLifesteal stores player heart profiles and ban records in either flat YAML files or a MySQL database. The backend is selected in `storage.yml` and all database I/O runs asynchronously to avoid blocking the main thread.

---

## Choosing a backend

```yaml
type: YAML   # or MYSQL
```

| Backend | Best for |
| --- | --- |
| `YAML` | Small servers, simple setup, no database infrastructure required |
| `MYSQL` | Large servers, shared data across a BungeeCord/Velocity network, or when you want SQL-level query access |

---

## YAML backend

Profiles and bans are stored in the plugin data folder:

- `data/players/<uuid>.yml` — heart count per player.
- `data/bans/<uuid>.yml` — ban record (reason, source, timestamps, active flag).

No further configuration is needed beyond `type: YAML`.

---

## MySQL backend

```yaml
type: MYSQL

mysql:
  host: localhost
  port: 3306
  database: lifesteal
  username: root
  password: changeme
  use-ssl: false
  table: lifesteal_players
```

The plugin creates two tables automatically:

| Table | Content |
| --- | --- |
| `lifesteal_players` | Heart profiles (`uuid`, `hearts`) |
| `lifesteal_players_bans` | Ban records with full audit fields |

Grant the database user `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `CREATE TABLE` on the target database at minimum. Avoid `GRANT ALL` in production.

---

## Ban record reconciliation

On startup the plugin reconciles stored ban records with Bukkit's runtime ban list:

- Active bans in storage that are missing from Bukkit are reapplied.
- Bukkit bans that are missing from storage are imported once (only when no existing record exists).

This ensures bans survive restarts and backend switches cleanly.

`/lifesteal revive <player>` clears both the storage record and Bukkit's runtime ban state simultaneously.

---

## Switching backends

1. Stop the server.
2. Change `type` in `storage.yml`.
3. Manually migrate data if needed (no automatic migration tool is included).
4. Start the server. The new backend initialises on startup.

Avoid switching backends repeatedly during an active ban window — reconciliation runs once at startup and may import stale state if records were modified externally while swapping.

---

## Config reference

- [Storage (`storage.yml`)](../config/storage) — exhaustive key reference with MySQL schema details
