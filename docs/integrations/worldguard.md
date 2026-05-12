---
title: WorldGuard
nav_order: 4
parent: Integrations
description: "WorldGuard integration — automatically protects spawned beacon regions"
---

# WorldGuard
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

[WorldGuard](https://dev.bukkit.org/projects/worldguard) is a world management and region protection plugin. When installed alongside EzLifesteal, each plugin-managed beacon spawn can automatically create and manage a protected cuboid region around the beacon block.

---

## What it enables

- **Automatic region creation on spawn** — when the beacon spawn system places a beacon and `spawn.worldguard.enabled: true`, a `ProtectedCuboidRegion` is registered in WorldGuard around the beacon block.
- **Automatic region deletion on despawn** — when the beacon is removed (expired, manually despawned, or consumed), the associated WorldGuard region is deleted.

---

## How it works

When a beacon transitions to the `SPAWNED` state:

1. EzLifesteal reads the region size from `spawn.worldguard.radius` in `revive-beacon.yml`.
2. A `ProtectedCuboidRegion` is created centred on the beacon block, extending `radius` blocks in every horizontal direction and a fixed height upwards.
3. The region ID is derived from the beacon's internal ID so it can be looked up for later deletion.

When the beacon is removed for any reason, EzLifesteal deletes the region from WorldGuard's region manager.

---

## When absent

The entire `worldguard` subsection in `revive-beacon.yml` is silently ignored. No region is created or deleted, and no errors are logged. All other beacon spawn behaviour continues normally.

---

## Setup

1. Install **WorldGuard** (and its dependency **WorldEdit**) on the same server.
2. Enable region protection in `revive-beacon.yml`:

   ```yaml
   spawn:
     worldguard:
       enabled: true
       radius: 5          # blocks from beacon centre in each horizontal direction
       region-flags: {}   # optional: WorldGuard flag overrides (e.g. pvp: allow)
   ```

3. No additional WorldGuard configuration is required. EzLifesteal manages region creation and deletion automatically.

---

## Related pages

- [Beacon Spawn](../features/beacon-spawn) — full beacon lifecycle guide
- [Beacon Spawn config (`revive-beacon.yml`)](../config/revive-beacon#beacon-spawn) — `spawn.worldguard` key reference
