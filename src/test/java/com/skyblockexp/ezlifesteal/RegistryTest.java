package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.config.AdminConfigAdapter;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.config.SmurfConfigAdapter;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.integration.SeasonsIntegrationState;
import com.skyblockexp.ezlifesteal.integration.VaultIntegrationState;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.listener.PlayerListener;
import com.skyblockexp.ezlifesteal.listener.SeasonsServiceListener;
import com.skyblockexp.ezlifesteal.listener.VaultServiceListener;
import com.skyblockexp.ezlifesteal.overlay.HeartOverlayManager;
import com.skyblockexp.ezlifesteal.placeholder.PlaceholderHook;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.io.File;
import java.util.concurrent.ExecutorService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class RegistryTest {

    @Test
    void initializesNestedStatesByDefault() {
        Registry registry = new Registry();

        assertNotNull(registry.getConfigState());
        assertNotNull(registry.getManagerState());
        assertNotNull(registry.getGameplayState());
        assertNotNull(registry.getIntegrationState());
        assertNotNull(registry.getVaultIntegrationState());
        assertNotNull(registry.getSeasonsIntegrationState());
        assertEquals("en", registry.getConfigState().getActiveLanguage());
    }

    @Test
    void supportsGetterSetterRoundTripsForRuntimeMutableReferences() {
        Registry registry = new Registry();
        Registry.ConfigState configState = registry.getConfigState();
        Registry.ManagerState managerState = registry.getManagerState();

        ConfigLoader configLoader = mock(ConfigLoader.class);
        YamlConfiguration adminConfig = mock(YamlConfiguration.class);
        YamlConfiguration smurfConfig = mock(YamlConfiguration.class);
        YamlConfiguration storageConfig = mock(YamlConfiguration.class);
        YamlConfiguration lifestealConfig = mock(YamlConfiguration.class);
        YamlConfiguration heartsConfig = mock(YamlConfiguration.class);
        YamlConfiguration shopConfig = mock(YamlConfiguration.class);
        YamlConfiguration featureConfig = mock(YamlConfiguration.class);
        File adminConfigFile = new File("admin.yml");
        File smurfConfigFile = new File("smurf.yml");
        File storageConfigFile = new File("storage.yml");
        File lifestealConfigFile = new File("lifesteal.yml");
        File heartsConfigFile = new File("hearts.yml");
        File shopConfigFile = new File("shop.yml");
        File featureConfigFile = new File("features.yml");
        AdminConfigAdapter adminConfigAdapter = mock(AdminConfigAdapter.class);
        LifestealConfigAdapter lifestealConfigAdapter = mock(LifestealConfigAdapter.class);
        SmurfConfigAdapter smurfConfigAdapter = mock(SmurfConfigAdapter.class);

        Storage storage = mock(Storage.class);
        LifestealManager lifestealManager = mock(LifestealManager.class);
        PlayerListener playerListener = mock(PlayerListener.class);
        PlaceholderHook placeholderHook = mock(PlaceholderHook.class);
        HeartOverlayManager heartOverlayManager = mock(HeartOverlayManager.class);
        KillStreakManager killStreakManager = mock(KillStreakManager.class);
        TopHologramManager topHologramManager = mock(TopHologramManager.class);
        PlayerLookupService playerLookupService = mock(PlayerLookupService.class);
        ExecutorService storageExecutor = mock(ExecutorService.class);

        configState.setConfigLoader(configLoader);
        configState.setAdminConfig(adminConfig);
        configState.setSmurfConfig(smurfConfig);
        configState.setStorageConfig(storageConfig);
        configState.setLifestealConfig(lifestealConfig);
        configState.setHeartsConfig(heartsConfig);
        configState.setShopConfig(shopConfig);
        configState.setFeatureConfig(featureConfig);
        configState.setAdminConfigFile(adminConfigFile);
        configState.setSmurfConfigFile(smurfConfigFile);
        configState.setStorageConfigFile(storageConfigFile);
        configState.setLifestealConfigFile(lifestealConfigFile);
        configState.setHeartsConfigFile(heartsConfigFile);
        configState.setShopConfigFile(shopConfigFile);
        configState.setFeatureConfigFile(featureConfigFile);
        configState.setAdminConfigAdapter(adminConfigAdapter);
        configState.setLifestealConfigAdapter(lifestealConfigAdapter);
        configState.setSmurfConfigAdapter(smurfConfigAdapter);
        configState.setActiveLanguage("nl");

        managerState.setStorage(storage);
        managerState.setLifestealManager(lifestealManager);
        managerState.setPlayerListener(playerListener);
        managerState.setPlaceholderExpansion(placeholderHook);
        managerState.setHeartOverlayManager(heartOverlayManager);
        managerState.setKillStreakManager(killStreakManager);
        managerState.setTopHologramManager(topHologramManager);
        managerState.setPlayerLookupService(playerLookupService);
        managerState.setStorageExecutor(storageExecutor);

        assertSame(configLoader, configState.getConfigLoader());
        assertSame(adminConfig, configState.getAdminConfig());
        assertSame(smurfConfig, configState.getSmurfConfig());
        assertSame(storageConfig, configState.getStorageConfig());
        assertSame(lifestealConfig, configState.getLifestealConfig());
        assertSame(heartsConfig, configState.getHeartsConfig());
        assertSame(shopConfig, configState.getShopConfig());
        assertSame(featureConfig, configState.getFeatureConfig());
        assertSame(adminConfigFile, configState.getAdminConfigFile());
        assertSame(smurfConfigFile, configState.getSmurfConfigFile());
        assertSame(storageConfigFile, configState.getStorageConfigFile());
        assertSame(lifestealConfigFile, configState.getLifestealConfigFile());
        assertSame(heartsConfigFile, configState.getHeartsConfigFile());
        assertSame(shopConfigFile, configState.getShopConfigFile());
        assertSame(featureConfigFile, configState.getFeatureConfigFile());
        assertSame(adminConfigAdapter, configState.getAdminConfigAdapter());
        assertSame(lifestealConfigAdapter, configState.getLifestealConfigAdapter());
        assertSame(smurfConfigAdapter, configState.getSmurfConfigAdapter());
        assertEquals("nl", configState.getActiveLanguage());

        assertSame(storage, managerState.getStorage());
        assertSame(lifestealManager, managerState.getLifestealManager());
        assertSame(playerListener, managerState.getPlayerListener());
        assertSame(placeholderHook, managerState.getPlaceholderExpansion());
        assertSame(heartOverlayManager, managerState.getHeartOverlayManager());
        assertSame(killStreakManager, managerState.getKillStreakManager());
        assertSame(topHologramManager, managerState.getTopHologramManager());
        assertSame(playerLookupService, managerState.getPlayerLookupService());
        assertSame(storageExecutor, managerState.getStorageExecutor());
    }

    @Test
    void keepsTopLevelAndNestedAccessorsCrossLinked() {
        Registry registry = new Registry();

        PlayerListener playerListener = mock(PlayerListener.class);
        PlayerLookupService playerLookupService = mock(PlayerLookupService.class);
        VaultServiceListener vaultServiceListener = mock(VaultServiceListener.class);
        SeasonsServiceListener seasonsServiceListener = mock(SeasonsServiceListener.class);

        registry.setPlayerListener(playerListener);
        registry.setPlayerLookupService(playerLookupService);
        registry.setVaultServiceListener(vaultServiceListener);
        registry.setSeasonsServiceListener(seasonsServiceListener);

        assertSame(playerListener, registry.getPlayerListener());
        assertSame(playerListener, registry.getManagerState().getPlayerListener());
        assertSame(playerLookupService, registry.getPlayerLookupService());
        assertSame(playerLookupService, registry.getManagerState().getPlayerLookupService());
        assertSame(vaultServiceListener, registry.getVaultServiceListener());
        assertSame(vaultServiceListener, registry.getIntegrationState().getVaultServiceListener());
        assertSame(seasonsServiceListener, registry.getSeasonsServiceListener());
        assertSame(seasonsServiceListener, registry.getIntegrationState().getSeasonsServiceListener());

        VaultIntegrationState vaultIntegrationState = registry.getVaultIntegrationState();
        SeasonsIntegrationState seasonsIntegrationState = registry.getSeasonsIntegrationState();

        assertSame(vaultIntegrationState, registry.getIntegrationState().getVaultIntegrationState());
        assertSame(seasonsIntegrationState, registry.getIntegrationState().getSeasonsIntegrationState());
    }

    @Test
    void allowsNullAssignmentsForCleanupPaths() {
        Registry registry = new Registry();
        Registry.ConfigState configState = registry.getConfigState();
        Registry.ManagerState managerState = registry.getManagerState();

        configState.setAdminConfig(mock(YamlConfiguration.class));
        managerState.setPlayerListener(mock(PlayerListener.class));
        registry.setPlayerLookupService(mock(PlayerLookupService.class));
        registry.setVaultServiceListener(mock(VaultServiceListener.class));

        configState.setAdminConfig(null);
        managerState.setPlayerListener(null);
        registry.setPlayerLookupService(null);
        registry.setVaultServiceListener(null);

        assertNull(configState.getAdminConfig());
        assertNull(managerState.getPlayerListener());
        assertNull(registry.getPlayerLookupService());
        assertNull(registry.getVaultServiceListener());
    }
}
