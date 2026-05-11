package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezcountdown.api.event.CountdownEndEvent;
import com.skyblockexp.ezlifesteal.gui.BeaconInfoMenu;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.BeaconSpawnService;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for external events that affect plugin-spawned beacons:
 *
 * <ul>
 *   <li>{@link CountdownEndEvent} — marks the beacon AVAILABLE when the EzCountdown timer ends.</li>
 *   <li>{@link BlockBreakEvent} — prevents players from breaking an active spawned beacon.</li>
 *   <li>Explosion events — prevents explosions from destroying spawned beacons.</li>
 * </ul>
 */
public final class SpawnedBeaconListener implements Listener {

    private static final String EZLS_COUNTDOWN_PREFIX = "ezls-beacon-";

    private final BeaconSpawnService beaconSpawnService;
    private final PluginAccessor accessor;
    private final Logger logger;

    public SpawnedBeaconListener(
            BeaconSpawnService beaconSpawnService,
            PluginAccessor accessor,
            Logger logger
    ) {
        this.beaconSpawnService = beaconSpawnService;
        this.accessor = accessor;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // EzCountdown integration
    // -------------------------------------------------------------------------

    /**
     * Triggered when an EzCountdown timer finishes.
     * Identifies beacons waiting on this countdown and marks them AVAILABLE.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCountdownEnd(CountdownEndEvent event) {
        final String countdownName = event.getCountdown().getName();
        if (!countdownName.startsWith(EZLS_COUNTDOWN_PREFIX)) {
            return;
        }
        // Extract the short beacon id that was embedded in the countdown name
        // Format: "ezls-beacon-{shortId}" (shortId = first 8 chars of UUID)
        final String shortId = countdownName.substring(EZLS_COUNTDOWN_PREFIX.length());

        beaconSpawnService.getActiveBeacons().stream()
                .filter(beacon -> beacon.shortId().equals(shortId))
                .map(beacon -> beacon.getId())
                .forEach(beaconId -> beaconSpawnService.markAvailable(beaconId, accessor));
    }

    /**
     * Opens the beacon info GUI when a player right-clicks a plugin-spawned beacon block.
     *
     * <p>The event is cancelled so the vanilla beacon GUI (potion effects UI) is never shown.
     * This handler runs at LOW priority so higher-priority listeners (e.g. the revive handler)
     * can cancel the event first — when that happens, {@code ignoreCancelled = true} ensures
     * we skip opening the info menu and let the revive proceed.</p>
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBeaconRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEACON) {
            return;
        }
        if (beaconSpawnService.findByLocation(block.getLocation()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        final Player player = event.getPlayer();
        beaconSpawnService.findByLocation(block.getLocation())
                .ifPresent(beacon -> new BeaconInfoMenu(player, beacon).open());
    }

    // -------------------------------------------------------------------------
    // Block protection
    // -------------------------------------------------------------------------

    /**
     * Cancels block break attempts on active spawned beacons, notifying the player.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.BEACON) {
            return;
        }
        if (beaconSpawnService.findByLocation(block.getLocation()).isPresent()) {
            event.setCancelled(true);
            if (event.getPlayer() != null) {
                final Player player = event.getPlayer();
                final String msg = accessor.getMessageService().getMessage("beacon-spawn-protected");
                player.sendMessage(msg);
            }
        }
    }

    /**
     * Removes spawned beacons from an explosion's block list to protect them from destruction.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block ->
                block.getType() == Material.BEACON
                        && beaconSpawnService.findByLocation(block.getLocation()).isPresent()
        );
    }

    /**
     * Removes spawned beacons from an entity explosion's block list.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
                block.getType() == Material.BEACON
                        && beaconSpawnService.findByLocation(block.getLocation()).isPresent()
        );
    }
}
