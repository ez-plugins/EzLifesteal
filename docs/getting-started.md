---
title: Getting Started
nav_order: 2
description: "Install EzLifesteal and set up your first lifesteal server"
---

# Getting Started
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Requirements

| Requirement | Minimum version |
|---|---|
| Server software | Paper 26.1 (or a compatible fork) |
| Java | 25 |

---

## Installation

1. Stop your server.
2. Download `EzLifesteal-1.1.0.jar` from [Modrinth](https://modrinth.com/plugin/ezlifesteal).
3. Copy the jar into your `plugins/` folder.
4. *(Optional)* Install [EzSeasons](https://modrinth.com/plugin/ezseasons) if you want
   hearts to reset automatically at the start of each season.
5. Start the server once. EzLifesteal generates all config files under
   `plugins/EzLifesteal/`.
6. Stop the server and edit the config files (see below).
7. Start the server.

---

## Basic configuration

Open `plugins/EzLifesteal/config.yml`. The minimum change is choosing a language.

```yaml
messages:
  language: "en"   # Supported: en, de, es, fr, nl, pt, ru, zh
  prefix: "&c[EzLifesteal]&r "
```

Open `plugins/EzLifesteal/lifesteal-core.yml` to enable lifesteal and set heart values.

```yaml
global-enabled: true
default-hearts: 10
max-hearts: 20
min-hearts: 0
hearts-per-kill: 1.0
hearts-lost-on-death: 1.0
ban-when-zero-hearts: true
```

---

## Storage backend

By default EzLifesteal uses YAML storage. To switch to MySQL, edit
`plugins/EzLifesteal/storage.yml`:

```yaml
storage:
  type: MYSQL
  mysql:
    host: "localhost"
    port: 3306
    database: "ezlifesteal"
    username: "root"
    password: "changeme"
```

See the [Storage reference](config/storage) for all options.

---

## Verify the setup

After starting the server with a valid config:

1. Join the server and run `/lifesteal` — you should see your current heart status.
2. Run `/lifesteal hearts <yourname>` to check your stored heart count.
3. Test a simulated kill with `/lifesteal test kill` (requires `lifesteal.test`).

---

## EzSeasons integration

If EzSeasons is installed, EzLifesteal automatically registers a season-reset
listener. When a season ends, all player heart profiles are reset to the default
value and a server-wide broadcast is sent.

No extra configuration is required. The integration is detected at startup.

---

## Next steps

- Read the full [Configuration reference](configuration) for every available option.
- See [Commands](commands) for all runtime commands.
- See [Permissions](permissions) to lock down admin commands on your server.
- If you are a developer, read the [Developer guide](developer-guide) to integrate
  with EzLifesteal or contribute to the project.
