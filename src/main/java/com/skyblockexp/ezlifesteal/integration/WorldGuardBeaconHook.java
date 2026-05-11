package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * WorldGuard-backed implementation of {@link BeaconAreaProtection}.
 */
public final class WorldGuardBeaconHook implements BeaconAreaProtection {

    private static final String REGION_PREFIX = "ezls-beacon-";

    private final Logger logger;

    public WorldGuardBeaconHook(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Optional<String> protect(Location center, BeaconSpawnSettings.WorldGuardSettings settings) {
        if (!settings.enabled()) {
            return Optional.empty();
        }
        try {
            final RegionManager regions = getRegionManager(center.getWorld());
            if (regions == null) {
                return Optional.empty();
            }
            final String regionId = REGION_PREFIX + Long.toHexString(System.nanoTime() & 0xFFFFFFFFL);
            final int radius = settings.radius();
            final int bx = center.getBlockX();
            final int by = center.getBlockY();
            final int bz = center.getBlockZ();
            final ProtectedCuboidRegion region = new ProtectedCuboidRegion(
                    regionId,
                    BlockVector3.at(bx - radius, Math.max(by - radius, -64), bz - radius),
                    BlockVector3.at(bx + radius, Math.min(by + radius, 320), bz + radius)
            );
            if (settings.denyBuild()) {
                region.setFlag(Flags.BUILD, StateFlag.State.DENY);
            }
            if (settings.denyPvp()) {
                region.setFlag(Flags.PVP, StateFlag.State.DENY);
            }
            if (settings.denyMobDamage()) {
                region.setFlag(Flags.MOB_DAMAGE, StateFlag.State.DENY);
            }
            if (settings.denyExplosions()) {
                region.setFlag(Flags.OTHER_EXPLOSION, StateFlag.State.DENY);
                region.setFlag(Flags.TNT, StateFlag.State.DENY);
            }
            regions.addRegion(region);
            return Optional.of(regionId);
        } catch (Exception exception) {
            logger.warning("Failed to create WorldGuard region for spawned beacon: " + exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void unprotect(String regionId, World world) {
        if (regionId == null) {
            return;
        }
        try {
            final RegionManager regions = getRegionManager(world);
            if (regions != null) {
                regions.removeRegion(regionId, RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
            }
        } catch (Exception exception) {
            logger.warning("Failed to remove WorldGuard region " + regionId + ": " + exception.getMessage());
        }
    }

    private RegionManager getRegionManager(World world) {
        if (world == null) {
            return null;
        }
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }
}
