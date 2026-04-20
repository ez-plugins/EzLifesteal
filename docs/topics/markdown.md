# EzLifesteal

EzLifesteal is a configurable Lifesteal plugin for Paper, Spigot, Folia, and Bukkit servers. It ships with flexible heart rules, fast storage, hologram/scoreboard overlays, PlaceholderAPI hooks, and built-in smurf protection.

## Highlights

- Works with Paper 26.1 (Java 25) and stays friendly with Paper, Spigot, Folia, and Bukkit. The jar includes the `folia-supported` marker.
- Saves player data asynchronously to YAML files or MySQL so leaderboards, profiles, and shutdown saves stay smooth.
- Control hearts per world: set default, minimum, and maximum hearts, adjust kill rewards and death penalties, pick zero-heart behaviour, allow or block worlds, make per-world overrides, and let mobs drop hearts with per-entity permissions.
- Optional kill streak rewards can hand out money, run console commands, give items, and broadcast wins when players hit streak milestones, with Vault support for economy payouts.
- Combat logout protection tags PvP combat for a configurable window and punishes quitters so duels stay fair.
- Built-in overlays show action-bar or boss-bar updates. `/lifesteal top` leaderboards, the hologram, and PlaceholderAPI placeholders share live standings anywhere.
- `/lifesteal hologram place` drops a self-updating leaderboard that survives restarts and can be cleared with one command. The scoreboard wildcard (`lifesteal.scoreboard.*`) makes it easy to hand out place/remove access.
- Need competitive seasons? Install the companion **EzSeasons** plugin for automatic heart resets, start/end scheduling, countdowns, and reminder broadcasts. EzLifesteal now hooks into its public API automatically, so no extra bridge jars are required.
- Smurf detection watches for players farming the same target or rotating killers, keeps history GUIs ready, and only alerts trusted staff.
- `admin.yml` lists staff by ops, permission nodes, UUIDs, or names, chooses if admins bypass heart gain or loss, and decides whether smurf alerts stay admin-only.
- Translate player messages quickly with built-in English and Dutch files in `languages/*.yml`.
- `/lifesteal` staff commands make it easy to add, remove, reset, revive, or reset all hearts without touching storage files.

## Storage & reliability

- Storage tasks run on their own thread so slow disks or databases never freeze the main server.
- YAML mode saves each player to `plugins/EzLifesteal/data/players/<uuid>.yml` and upgrades old `players.yml` data the first time it loads.
- MySQL mode creates its table on startup (`uuid`/`hearts`) and uses `ON DUPLICATE KEY UPDATE` so repeat saves stay safe.
- Use `/lifesteal reload` to refresh configuration, messages, admin rules, storage backends, overlays, and PlaceholderAPI hooks without restarting.

## EzSeasons & smurf defence

![ez-lifesteal-top-scoreboard](https://i.ibb.co/Y4NzV3Vd/ez-lifesteal-top-scoreboard.png)

- **EzSeasons** lets you set a season length with `length-days` or pick exact `start`/`end` timestamps, including optional recurring windows.
- Broadcast reminders and show the countdown with `/season` from the EzSeasons plugin while EzLifesteal handles smurf protection and automatically resets hearts when the season rolls over.
- Smurf detection logs kills per killer and victim, watches for rotating killer rings, saves alert and kill history, and lets you exempt trusted players.
- Give moderators `lifesteal.alert` and edit `smurf.yml` to tweak thresholds or exempt trusted UUIDs.

## Requirements

- Java 25 runtime (matches Paper 26.1 requirements).
- Paper, Spigot, Folia, or Bukkit 26.1 or newer.
- Optional: PlaceholderAPI for placeholders, MySQL 8+ for database storage.
- Optional: Vault plus an economy provider for kill streak currency rewards.

## Quick start

1. Place the jar in your server's `plugins` folder and restart the server.
2. Choose `YAML` (default) or `MYSQL` storage in `storage.yml`. Set the default language in `config.yml`. Adjust heart rules in `lifesteal-core.yml`, world scoping in `lifesteal-worlds.yml`, and overlays in `features.yml`. Install EzSeasons if you want scheduled resets.
3. Review `admin.yml` to decide who counts as staff, whether they bypass heart gain or loss, and who receives smurf alerts.
4. Use `/lifesteal reload` after configuration changes and give your staff the permissions listed below.
5. Run `/lifesteal test <kill|death>` on a staging server to check kill and death flows before you go live.

## Commands & permissions

Player commands include `/lifesteal`, `/lifesteal top`, `/lifesteal transfer`, and `/lifesteal shop`. Staff commands cover heart management, reviving players, reloading config, and moderation. Shortcut bundles — `lifesteal.player`, `lifesteal.mod`, and `lifesteal.admin` — cover the most common permission setups.

See the full [Commands reference](https://ez-plugins.github.io/EzLifesteal/commands) and [Permissions list](https://ez-plugins.github.io/EzLifesteal/permissions).

## Configuration

Configuration is split across focused YAML files under `plugins/EzLifesteal/`. Heart rules live in `lifesteal-core.yml`, world scoping in `lifesteal-worlds.yml`, mob rewards in `lifesteal-mobs.yml`, and overlays in `features.yml`. Use `/lifesteal reload` to pick up changes without restarting.

See the [Configuration overview](https://ez-plugins.github.io/EzLifesteal/configuration) for the full file list and per-file references.

## Admin & smurf detection

Staff are defined in `admin.yml` by ops, permission node, or explicit UUID/name lists. Enable bypass flags to let them watch gameplay without losing hearts. Smurf detection watches kill patterns within a configurable time window and alerts staff with the `lifesteal.alert` permission.

Full details: [admin.yml reference](https://ez-plugins.github.io/EzLifesteal/config/admin) · [smurf.yml reference](https://ez-plugins.github.io/EzLifesteal/config/smurf)

## Configuration presets

### Casual SMP
Raises the minimum hearts and disables zero-heart bans so everyone can keep playing.

```yaml
# lifesteal-core.yml
default-hearts: 12.0
min-hearts: 6.0
max-hearts: 30.0
hearts-per-kill: 1.0
hearts-lost-on-death: 1.0
ban-when-zero-hearts: false
```

### Hardcore grind
Every fight is dangerous — players lose two hearts on death, gain half a heart on kills, and get banned at zero hearts, with tougher rules in the Nether.

```yaml
# lifesteal-core.yml
default-hearts: 8.0
min-hearts: 1.0
max-hearts: 16.0
hearts-per-kill: 0.5
hearts-lost-on-death: 2.0
ban-when-zero-hearts: true

# lifesteal-worlds.yml
world-overrides:
  world_nether:
    hearts-per-kill: 0.25
    hearts-lost-on-death: 3.0
```

## PlaceholderAPI

- Install [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) alongside EzLifesteal. The expansion registers automatically and caches lookups briefly to stay responsive.
- Player-centric placeholders: `%ezlifesteal_hearts%` / `%ezlifesteal_current_hearts%` (stored hearts), `%ezlifesteal_default_hearts%`, `%ezlifesteal_min_hearts%`, and `%ezlifesteal_max_hearts%`.
- Leaderboard placeholders: `%ezlifesteal_top_<position>_hearts%`, `%ezlifesteal_top_<position>_current_hearts%`, `%ezlifesteal_top_<position>_name%`, and `%ezlifesteal_top_<position>_uuid%`.

## Under the hood

- Hearts stay saved across restarts, reloads, and world switches, and reapply instantly on join, quit, PvP, or admin tweaks.
- Health scaling can be toggled separately from heart totals so you can keep the vanilla HUD or use scaled hearts.
- Background storage work and scheduled overlay updates keep the main thread responsive even during big migrations or resets.
- Mob rewards support fractional payouts, per-entity permissions, and world allow/deny lists so you can shape lifesteal progress everywhere.

Need support or want a new feature? Join [our discord server](https://discord.gg/yWP95XfmBS) and tell us what to add next!

[![Try the other Minecraft plugins in the EzPlugins series](https://i.ibb.co/PzfjNjh0/ezplugins-try-other-plugins.png)](https://modrinth.com/collection/Q98Ov6dA)
