# EzLifesteal

EzLifesteal is a configurable Lifesteal plugin for Paper, Spigot, Folia, and Bukkit servers. It ships with flexible heart rules, fast storage, hologram/scoreboard overlays, PlaceholderAPI hooks, and built-in smurf protection.

> **Alpha notice:** v0.3.0 is still being tuned, so expect frequent updates and the occasional breaking change while we finish the basics.

## Highlights

- Works with Paper 1.21.10 (Java 21) and stays friendly with Paper, Spigot, Folia, and Bukkit. The jar includes the `folia-supported` marker.
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

- Java 21 runtime (matches current Paper 1.21.10 requirements).
- Paper, Spigot, Folia, or Bukkit 1.21.10 or newer.
- Optional: PlaceholderAPI for placeholders, MySQL 8+ for database storage.
- Optional: Vault plus an economy provider for kill streak currency rewards.

## Quick start

1. Place the jar in your server's `plugins` folder and restart the server.
2. Choose `YAML` (default) or `MYSQL` storage in `storage.yml`. Set the default language in `config.yml`. Adjust heart and world rules in `lifesteal.yml` and tweak overlays in `features.yml`. Install EzSeasons if you want scheduled resets.
3. Review `admin.yml` to decide who counts as staff, whether they bypass heart gain or loss, and who receives smurf alerts.
4. Use `/lifesteal reload` after configuration changes and give your staff the permissions listed below.
5. Run `/lifesteal test <kill|death>` on a staging server to check kill and death flows before you go live.

## Commands & permissions

| Command | Description | Permission |
|---------|-------------|------------|
| /lifesteal | Show your stored heart total (players) or run sub-commands. | lifesteal.command.base |
| /lifesteal hearts <player> | View any player's saved hearts, even if they are offline. | lifesteal.manage.view |
| /lifesteal set <player> <hearts> | Force a player's heart total within the configured bounds. | lifesteal.manage.modify |
| /lifesteal add <player> <hearts> | Give a player extra hearts without exceeding the configured maximum. | lifesteal.manage.modify |
| /lifesteal remove <player> <hearts> | Remove hearts while respecting the configured minimum floor. | lifesteal.manage.modify |
| /lifesteal reset <player> | Restore a player's hearts to the configured default. | lifesteal.manage.modify |
| /lifesteal revive <player> | Restore default hearts and pardon any bans for the player. | lifesteal.manage.modify |
| /lifesteal giveheart <player> <heartId|tier> [amount] | Give a player heart items directly to their inventory. | lifesteal.manage.modify |
| /lifesteal resetall | Reset every stored profile to the default hearts asynchronously. | lifesteal.manage.resetall |
| /lifesteal transfer <player> <amount> | Send hearts to another player while respecting the minimum heart safety floor. | lifesteal.transfer |
| /lifesteal top [page] | Display the hearts leaderboard with pagination and PlaceholderAPI support. | lifesteal.top |
| /lifesteal hologram <place\|remove> | Place or remove the auto-updating hologram leaderboard at your location. | lifesteal.scoreboard.place / lifesteal.scoreboard.remove |
| /lifesteal smurf | Open the smurf-farming investigation GUI with recent alerts and kill history. | lifesteal.smurf.manage |
| /lifesteal reload | Reload configuration, messages, admin rules, storage backends, and overlays. | lifesteal.reload |
| /lifesteal test <kill\|death> | Simulate kills or deaths to validate lifesteal settings on a staging server. | lifesteal.test |

> Shortcut bundles: give `lifesteal.player` for normal play, add `lifesteal.mod` for moderation tools (alerts, GUI, scoreboard wildcard), and finish with `lifesteal.admin` for full control.

> Give staff the optional `lifesteal.alert` permission for live smurf alerts. Pair it with `restrict-smurf-alerts-to-admins: true` in `admin.yml` so only verified admins get pinged.

## Configuration snapshots

```yaml
# config.yml
# Supported languages: "en", "nl"
language: en

# storage.yml
type: YAML
mysql:
  host: localhost
  port: 3306
  database: lifesteal
  username: root
  password: password
  use-ssl: false
  table: lifesteal_players

# lifesteal.yml
global-enabled: true
default-hearts: 10.0
min-hearts: 1.0
max-hearts: 40.0
apply-health-scale: false
health-scale: 20.0
hearts-per-kill: 1.0
hearts-lost-on-death: 1.0
dont-remove-hearts-from-mobs: true
mob-remove-hearts-greater-than: -1
ban-when-zero-hearts: false
zero-heart-ban-message: "You have run out of hearts."
zero-heart-kick-message: "You have run out of hearts."
# Commands to run when a player reaches zero hearts and banning is disabled.
# Commands are executed as the server console and support the following placeholders:
#  - %player% / %victim% / %player_uuid% / %victim_uuid%
#  - %killer% / %killer_uuid%
#  - %hearts% / %remaining_hearts%
# Example:
# zero-heart-commands:
#   - "broadcast %player% has reached zero hearts!"
#   - "minecraft:execute as %player% run effect give @s minecraft:levitation 5 1"
# Use this to run custom punishments, announcements, or integration hooks when you prefer not to auto-ban.
combat-logout-protection:
  enabled: false
  tag-duration-seconds: 15.0
# Particle effects when consuming heart items
heart-consumption-effects:
  enabled: true
  # Enable stacking particle effects for a more intense visual experience
  stacking-particles: true
  # List of particle configurations for combined effects
  particles:
    - type: DUST
      count: 50
      speed: 0.1
      color: "#8B0000"  # Dark red for blood-like effect
    - type: HEART
      count: 20
      speed: 0.05
  # Duration in ticks for stacking effect (20 ticks = 1 second)
  stacking-duration-ticks: 40
enabled-worlds: []
disabled-worlds: []
world-overrides: {}
mob-rewards: {}
kill-streaks:
  enabled: false
  reset-on-death: true
  rewards: {}

# features.yml
action-bar:
  enabled: true
  mode: ACTION_BAR
  update-interval-ticks: 40
  message: "&c❤ %hearts% &7hearts"
  enabled-worlds: []
  disabled-worlds: []
  boss-bar:
    color: RED
    overlay: PROGRESS
hologram:
  update-interval-ticks: 600
  max-entries: 10
  location: {}
```

## Admin rules (`admin.yml`)

```yaml
enabled: true
treat-ops-as-admin: true
permission-node: lifesteal.admin
allowed-uuids: []
allowed-names: []
bypass-heart-loss: true
bypass-heart-gain: true
restrict-smurf-alerts-to-admins: false
```

## Admin bypass & testing

- Staff listed in `admin.yml` skip heart gains and losses when `bypass-heart-gain` and `bypass-heart-loss` stay enabled, so moderators can watch gameplay without losing progress.
- Switch those values to `false` (or remove yourself from the admin list) before testing lifesteal flows, or kill/death simulations, duels, and streak rewards will skip admins.
- Turn the bypass back on after testing so daily staff checks keep working without unwanted penalties.

## Smurf detection (`smurf.yml`)

```yaml
enabled: true
same-victim-threshold: 3
time-window-minutes: 15
notify-permission: lifesteal.alert
history-limit: 50
kill-history-limit: 120
exempted-players: []
```

## Configuration presets

### Casual SMP
Keeps fights exciting without scaring casual players. Raises the minimum hearts, gives one heart per kill, and disables zero-heart bans so everyone can keep playing.

```yaml
# lifesteal.yml
default-hearts: 12.0
min-hearts: 6.0
max-hearts: 30.0
hearts-per-kill: 1.0
hearts-lost-on-death: 1.0
ban-when-zero-hearts: false

# features.yml
action-bar:
  enabled: true
```

### Competitive season
Pair EzLifesteal with EzSeasons, lower the max heart cap, and speed up reminders as the reset gets closer.

```yaml
# lifesteal.yml
default-hearts: 10.0
min-hearts: 4.0
max-hearts: 24.0
hearts-per-kill: 1.0
hearts-lost-on-death: 1.0
ban-when-zero-hearts: true

# ezseasons config.yml
season:
  enabled: true
  length-days: 14
  reminder-minutes: [1440, 60, 15, 5, 1]
  broadcast-message: "&eSeason reset! Hearts are refreshed for everyone."
```

### Hardcore grind
Every fight is dangerous. Players lose two hearts on death, gain half a heart on kills, and get banned at zero hearts, so teams must play carefully.

```yaml
# lifesteal.yml
default-hearts: 8.0
min-hearts: 1.0
max-hearts: 16.0
hearts-per-kill: 0.5
hearts-lost-on-death: 2.0
ban-when-zero-hearts: true
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

Need support or want a new feature? Join [our discord server](https://discord.gg/yWP95XfmBS) tell us what to add next!

[![Try the other Minecraft plugins in the EzPlugins series](https://i.ibb.co/PzfjNjh0/ezplugins-try-other-plugins.png)](https://modrinth.com/collection/Q98Ov6dA)
