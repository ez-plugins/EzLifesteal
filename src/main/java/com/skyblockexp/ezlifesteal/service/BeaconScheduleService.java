package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter.TaskHandle;
import java.util.logging.Logger;

/**
 * Manages the optional recurring schedule that auto-spawns beacons at a configured interval.
 *
 * <p>If the schedule feature is disabled in settings the service stays idle. The task spawns a
 * random beacon only when the number of active beacons is below the configured
 * {@link BeaconSpawnSettings#maxConcurrent()} limit.</p>
 */
public final class BeaconScheduleService {

    private final BeaconSpawnService beaconSpawnService;
    private final Logger logger;
    private TaskHandle scheduleTask;

    public BeaconScheduleService(BeaconSpawnService beaconSpawnService, Logger logger) {
        this.beaconSpawnService = beaconSpawnService;
        this.logger = logger;
    }

    /**
     * Starts the recurring schedule if {@link BeaconSpawnSettings.ScheduleSettings#enabled()} is true.
     *
     * @param accessor plugin accessor used for scheduling and reading settings
     */
    public void start(PluginAccessor accessor) {
        stop(); // cancel any existing task before (re)start

        final BeaconSpawnSettings settings = accessor.getBeaconSpawnSettings();
        if (!settings.enabled() || !settings.schedule().enabled()) {
            return;
        }
        final int intervalMinutes = Math.max(1, settings.schedule().intervalMinutes());
        final long periodTicks = (long) intervalMinutes * 60L * 20L;

        scheduleTask = SchedulerAdapter.runTimer(
                accessor.getPlugin(),
                () -> tick(accessor, settings),
                periodTicks,
                periodTicks
        );
        logger.info("Beacon spawn schedule started — interval: " + intervalMinutes + " minute(s).");
    }

    /**
     * Cancels the running schedule task, if any.
     */
    public void stop() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
            scheduleTask = null;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void tick(PluginAccessor accessor, BeaconSpawnSettings settings) {
        final int activeCount = beaconSpawnService.getActiveBeacons().size();
        if (activeCount >= settings.maxConcurrent()) {
            return;
        }
        beaconSpawnService.findRandomSpawnLocation(settings).ifPresentOrElse(
                location -> beaconSpawnService.spawnBeacon(location, accessor).ifPresentOrElse(
                        beacon -> {},
                        () -> logger.warning("Scheduled spawn rejected despite location being valid.")
                ),
                () -> logger.warning("Schedule tick: could not find a random spawn location.")
        );
    }
}
