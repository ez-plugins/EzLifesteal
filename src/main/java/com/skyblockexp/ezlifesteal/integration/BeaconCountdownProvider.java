package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import java.util.Optional;

/**
 * Abstraction over EzCountdown for beacon countdown management.
 *
 * <p>When EzCountdown is absent, a no-op implementation is used and the
 * {@link com.skyblockexp.ezlifesteal.service.BeaconSpawnService} falls back
 * to an internal {@link com.skyblockexp.ezlifesteal.util.SchedulerAdapter} timer.</p>
 */
public interface BeaconCountdownProvider {

    /**
     * Creates and starts a countdown display.
     *
     * @param beaconShortId 8-character hex prefix of the beacon UUID, used to name the countdown
     * @param settings      the full countdown config (duration, display types, format message, bar style)
     * @return the countdown identifier if creation succeeded, or empty when EzCountdown is unavailable
     */
    Optional<String> startCountdown(String beaconShortId, BeaconSpawnSettings.CountdownSettings settings);

    /**
     * Cancels and deletes a countdown.
     *
     * @param countdownId the id returned by {@link #startCountdown}, may be {@code null}
     */
    void cancelCountdown(String countdownId);
}
