package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import java.util.List;

/**
 * Encapsulates creation and wiring of runtime managers.
 */
public class ManagerService {
    private final DefaultPluginRuntimeServices runtime;

    private final AdminSettingsFactory adminSettingsFactory;

    private final LifestealSettingsFactory lifestealSettingsFactory;

    private final SmurfSettingsFactory smurfSettingsFactory;

    private final GameplayStateApplier gameplayStateApplier;

    private final ManagerStateInitializer managerStateInitializer;


    public ManagerService(
            DefaultPluginRuntimeServices runtime,
            AdminSettingsFactory adminSettingsFactory,
            LifestealSettingsFactory lifestealSettingsFactory,
            SmurfSettingsFactory smurfSettingsFactory,
            GameplayStateApplier gameplayStateApplier,
            ManagerStateInitializer managerStateInitializer
    ) {
        this.runtime = runtime;
        this.adminSettingsFactory = adminSettingsFactory;
        this.lifestealSettingsFactory = lifestealSettingsFactory;
        this.smurfSettingsFactory = smurfSettingsFactory;
        this.gameplayStateApplier = gameplayStateApplier;
        this.managerStateInitializer = managerStateInitializer;
    }

    public void setupManagers() {
        final AdminSettingsFactory.AdminSettings adminSettings =
                adminSettingsFactory.create(runtime.getAdminConfigAdapter());
        final LifestealSettingsFactory.LifestealSettings lifestealSettings =
                lifestealSettingsFactory.create(runtime.getLifestealConfigAdapter());
        final SmurfSettingsFactory.SmurfSettings smurfSettings = smurfSettingsFactory.create(
                runtime.getSmurfConfigAdapter(),
                runtime.getMessageService(),
                adminSettings.adminDetector(),
                adminSettings.restrictSmurfAlertsToAdmins()
        );

        logWarnings(adminSettings.warnings());
        logWarnings(lifestealSettings.warnings());
        logWarnings(smurfSettings.warnings());

        if (lifestealSettings.heartBoundsAdjusted()) {
            runtime.setLifestealValue("min-hearts", lifestealSettings.minHearts());
            runtime.setLifestealValue("default-hearts", lifestealSettings.defaultHearts());
            runtime.setLifestealValue("max-hearts", lifestealSettings.maxHearts());
            runtime.saveLifestealSettings();
        }

        runtime.parseWorldOverrides();
        runtime.parseMobRewards();
        final var killStreakSettings = runtime.parseKillStreakSettings();

        final ProfileRepository profileRepository = runtime.getProfileRepository();
        managerStateInitializer.initialize(
                runtime.getRegistry().getManagerState(),
                profileRepository,
                lifestealSettings,
                killStreakSettings,
                runtime.getKillStreakManager()
        );

        gameplayStateApplier.apply(
                runtime.getRegistry().getGameplayState(),
                lifestealSettings,
                adminSettings,
                killStreakSettings,
                runtime.getEnabledWorlds(),
                runtime.getDisabledWorlds()
        );

        runtime.setAdminDetector(adminSettings.adminDetector());
        runtime.setSmurfDetector(smurfSettings.smurfDetector());
    }

    private void logWarnings(List<String> warnings) {
        for (String warning : warnings) {
            runtime.getLogger().warning(warning);
        }
    }

    public void shutdownManagers() {
        final var managerState = runtime.getRegistry().getManagerState();
        final LifestealManager lm = managerState.getLifestealManager();
        if (lm != null) {
            lm.saveAllSync();
        }
        final var hom = managerState.getHeartOverlayManager();
        if (hom != null) {
            hom.shutdown();
        }
        final var thm = managerState.getTopHologramManager();
        if (thm != null) {
            thm.shutdown();
        }
        if (managerState.getPlaceholderExpansion() != null) {
            managerState.getPlaceholderExpansion().unregisterExpansion();
            managerState.setPlaceholderExpansion(null);
        }
        if (managerState.getKillStreakManager() != null) {
            managerState.getKillStreakManager().clear();
        }
    }
}
