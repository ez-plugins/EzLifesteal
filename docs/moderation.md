---
title: Moderation Guide
nav_order: 8
description: "How to set up staff roles, manage hearts, investigate smurf reports, and test config changes safely"
---

# Moderation Guide
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

This guide covers how to configure staff roles, perform day-to-day heart management, investigate smurf alerts, and test rule changes safely before they affect your live server.

---

## Step 1: Configure Admin Detection

`admin.yml` controls who counts as an "admin" in EzLifesteal. Detected admins are excluded from heart gain and loss so they can moderate gameplay without drifting into the leaderboard.

```yaml
# admin.yml — recommended starting point
enabled: true
treat-ops-as-admin: true          # server operators are always admins
permission-node: lifesteal.admin  # anyone holding this perm is an admin
allowed-uuids: []                 # optional fallback for specific accounts
bypass-heart-loss: true
bypass-heart-gain: true
restrict-smurf-alerts-to-admins: false
```

> When `restrict-smurf-alerts-to-admins: false`, all players holding `lifesteal.alert` receive smurf notifications. Set it to `true` if you only want players who also qualify as full admins to see alerts.

Full field reference: [admin.yml reference]({{ site.baseurl }}/config/admin).

---

## Step 2: Assign Permission Groups

EzLifesteal ships with three role bundles. Assign them via your permissions plugin (LuckPerms, PermissionsEx, etc.):

| Bundle | Includes | Who gets it |
| --- | --- | --- |
| `lifesteal.player` | `command.base`, `top`, `transfer`, `withdraw` | All regular players |
| `lifesteal.mod` | `lifesteal.player` + `alert`, `manage.view`, `smurf.manage`, `scoreboard.*` | Trusted moderators |
| `lifesteal.admin` | `lifesteal.mod` + `manage.*`, `admin.banlist`, `reload`, `test` | Server administrators |

> Give moderators `lifesteal.alert` explicitly if you want them to receive live smurf notifications without granting the full `lifesteal.mod` bundle.

Full node list: [Permissions reference]({{ site.baseurl }}/permissions).

---

## Day-to-Day Tasks

### Checking a Player's Hearts

```text
/lifesteal hearts <player>
```

Works for offline players. Returns the stored heart total from the data layer, not the live in-game health bar.

### Adjusting Hearts

| Situation | Command |
| --- | --- |
| Override to an exact value | `/lifesteal set <player> <hearts>` |
| Add hearts (e.g. compensate a rollback) | `/lifesteal add <player> <hearts>` |
| Remove hearts (e.g. punish cheating) | `/lifesteal remove <player> <hearts>` |
| Restore to configured default | `/lifesteal reset <player>` |
| Revive a banned player | `/lifesteal revive <player>` |

All adjustments respect `min-hearts` and `max-hearts` from `lifesteal-core.yml`, so you cannot accidentally push a player outside the configured bounds.

### Giving Heart Vouchers Directly

```text
/lifesteal giveheart <player> <heartId|tier> [amount]
```

Delivers heart voucher items directly to the player's inventory. If the inventory is full, items drop at their feet. Use a `heartId` (e.g. `basic`, `gold`) to give a specific heart type, or a `tier` number to give by tier.

### Viewing the Leaderboard

```text
/lifesteal top [page]
```

Shows the hearts leaderboard sorted from highest to lowest. Page through results with the optional page number.

### Resetting All Players

```text
/lifesteal resetall
```

Resets every stored profile to the configured default hearts. Runs asynchronously so it will not freeze the server, but it is **irreversible** — back up `plugins/EzLifesteal/data/` before running this in production.

---

## Smurf Investigation Workflow

When a smurf alert fires, players holding `lifesteal.alert` receive a chat notification. To investigate:

**1. Open the review GUI:**

```text
/lifesteal smurf
```

Lists all recorded alerts with suspect name, victim name, kill count, and timestamp. Requires `lifesteal.smurf.manage`.

**2. Click an alert entry** to open the kill history detail view. Look for:

- **One-sided pattern** — all kills from A → B and none from B → A is a strong farming signal.
- **Rapid bursts** — kills within seconds of each other may indicate teleport or exploit abuse.
- **Spread over days** — kills distributed across many sessions are more likely legitimate rivalry than deliberate farming.

**3. Cross-reference current state:**

```text
/lifesteal hearts <suspect>
/lifesteal hearts <victim>
```

Check whether the victim has been bled down significantly and whether the suspect has an unusually high count.

**4. Take action:**

| Finding | Recommended action |
| --- | --- |
| Confirmed one-sided farming | `/lifesteal remove` hearts from suspect; `/lifesteal add` hearts to victim |
| Suspected alt or shared account | Restore victim's hearts; investigate IPs via server management tools |
| False positive (legitimate rivalry) | Raise `same-victim-threshold` in `smurf.yml` or add both UUIDs to `exempted-players` |
| Organised event causing alerts | Temporarily add participant UUIDs to `exempted-players` for the event duration |

---

## Hologram Leaderboard

Place a floating leaderboard at your current position:

```text
/lifesteal hologram place
```

Remove it:

```text
/lifesteal hologram remove
```

The hologram refreshes automatically on the interval configured by `hologram.update-interval-ticks` in `features.yml` and persists across server restarts. Run the place command while standing exactly where you want the **top line** to appear — the hologram grows downward.

---

## Testing Config Changes Safely

Before applying heart-rule changes on a live server, validate them on a staging server:

1. **Disable the admin bypass** — set `bypass-heart-loss: false` and `bypass-heart-gain: false` in `admin.yml`, or remove your account from admin detection. This ensures test flows are not silently skipped.

2. **Run simulated flows:**

   ```text
   /lifesteal test kill
   /lifesteal test death
   ```

3. **Verify the delta:**

   ```text
   /lifesteal hearts <yourname>
   ```

   Confirm hearts changed by the expected amount after each simulation.

4. **Re-enable the bypass** before pushing to production.

---

## Reloading Configuration

```text
/lifesteal reload
```

Refreshes all YAML files (`config.yml`, `admin.yml`, `smurf.yml`, `storage.yml`, `lifesteal-*.yml`, language files, and overlays) without restarting. Safe to run at any time, but avoid it during active PvP — overlay updates may flicker briefly for online players.

**When to restart instead:**

- Changing `type` in `storage.yml` (YAML ↔ MySQL) requires a full restart so the storage backend can reinitialise cleanly.
- Adding a new soft-dependency jar (PlaceholderAPI, Vault) to the server for the first time.

---

## Tips & Tricks

- **Asymmetric bypass:** `bypass-heart-loss` and `bypass-heart-gain` are independent. For senior admins who want to earn hearts normally but never lose them while moderating, set `bypass-heart-loss: true` and `bypass-heart-gain: false`.
- **Alert scope:** if your team is large and alert volume is high, set `restrict-smurf-alerts-to-admins: true` in `admin.yml` so only senior admins are notified. Moderators can still investigate on demand via `/lifesteal smurf`.
- **Pre-season backup:** before running `/lifesteal resetall`, copy `plugins/EzLifesteal/data/players/` to a safe location. YAML player files are plain text and trivial to restore if needed.
- **Shop post-purchase logging:** use the `commands` field in `shop.yml` to hook any purchase into a logging command or Discord bridge so you have a transaction trail.
- **Hologram height:** place the hologram with your crosshair at the centre of where you want the list to appear, not the top, since each new line adds below the anchor. A good default is standing at the spot and aiming straight ahead.
- **Admin test account:** if admins play legitimately during off-hours, keep a separate account UUID in `allowed-uuids` for staff duties and play on a personal account (not on the admin list) to remain in the leaderboard.
- **Zero-heart command hooks:** use `zero-heart-commands` in `lifesteal-core.yml` to automate punishment workflows — for example, broadcasting eliminations or logging them to an audit channel — instead of relying on manual admin intervention.
