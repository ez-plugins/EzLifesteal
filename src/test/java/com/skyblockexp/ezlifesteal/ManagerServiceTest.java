package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.config.AdminConfigAdapter;
import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.config.SmurfConfigAdapter;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings;
import com.skyblockexp.ezlifesteal.overlay.HeartOverlayManager;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.runtime.state.GameplayState;
import com.skyblockexp.ezlifesteal.service.AdminSettingsFactory;
import com.skyblockexp.ezlifesteal.service.GameplayStateApplier;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.service.LifestealSettingsFactory;
import com.skyblockexp.ezlifesteal.service.ManagerService;
import com.skyblockexp.ezlifesteal.service.ManagerStateInitializer;
import com.skyblockexp.ezlifesteal.service.SmurfSettingsFactory;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerServiceTest {

    @ParameterizedTest
    @CsvSource({
        "'',true,1.0,true",
        "invalid-uuid,true,1.0,true",
        "invalid-uuid,false,2.5,false"
    })
    void setupManagersBuildsDeterministicGameplayStateForConfigCombinations(String adminUuid,
                                                                             boolean globalEnabled,
                                                                             double heartsPerKill,
                                                                             boolean banWhenZeroHearts) {
        Registry registry = new Registry();
        DefaultPluginRuntimeServices runtime = baseRuntime(registry);

        YamlConfiguration adminConfig = new YamlConfiguration();
        if (!adminUuid.isBlank()) {
            adminConfig.set("allowed-uuids", java.util.List.of(adminUuid));
        }
        adminConfig.set("bypass-heart-loss", false);
        adminConfig.set("bypass-heart-gain", true);

        YamlConfiguration lifestealConfig = new YamlConfiguration();
        lifestealConfig.set("global-enabled", globalEnabled);
        lifestealConfig.set("hearts-per-kill", heartsPerKill);
        lifestealConfig.set("hearts-lost-on-death", 2.0);
        lifestealConfig.set("ban-when-zero-hearts", banWhenZeroHearts);
        lifestealConfig.set("drop-heart-id", "basic");
        lifestealConfig.set("drop-heart-amount", 3);
        lifestealConfig.set("drop-heart-on-death", true);
        lifestealConfig.set("drop-heart-only-when-killed-by-player", false);
        lifestealConfig.set("revive-beacon.enabled", true);
        lifestealConfig.set("revive-beacon.voucher-heart-id", "basic");
        lifestealConfig.set("revive-beacon.require-sneak", true);
        lifestealConfig.set("revive-beacon.max-distance", 12.0D);
        lifestealConfig.set("revive-beacon.consume-on-fail", true);
        lifestealConfig.set("dont-remove-hearts-from-mobs", false);
        lifestealConfig.set("mob-remove-hearts-greater-than", 6.0);

        YamlConfiguration smurfConfig = new YamlConfiguration();

        when(runtime.getAdminConfigAdapter()).thenReturn(new AdminConfigAdapter(adminConfig, new YamlConfiguration()));
        when(runtime.getLifestealConfigAdapter())
                .thenReturn(new LifestealConfigAdapter(lifestealConfig, new YamlConfiguration()));
        when(runtime.getSmurfConfigAdapter()).thenReturn(new SmurfConfigAdapter(smurfConfig, new YamlConfiguration()));

        KillStreakSettings killStreakSettings = KillStreakSettings.disabled();
        when(runtime.parseKillStreakSettings()).thenReturn(killStreakSettings);

        LifestealManager lifestealManager = mock(LifestealManager.class);
        KillStreakManager killStreakManager = mock(KillStreakManager.class);
        HeartOverlayManager heartOverlayManager = mock(HeartOverlayManager.class);
        TopHologramManager topHologramManager = mock(TopHologramManager.class);

        ManagerStateInitializer initializer = mock(ManagerStateInitializer.class);
        doAnswer(invocation -> {
            Registry.ManagerState state = invocation.getArgument(0);
            state.setLifestealManager(lifestealManager);
            state.setKillStreakManager(killStreakManager);
            state.setHeartOverlayManager(heartOverlayManager);
            state.setTopHologramManager(topHologramManager);
            return null;
        }).when(initializer).initialize(any(), any(), any(), any(), any());

        ManagerService service = new ManagerService(
                runtime,
                new AdminSettingsFactory(),
                new LifestealSettingsFactory(),
                new SmurfSettingsFactory(),
                new GameplayStateApplier(),
                initializer
        );

        service.setupManagers();

        Registry.ManagerState managerState = registry.getManagerState();
        GameplayState gameplayState = registry.getGameplayState();

        assertSame(lifestealManager, managerState.getLifestealManager());
        assertSame(killStreakManager, managerState.getKillStreakManager());
        assertSame(heartOverlayManager, managerState.getHeartOverlayManager());
        assertSame(topHologramManager, managerState.getTopHologramManager());

        assertEquals(heartsPerKill, gameplayState.getHeartRulesState().getHeartsPerKill());
        assertEquals(2.0, gameplayState.getHeartRulesState().getHeartsLostOnDeath());
        assertEquals(globalEnabled, gameplayState.getHeartRulesState().isGlobalLifestealEnabled());
        assertEquals(banWhenZeroHearts, gameplayState.getHeartRulesState().isBanWhenZeroHearts());
        assertTrue(gameplayState.getDropRulesState().isDropHeartOnDeath());
        assertTrue(gameplayState.getReviveBeaconState().isReviveBeaconEnabled());
        assertEquals("basic", gameplayState.getReviveBeaconState().getReviveBeaconVoucherHeartId());
        assertTrue(gameplayState.getReviveBeaconState().isReviveBeaconRequireSneak());
        assertEquals(12.0D, gameplayState.getReviveBeaconState().getReviveBeaconMaxDistance());
        assertTrue(gameplayState.getReviveBeaconState().isReviveBeaconConsumeOnFail());
        assertTrue(gameplayState.getKillStreakSettings().configuredStreaks().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void setupManagersLogsInvalidUuidsAndContinues(boolean includeSmurfInvalidUuid) {
        Registry registry = new Registry();
        DefaultPluginRuntimeServices runtime = baseRuntime(registry);
        Logger logger = mock(Logger.class);
        when(runtime.getLogger()).thenReturn(logger);

        YamlConfiguration adminConfig = new YamlConfiguration();
        adminConfig.set("allowed-uuids", java.util.List.of("not-a-uuid", UUID.randomUUID().toString()));
        YamlConfiguration smurfConfig = new YamlConfiguration();
        if (includeSmurfInvalidUuid) {
            smurfConfig.set("exempted-players", java.util.List.of("oops-invalid"));
        }

        when(runtime.getAdminConfigAdapter()).thenReturn(new AdminConfigAdapter(adminConfig, new YamlConfiguration()));
        when(runtime.getLifestealConfigAdapter())
                .thenReturn(new LifestealConfigAdapter(new YamlConfiguration(), new YamlConfiguration()));
        when(runtime.getSmurfConfigAdapter()).thenReturn(new SmurfConfigAdapter(smurfConfig, new YamlConfiguration()));

        ManagerStateInitializer initializer = mock(ManagerStateInitializer.class);
        doAnswer(invocation -> {
            Registry.ManagerState state = invocation.getArgument(0);
            state.setLifestealManager(mock(LifestealManager.class));
            state.setKillStreakManager(mock(KillStreakManager.class));
            return null;
        }).when(initializer).initialize(any(), any(), any(), any(), any());

        ManagerService service = new ManagerService(
                runtime,
                new AdminSettingsFactory(),
                new LifestealSettingsFactory(),
                new SmurfSettingsFactory(),
                new GameplayStateApplier(),
                initializer
        );

        service.setupManagers();

        verify(logger).warning(contains("Invalid admin UUID"));
        if (includeSmurfInvalidUuid) {
            verify(logger).warning(contains("Invalid smurf exemption UUID"));
        }
        verify(runtime).setAdminDetector(any());
        verify(runtime).setSmurfDetector(any());
        assertNotNull(registry.getManagerState().getLifestealManager());
    }

    @ParameterizedTest
    @CsvSource({
        "-5.0,7.0,2.0,0.0,2.0,2.0",
        "1.0,10.0,4.0,1.0,4.0,4.0"
    })
    void setupManagersSanitizesHeartBoundsAndWritesBack(double configuredMin,
                                                        double configuredDefault,
                                                        double configuredMax,
                                                        double expectedMin,
                                                        double expectedDefault,
                                                        double expectedMax) {
        Registry registry = new Registry();
        DefaultPluginRuntimeServices runtime = baseRuntime(registry);

        YamlConfiguration lifestealConfig = new YamlConfiguration();
        lifestealConfig.set("min-hearts", configuredMin);
        lifestealConfig.set("default-hearts", configuredDefault);
        lifestealConfig.set("max-hearts", configuredMax);

        when(runtime.getLifestealConfigAdapter())
                .thenReturn(new LifestealConfigAdapter(lifestealConfig, new YamlConfiguration()));

        ManagerStateInitializer initializer = mock(ManagerStateInitializer.class);
        doAnswer(invocation -> {
            Registry.ManagerState state = invocation.getArgument(0);
            state.setLifestealManager(mock(LifestealManager.class));
            return null;
        }).when(initializer).initialize(any(), any(), any(), any(), any());

        ManagerService service = new ManagerService(
                runtime,
                new AdminSettingsFactory(),
                new LifestealSettingsFactory(),
                new SmurfSettingsFactory(),
                new GameplayStateApplier(),
                initializer
        );

        service.setupManagers();

        if (configuredMin < 0 || configuredDefault > configuredMax) {
            verify(runtime).setLifestealValue("min-hearts", expectedMin);
            verify(runtime).setLifestealValue("default-hearts", expectedDefault);
            verify(runtime).setLifestealValue("max-hearts", expectedMax);
            verify(runtime).saveLifestealSettings();
        }
        else {
            verify(runtime, never()).saveLifestealSettings();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void setupManagersPassesExistingVsNewKillStreakManagerBranch(boolean existingKillStreakManager) {
        Registry registry = new Registry();
        DefaultPluginRuntimeServices runtime = baseRuntime(registry);

        KillStreakManager providedManager = existingKillStreakManager ? mock(KillStreakManager.class) : null;
        when(runtime.getKillStreakManager()).thenReturn(providedManager);

        ManagerStateInitializer initializer = mock(ManagerStateInitializer.class);
        doAnswer(invocation -> {
            Registry.ManagerState state = invocation.getArgument(0);
            KillStreakManager passedManager = invocation.getArgument(4);
            state
                    .setKillStreakManager(passedManager == null
                            ? new KillStreakManager(mock(EzLifestealPlugin.class)) : passedManager);
            return null;
        }).when(initializer).initialize(any(), any(), any(), any(), any());

        ManagerService service = new ManagerService(
                runtime,
                new AdminSettingsFactory(),
                new LifestealSettingsFactory(),
                new SmurfSettingsFactory(),
                new GameplayStateApplier(),
                initializer
        );

        service.setupManagers();

        Registry.ManagerState managerState = registry.getManagerState();
        if (existingKillStreakManager) {
            assertSame(providedManager, managerState.getKillStreakManager());
        }
        else {
            assertNotNull(managerState.getKillStreakManager());
        }
        verify(initializer).initialize(eq(managerState), any(), any(), any(), eq(providedManager));
    }

    private DefaultPluginRuntimeServices baseRuntime(Registry registry) {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);

        when(runtime.getRegistry()).thenReturn(registry);
        when(runtime.getLogger()).thenReturn(mock(Logger.class));
        when(runtime.getProfileRepository()).thenReturn(mock(ProfileRepository.class));
        when(runtime.getMessageService()).thenReturn(mock(MessageService.class));

        when(runtime.getAdminConfigAdapter())
                .thenReturn(new AdminConfigAdapter(new YamlConfiguration(), new YamlConfiguration()));
        when(runtime.getLifestealConfigAdapter())
                .thenReturn(new LifestealConfigAdapter(new YamlConfiguration(), new YamlConfiguration()));
        when(runtime.getSmurfConfigAdapter())
                .thenReturn(new SmurfConfigAdapter(new YamlConfiguration(), new YamlConfiguration()));

        when(runtime.getEnabledWorlds()).thenReturn(Set.of("world"));
        when(runtime.getDisabledWorlds()).thenReturn(Set.of("world_nether"));
        when(runtime.parseKillStreakSettings()).thenReturn(KillStreakSettings.disabled());

        return runtime;
    }
}
