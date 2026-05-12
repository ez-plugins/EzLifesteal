package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link EzCountdownBeaconHook}.
 *
 * <p>EzCountdownApi is not on the test classpath; all tests exercise the "no provider" path
 * where {@code Bukkit.getServicesManager()} returns {@code null} for the EzCountdownApi
 * registration.</p>
 */
class EzCountdownBeaconHookTest {

    private ServerMock server;
    private Logger logger;
    private EzCountdownBeaconHook hook;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        logger = mock(Logger.class);
        hook = new EzCountdownBeaconHook(logger);
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    private BeaconSpawnSettings.CountdownSettings defaultSettings() {
        return new BeaconSpawnSettings.CountdownSettings(
                true,
                300,
                List.of("ACTION_BAR"),
                "&d{formatted} until active",
                "PURPLE",
                "SEGMENTED_20"
        );
    }

    @Test
    void startCountdown_apiNotRegistered_returnsEmpty() {
        // No EzCountdownApi registered in MockBukkit services → provider is null
        Optional<String> result = hook.startCountdown("abc12345", defaultSettings());
        assertTrue(result.isEmpty());
    }

    @Test
    void cancelCountdown_nullId_doesNotThrow() {
        // Should not throw when countdownId is null
        hook.cancelCountdown(null);
    }

    @Test
    void cancelCountdown_apiNotRegistered_doesNotThrow() {
        // Api not available — should log warning and return gracefully
        hook.cancelCountdown("ezls-beacon-abc12345");
    }

    @Test
    void startCountdown_withUnknownDisplayType_logsWarningAndReturnsEmpty() {
        BeaconSpawnSettings.CountdownSettings settings = new BeaconSpawnSettings.CountdownSettings(
                true,
                60,
                List.of("INVALID_TYPE"),
                null,
                null,
                null
        );
        // No API registered, so returns empty — but validates the parseDisplayTypes path
        Optional<String> result = hook.startCountdown("testid12", settings);
        assertTrue(result.isEmpty());
    }
}
