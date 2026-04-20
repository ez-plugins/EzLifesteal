package com.skyblockexp.ezlifesteal.runtime;

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
import com.skyblockexp.ezlifesteal.runtime.state.GameplayState;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.io.File;
import java.util.concurrent.ExecutorService;
import org.bukkit.configuration.file.YamlConfiguration;

public class Registry {

    private Bootstrap bootstrap;

    private final IntegrationState integrationState = new IntegrationState();

    private final ConfigState configState = new ConfigState();

    private final ManagerState managerState = new ManagerState();

    private final GameplayState gameplayState = GameplayState.defaults();


    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public IntegrationState getIntegrationState() {
        return integrationState;
    }

    public ConfigState getConfigState() {
        return configState;
    }

    public ManagerState getManagerState() {
        return managerState;
    }

    public GameplayState getGameplayState() {
        return gameplayState;
    }

    public PlayerLookupService getPlayerLookupService() {
        return managerState.getPlayerLookupService();
    }

    public void setPlayerLookupService(PlayerLookupService playerLookupService) {
        managerState.setPlayerLookupService(playerLookupService);
    }

    public PlayerListener getPlayerListener() {
        return managerState.getPlayerListener();
    }

    public void setPlayerListener(PlayerListener playerListener) {
        managerState.setPlayerListener(playerListener);
    }

    public VaultServiceListener getVaultServiceListener() {
        return integrationState.getVaultServiceListener();
    }

    public void setVaultServiceListener(VaultServiceListener vaultServiceListener) {
        integrationState.setVaultServiceListener(vaultServiceListener);
    }

    public SeasonsServiceListener getSeasonsServiceListener() {
        return integrationState.getSeasonsServiceListener();
    }

    public void setSeasonsServiceListener(SeasonsServiceListener seasonsServiceListener) {
        integrationState.setSeasonsServiceListener(seasonsServiceListener);
    }

    public VaultIntegrationState getVaultIntegrationState() {
        return integrationState.getVaultIntegrationState();
    }

    public SeasonsIntegrationState getSeasonsIntegrationState() {
        return integrationState.getSeasonsIntegrationState();
    }

    public static final class IntegrationState {
        private VaultServiceListener vaultServiceListener;

        private SeasonsServiceListener seasonsServiceListener;

        private final VaultIntegrationState vaultIntegrationState = new VaultIntegrationState();

        private final SeasonsIntegrationState seasonsIntegrationState = new SeasonsIntegrationState();


        public VaultServiceListener getVaultServiceListener() {
            return vaultServiceListener;
        }

        public void setVaultServiceListener(VaultServiceListener vaultServiceListener) {
            this.vaultServiceListener = vaultServiceListener;
        }

        public SeasonsServiceListener getSeasonsServiceListener() {
            return seasonsServiceListener;
        }

        public void setSeasonsServiceListener(SeasonsServiceListener seasonsServiceListener) {
            this.seasonsServiceListener = seasonsServiceListener;
        }

        public VaultIntegrationState getVaultIntegrationState() {
            return vaultIntegrationState;
        }

        public SeasonsIntegrationState getSeasonsIntegrationState() {
            return seasonsIntegrationState;
        }
    }

    public static class ConfigState {
        private ConfigLoader configLoader;

        private YamlConfiguration adminConfig;

        private File adminConfigFile;

        private YamlConfiguration smurfConfig;

        private File smurfConfigFile;

        private YamlConfiguration storageConfig;

        private File storageConfigFile;

        private YamlConfiguration lifestealConfig;

        private File lifestealConfigFile;

        private YamlConfiguration heartsConfig;

        private File heartsConfigFile;

        private YamlConfiguration shopConfig;

        private File shopConfigFile;

        private YamlConfiguration featureConfig;

        private File featureConfigFile;

        private AdminConfigAdapter adminConfigAdapter;

        private LifestealConfigAdapter lifestealConfigAdapter;

        private SmurfConfigAdapter smurfConfigAdapter;

        private String activeLanguage = "en";


        public ConfigLoader getConfigLoader() {
            return configLoader;
        }

        public void setConfigLoader(ConfigLoader configLoader) {
            this.configLoader = configLoader;
        }

        public YamlConfiguration getAdminConfig() {
            return adminConfig;
        }

        public void setAdminConfig(YamlConfiguration adminConfig) {
            this.adminConfig = adminConfig;
        }

        public File getAdminConfigFile() {
            return adminConfigFile;
        }

        public void setAdminConfigFile(File adminConfigFile) {
            this.adminConfigFile = adminConfigFile;
        }

        public YamlConfiguration getSmurfConfig() {
            return smurfConfig;
        }

        public void setSmurfConfig(YamlConfiguration smurfConfig) {
            this.smurfConfig = smurfConfig;
        }

        public File getSmurfConfigFile() {
            return smurfConfigFile;
        }

        public void setSmurfConfigFile(File smurfConfigFile) {
            this.smurfConfigFile = smurfConfigFile;
        }

        public YamlConfiguration getStorageConfig() {
            return storageConfig;
        }

        public void setStorageConfig(YamlConfiguration storageConfig) {
            this.storageConfig = storageConfig;
        }

        public File getStorageConfigFile() {
            return storageConfigFile;
        }

        public void setStorageConfigFile(File storageConfigFile) {
            this.storageConfigFile = storageConfigFile;
        }

        public YamlConfiguration getLifestealConfig() {
            return lifestealConfig;
        }

        public void setLifestealConfig(YamlConfiguration lifestealConfig) {
            this.lifestealConfig = lifestealConfig;
        }

        public File getLifestealConfigFile() {
            return lifestealConfigFile;
        }

        public void setLifestealConfigFile(File lifestealConfigFile) {
            this.lifestealConfigFile = lifestealConfigFile;
        }

        public YamlConfiguration getHeartsConfig() {
            return heartsConfig;
        }

        public void setHeartsConfig(YamlConfiguration heartsConfig) {
            this.heartsConfig = heartsConfig;
        }

        public File getHeartsConfigFile() {
            return heartsConfigFile;
        }

        public void setHeartsConfigFile(File heartsConfigFile) {
            this.heartsConfigFile = heartsConfigFile;
        }

        public YamlConfiguration getShopConfig() {
            return shopConfig;
        }

        public void setShopConfig(YamlConfiguration shopConfig) {
            this.shopConfig = shopConfig;
        }

        public File getShopConfigFile() {
            return shopConfigFile;
        }

        public void setShopConfigFile(File shopConfigFile) {
            this.shopConfigFile = shopConfigFile;
        }

        public YamlConfiguration getFeatureConfig() {
            return featureConfig;
        }

        public void setFeatureConfig(YamlConfiguration featureConfig) {
            this.featureConfig = featureConfig;
        }

        public File getFeatureConfigFile() {
            return featureConfigFile;
        }

        public void setFeatureConfigFile(File featureConfigFile) {
            this.featureConfigFile = featureConfigFile;
        }

        public AdminConfigAdapter getAdminConfigAdapter() {
            return adminConfigAdapter;
        }

        public void setAdminConfigAdapter(AdminConfigAdapter adminConfigAdapter) {
            this.adminConfigAdapter = adminConfigAdapter;
        }

        public LifestealConfigAdapter getLifestealConfigAdapter() {
            return lifestealConfigAdapter;
        }

        public void setLifestealConfigAdapter(LifestealConfigAdapter lifestealConfigAdapter) {
            this.lifestealConfigAdapter = lifestealConfigAdapter;
        }

        public SmurfConfigAdapter getSmurfConfigAdapter() {
            return smurfConfigAdapter;
        }

        public void setSmurfConfigAdapter(SmurfConfigAdapter smurfConfigAdapter) {
            this.smurfConfigAdapter = smurfConfigAdapter;
        }

        public String getActiveLanguage() {
            return activeLanguage;
        }

        public void setActiveLanguage(String activeLanguage) {
            this.activeLanguage = activeLanguage;
        }

    }

    public static class ManagerState {
        private Storage storage;

        private LifestealManager lifestealManager;

        private PlayerListener playerListener;

        private PlaceholderHook placeholderExpansion;

        private HeartOverlayManager heartOverlayManager;

        private KillStreakManager killStreakManager;

        private TopHologramManager topHologramManager;

        private PlayerLookupService playerLookupService;

        private ExecutorService storageExecutor;


        public Storage getStorage() {
            return storage;
        }

        public void setStorage(Storage storage) {
            this.storage = storage;
        }

        public LifestealManager getLifestealManager() {
            return lifestealManager;
        }

        public void setLifestealManager(LifestealManager lifestealManager) {
            this.lifestealManager = lifestealManager;
        }

        public PlayerListener getPlayerListener() {
            return playerListener;
        }

        public void setPlayerListener(PlayerListener playerListener) {
            this.playerListener = playerListener;
        }

        public PlaceholderHook getPlaceholderExpansion() {
            return placeholderExpansion;
        }

        public void setPlaceholderExpansion(PlaceholderHook placeholderExpansion) {
            this.placeholderExpansion = placeholderExpansion;
        }

        public HeartOverlayManager getHeartOverlayManager() {
            return heartOverlayManager;
        }

        public void setHeartOverlayManager(HeartOverlayManager heartOverlayManager) {
            this.heartOverlayManager = heartOverlayManager;
        }

        public KillStreakManager getKillStreakManager() {
            return killStreakManager;
        }

        public void setKillStreakManager(KillStreakManager killStreakManager) {
            this.killStreakManager = killStreakManager;
        }

        public TopHologramManager getTopHologramManager() {
            return topHologramManager;
        }

        public void setTopHologramManager(TopHologramManager topHologramManager) {
            this.topHologramManager = topHologramManager;
        }

        public PlayerLookupService getPlayerLookupService() {
            return playerLookupService;
        }

        public void setPlayerLookupService(PlayerLookupService playerLookupService) {
            this.playerLookupService = playerLookupService;
        }

        public ExecutorService getStorageExecutor() {
            return storageExecutor;
        }

        public void setStorageExecutor(ExecutorService storageExecutor) {
            this.storageExecutor = storageExecutor;
        }

    }

}
