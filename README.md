# EzLifesteal

[![CI](https://github.com/ez-plugins/EzLifesteal/actions/workflows/ci.yml/badge.svg)](https://github.com/ez-plugins/EzLifesteal/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/ez-plugins/EzLifesteal/graph/badge.svg)](https://codecov.io/gh/ez-plugins/EzLifesteal)
[![Modrinth](https://img.shields.io/modrinth/v/ezlifesteal?logo=modrinth&label=Modrinth)](https://modrinth.com/plugin/ezlifesteal)
[![Downloads](https://img.shields.io/modrinth/dt/ezlifesteal?logo=modrinth&label=Downloads)](https://modrinth.com/plugin/ezlifesteal)
[![Java](https://img.shields.io/badge/Java-21%20%7C%2025-orange?logo=openjdk)](https://adoptium.net/)
[![License](https://img.shields.io/github/license/ez-plugins/EzLifesteal)](LICENSE)

EzLifesteal is a production-ready Paper plugin for running Lifesteal game modes on Minecraft servers. It gives server owners full control over heart gain/loss, progression, anti-abuse systems, seasonal resets, and monetization-friendly options while staying lightweight and configurable.

## Quick links

- **Download on Modrinth:** https://modrinth.com/plugin/ezlifesteal
- **Commands reference:** [docs/commands.md](docs/commands.md)
- **Permissions reference:** [docs/permissions.md](docs/permissions.md)
- **Full configuration guide:** [docs/configuration.md](docs/configuration.md)
- **Developer guide:** [docs/developer-guide.md](docs/developer-guide.md)
- **Configuration by file:**
  - [config/lifesteal.md](docs/config/lifesteal.md)
  - [config/hearts.md](docs/config/hearts.md)
  - [config/shop.md](docs/config/shop.md)
  - [config/features.md](docs/config/features.md)
  - [config/storage.md](docs/config/storage.md)
  - [config/killstreaks.md](docs/config/killstreaks.md)
  - [config/admin.md](docs/config/admin.md)
  - [config/smurf.md](docs/config/smurf.md)

## Why server owners choose EzLifesteal

- **Flexible gameplay rules** for heart transfer, death penalties, and progression pacing.
- **Built-in heart ecosystem** with heart items, recipes, and a shop flow.
- **Competitive features** including killstreak rewards and leaderboard/hologram support.
- **Operational controls** with admin detection and smurf detection tooling.
- **Deployment flexibility** with YAML or MySQL storage backends.
- **Safe optional integrations**: missing dependencies degrade gracefully without preventing server startup.

## Core features

- Configurable lifesteal heart gain/loss logic.
- Heart item registry and recipe support.
- Heart shop and related GUI/listener systems.
- Killstreak reward pipeline.
- Player/mob listener hooks for custom reward flows.
- Top hologram and overlay support.
- Revive beacon system for banned player revives.
- Beacon spawn subsystem: auto-places BEACON blocks in the world on a schedule or on demand.

## Optional integrations

EzLifesteal runs standalone, but can integrate with:

- **[Vault](https://modrinth.com/plugin/vault)** for economy rewards and money-based incentives.
- **[PlaceholderAPI](https://modrinth.com/plugin/placeholderapi)** for placeholders in scoreboards, tab lists, and other plugins.
- **[EzSeasons](https://modrinth.com/plugin/ezseasons)** for season reset workflows and season-aware progression.
- **[WorldGuard](https://modrinth.com/plugin/worldguard)** for automatic region protection around plugin-spawned beacons.
- **[EzCountdown](https://modrinth.com/plugin/ezcountdown)** for a visible countdown timer during the beacon warm-up phase, with configurable start/end broadcasts, end commands, visibility scoping, and ephemeral mode.

If these plugins are not installed, EzLifesteal keeps running and only disables the corresponding integration behavior.

## Requirements

- **Java:** 21 or 25
- **Server software:** Paper 1.21+

## Java compatibility structure

- **Shared baseline code:** `src/main/java/com/skyblockexp/ezlifesteal/**`
- **Version-sensitive adapters:** `src/main/java/com/skyblockexp/ezlifesteal/compat/**`
- **Shared tests:** `src/test/java/**`

Place compatibility logic under `compat/` and keep feature/business behavior in `service/`, `gui/`, `listener/`, and
other domain packages. This keeps Java-version concerns isolated and easier to review.

## Installation

1. Download the latest EzLifesteal release from Modrinth.
2. Place the plugin JAR in your server's `plugins/` directory.
3. Start (or restart) the server to generate default config files.
4. Review and adjust settings using the docs linked above.
5. Reload/restart and validate your commands/permissions setup.

## Build from source

```bash
mvn clean package
```

Compiled artifacts are generated in `target/`.


## Coverage

Run coverage from a clean build so JaCoCo only inspects freshly compiled classes:

```bash
rm -rf target
mvn clean verify -Djacoco.haltOnFailure=false
```

- One-time local cleanup tip: if you have old compiled artifacts, delete `target/` before measuring coverage.
- CI uses the same `clean verify` coverage command.
- Reports are written to `target/site/jacoco/`.

## Default configuration files

On first startup, EzLifesteal creates and manages these runtime-backed configuration files:

- `config.yml`
- `admin.yml`
- `smurf.yml`
- `storage.yml`
- `lifesteal-core.yml`
- `lifesteal-drops.yml`
- `lifesteal-worlds.yml`
- `lifesteal-mobs.yml`
- `lifesteal-killstreaks.yml`
- `hearts.yml`
- `shop.yml`
- `features.yml`
- `revive-beacon.yml`
- `languages/*.yml`

Message and language keys are defined in `languages/<locale>.yml` files (for example, `languages/en.yml`).

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
