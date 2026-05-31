
[![GitHub](https://img.shields.io/badge/GitHub-Repo-181717?logo=github)](https://github.com/ez-plugins/EzLifesteal) [![Discord](https://img.shields.io/badge/Discord-Join%20our%20server-7289DA?logo=discord)](https://discord.gg/yWP95XfmBS) [![License](https://img.shields.io/github/license/ez-plugins/EzLifesteal)](https://github.com/ez-plugins/EzLifesteal/blob/main/LICENSE)

A full-featured **lifesteal plugin for Minecraft** built for Paper servers. EzLifesteal gives server owners precise control over heart gain and loss, a complete heart item economy, seasonal resets, anti-abuse systems, and a revive beacon subsystem  -  all in one lightweight, highly configurable jar.

## Gameplay Features

- **Per-world heart rules**  -  set default, minimum, and maximum hearts; configure kill rewards and death penalties; choose zero-heart ban or respawn behaviour; define per-world overrides in `lifesteal-worlds.yml`.
- **Mob heart drops**  -  let mobs drop hearts on death with per-entity permission nodes and world allow/deny lists.
- **Kill streak rewards**  -  trigger money payouts ([Vault](https://modrinth.com/plugin/vault)), console commands, item grants, and server broadcasts at configurable milestone streaks.
- **Heart item economy**  -  physical heart objects with custom materials, textures, and optional NBT. Players give, trade, auction, or consume them to gain hearts. Configured in `hearts.yml`.
- **Heart shop**  -  configurable shop GUI where players can spend in-game currency on hearts.
- **Combat logout protection**  -  tags PvP sessions for a configurable window and punishes quitters so duels stay fair.
- **Leaderboards & overlays**  -  action-bar / boss-bar overlays, a self-updating `/lifesteal hologram place` leaderboard, `/lifesteal top` rankings, and live [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) data for scoreboards. Run `/lifesteal hologram cleanup [radius]` to remove ghost stands left by a crash.
- **Smurf detection**  -  monitors farm loops and rotating kill rings; alerts staff holding `lifesteal.alert`; stores full history for review.
- **Admin controls**  -  define staff by ops, permission node, UUID, or name; optionally bypass heart gain or loss for admins; smurf alerts can be kept admin-only.
- **Seasonal resets**  -  install **[EzSeasons](https://modrinth.com/plugin/ezseasons)** for automated season scheduling, countdown broadcasts, and automatic heart resets. No extra bridge jars required.
- **8 built-in translations**  -  English, German, Spanish, French, Dutch, Portuguese, Russian, and Chinese. All messages editable in `languages/<locale>.yml`.

## Revive Beacon

Banned players can be restored by a teammate who right-clicks a beacon block with a configurable voucher item.

- Whitelist existing beacons with `/lifesteal beacon add`, or let the plugin place and manage beacons automatically.
- **Beacon spawn subsystem**  -  places BEACON blocks at random or specified coordinates on a schedule or on demand.
- Full lifecycle: placement → optional [WorldGuard](https://modrinth.com/plugin/worldguard) protection region → optional [EzCountdown](https://modrinth.com/plugin/ezcountdown) warm-up timer → available → auto-expiry.
- When a beacon becomes claimable, the plugin fires a server-wide broadcast, title overlay, particles, and fireworks  -  fully configurable under `revive-beacon.spawn.availability-event`.
-- Beacon commands: `/beacon spawn [world x y z]`, `/beacon despawn <id|all>`, `/beacon spawns`.

#### Screenshots

![Beacon countdown](https://i.ibb.co/60VcQ6Z1/image.png) 

![Beacon warming-up](https://i.ibb.co/wZshsy8b/image.png)
![Beacon player selection](https://i.ibb.co/KRpjZw0/image.png)

## Heart Objects

Physical heart items that players can hold, trade, and consume to gain hearts.

- Define any number of heart tiers in `hearts.yml` with custom materials, player head textures, and optional NBT data.
- Use `/lifesteal giveheart <player> <id|tier> [amount]` to hand them out. Items drop at the player's feet if inventory is full.
- Use cases: loot drops on kill, auction-house trading, rare collectible hearts, heart-based economy.

## Optional Integrations

![EzLifesteal leaderboard and scoreboard](https://i.ibb.co/Y4NzV3Vd/ez-lifesteal-top-scoreboard.png)

EzLifesteal runs standalone. All integrations are soft dependencies, the plugin starts normally if any of them are absent.

| Integration | Purpose |
|---|---|
| [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) | Player and leaderboard placeholders for scoreboards, tab lists, and other plugins |
| [Vault](https://modrinth.com/plugin/vault) | Economy payouts for kill streak rewards |
| [EzSeasons](https://modrinth.com/plugin/ezseasons) | Automated season scheduling and heart resets |
| [WorldGuard](https://modrinth.com/plugin/worldguard) | Protection region around each plugin-spawned beacon |
| [EzCountdown](https://modrinth.com/plugin/ezcountdown) | Visible countdown timer during beacon warm-up (falls back to built-in Bukkit task) |
| TeamsAPI | Skip heart transfers between teammates (`team-kill-bypass-with-teams-api` in `lifesteal-core.yml`) |

## Configuration Presets

### Casual SMP
Raise the heart floor and disable zero-heart bans so everyone keeps playing.

```yaml
# lifesteal-core.yml
default-hearts: 12.0
min-hearts: 6.0
max-hearts: 30.0
hearts-per-kill: 1.0
hearts-lost-on-death: 1.0
ban-when-zero-hearts: false
```

### Hardcore Grind
Every fight counts  -  lose two hearts on death, gain half a heart on kills, with tougher rules in the Nether.

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

## PlaceholderAPI Placeholders

Install [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) and the expansion registers automatically.

- `%ezlifesteal_hearts%` / `%ezlifesteal_current_hearts%`  -  stored heart count for a player.
- `%ezlifesteal_default_hearts%`, `%ezlifesteal_min_hearts%`, `%ezlifesteal_max_hearts%`
- `%ezlifesteal_top_<n>_hearts%`, `%ezlifesteal_top_<n>_current_hearts%`, `%ezlifesteal_top_<n>_name%`, `%ezlifesteal_top_<n>_uuid%`

## Commands & Permissions

| Scope | Examples |
|---|---|
| Player | `/lifesteal`, `/lifesteal top`, `/lifesteal transfer`, `/lifesteal shop` |
| Staff | `/lifesteal give`, `/lifesteal revive`, `/lifesteal reload`, `/lifesteal test` |
| Beacon | `/beacon add|remove|list|clear|spawn|despawn|spawns` (alias for `/lifesteal beacon`) |

Permission bundles: `lifesteal.player` · `lifesteal.mod` · `lifesteal.admin`

See the full [Commands reference](https://ez-plugins.github.io/EzLifesteal/commands) and [Permissions list](https://ez-plugins.github.io/EzLifesteal/permissions).

## Storage & Reliability

- **YAML** (default)  -  one file per player at `data/players/<uuid>.yml`; auto-migrates legacy `players.yml` on first load.
- **MySQL**  -  table created on startup using `ON DUPLICATE KEY UPDATE` for safe concurrent saves.
- All I/O runs off the main thread. Use `/lifesteal reload` to switch backends without restarting.

## Quick Start

1. Place the jar in your server's `plugins/` folder and start the server.
2. Pick `YAML` or `MYSQL` storage in `storage.yml`, set your language in `config.yml`, and tune heart rules in `lifesteal-core.yml`.
3. Configure world overrides in `lifesteal-worlds.yml` and overlays in `features.yml`. Install [EzSeasons](https://modrinth.com/plugin/ezseasons) if you want season resets.
4. Review `admin.yml` to define staff and bypass rules.
5. Use `/lifesteal reload` after config changes. Run `/lifesteal test <kill|death>` to validate flows before going live.

---

Need help or want to request a feature? Join [our Discord](https://discord.gg/yWP95XfmBS). Source: [GitHub repository](https://github.com/ez-plugins/EzLifesteal)

[![Try the other plugins in the EzPlugins series](https://i.ibb.co/PzfjNjh0/ezplugins-try-other-plugins.png)](https://modrinth.com/collection/Q98Ov6dA)

