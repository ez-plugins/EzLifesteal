package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.AdminConfigAdapter;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import com.skyblockexp.ezlifesteal.config.LegacyConfigResolver;
import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.config.SmurfConfigAdapter;
import com.skyblockexp.ezlifesteal.compat.AdapterSupport;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakReward;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings;
import com.skyblockexp.ezlifesteal.gui.BeaconGuiListener;
import com.skyblockexp.ezlifesteal.integration.BeaconAreaProtection;
import com.skyblockexp.ezlifesteal.integration.BeaconCountdownProvider;
import com.skyblockexp.ezlifesteal.integration.EzCountdownBeaconHook;
import com.skyblockexp.ezlifesteal.integration.TeamKillBypassService;
import com.skyblockexp.ezlifesteal.integration.TeamsApiTeamResolver;
import com.skyblockexp.ezlifesteal.integration.WorldGuardBeaconHook;
import com.skyblockexp.ezlifesteal.listener.PlayerListener;
import com.skyblockexp.ezlifesteal.listener.SpawnedBeaconListener;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.overlay.HeartOverlayManager;
import com.skyblockexp.ezlifesteal.placeholder.PlaceholderHook;
import com.skyblockexp.ezlifesteal.service.BeaconAvailabilityService;
import com.skyblockexp.ezlifesteal.service.BeaconScheduleService;
import com.skyblockexp.ezlifesteal.service.BeaconSpawnService;
import com.skyblockexp.ezlifesteal.storage.SpawnedBeaconRepository;
import com.skyblockexp.ezlifesteal.runtime.state.GameplayState;
import com.skyblockexp.ezlifesteal.service.AdminSettingsFactory;
import com.skyblockexp.ezlifesteal.service.ConfigService;
import com.skyblockexp.ezlifesteal.service.GameplayParsingService;
import com.skyblockexp.ezlifesteal.service.GameplayStateApplier;
import com.skyblockexp.ezlifesteal.service.HologramService;
import com.skyblockexp.ezlifesteal.service.IntegrationService;
import com.skyblockexp.ezlifesteal.service.KillStreakParsingService;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.service.LifestealSettingsFactory;
import com.skyblockexp.ezlifesteal.service.ListenerService;
import com.skyblockexp.ezlifesteal.service.ManagerService;
import com.skyblockexp.ezlifesteal.service.ManagerStateInitializer;
import com.skyblockexp.ezlifesteal.service.OverlayService;
import com.skyblockexp.ezlifesteal.service.PlaceholderService;
import com.skyblockexp.ezlifesteal.service.RecipeService;
import com.skyblockexp.ezlifesteal.service.SmurfSettingsFactory;
import com.skyblockexp.ezlifesteal.service.StorageService;
import com.skyblockexp.ezlifesteal.service.TeamBankService;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.RepositoryBackedStorageBridge;
import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.mysql.MySqlStorageProvider;
import com.skyblockexp.ezlifesteal.storage.provider.StorageProvider;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import com.skyblockexp.ezlifesteal.storage.yaml.YamlStorageProvider;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import com.skyblockexp.ezlifesteal.util.PluginLifecycleSupport;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import com.skyblockexp.ezlifesteal.util.ban.BanEntryView;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class DefaultPluginRuntimeServices {

    private final EzLifestealPlugin plugin;

    private final Registry registry;

    private final com.skyblockexp.ezlifesteal.runtime.PluginContext pluginContext;

    private final Registry.ConfigState configState;

    private final Registry.ManagerState managerState;

    private final GameplayState gameplayState;

    private final RuntimeCompatibilityAdapter compatibilityAdapter;

    private final PlatformBanAdapter banAdapter;

    public DefaultPluginRuntimeServices(EzLifestealPlugin plugin, Registry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.pluginContext = new com.skyblockexp.ezlifesteal.runtime.PluginContext(registry);
        this.configState = registry.getConfigState();
        this.managerState = registry.getManagerState();
        this.gameplayState = registry.getGameplayState();
        this.compatibilityAdapter = new RuntimeCompatibilityAdapter(this, registry);
        this.banAdapter = PlatformBanAdapter.create();
    }

    // Seasons integration candidate names (compatibility constants)
    private static final String SEASONS_PLUGIN_NAME = "EzSeasons";

    private static final String SEASONS_API_CLASS_NAME = "com.skyblockexp.lifesteal.seasons.api.SeasonsApi";

    private static final String LEGACY_SEASONS_API_CLASS_NAME = "com.ezlifesteal.seasons.api.SeasonsApi";

    private static final String SEASONS_INTEGRATION_CLASS_NAME =
            "com.skyblockexp.lifesteal.seasons.integration.LifestealIntegration";

    private static final String LEGACY_SEASONS_INTEGRATION_CLASS_NAME =
            "com.skyblockexp.lifesteal.seasons.api.SeasonsIntegration";

        private static final String EZCOUNTDOWN_END_EVENT_CLASS_NAME =
            "com.skyblockexp.ezcountdown.api.event.CountdownEndEvent";


    public java.util.logging.Logger getLogger() {
        return plugin.getLogger();
    }

    public EzLifestealPlugin getPlugin() {
        return plugin;
    }

    public PlatformBanAdapter getBanAdapter() {
        return banAdapter;
    }

    public java.io.File getDataFolder() {
        return plugin.getDataFolder();
    }

    public org.bukkit.Server getServer() {
        return plugin.getServer();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public ClassLoader getClassLoader() {
        return plugin.getClass().getClassLoader();
    }

    private YamlConfiguration heartsConfig;

    private File heartsConfigFile;

    private YamlConfiguration shopConfig;

    private File shopConfigFile;

    private YamlConfiguration featureConfig;

    private File featureConfigFile;

    private AdminConfigAdapter adminConfigAdapter;

    private LifestealConfigAdapter lifestealConfigAdapter;

    private SmurfConfigAdapter smurfConfigAdapter;

    private com.skyblockexp.ezlifesteal.heart.HeartRegistry heartRegistry;

    private final java.util.Set<NamespacedKey> registeredHeartRecipes = new java.util.HashSet<>();

    private String activeLanguage = "en";

    private ConfigLoader configLoader;

    private ConfigService configService;

    private StorageService storageService;

    private IntegrationService integrationService;

    private RecipeService recipeService;

    private ManagerService managerService;

    private HologramService hologramService;

    private OverlayService overlayService;

    private ListenerService listenerService;

    private KillStreakParsingService killStreakParsingService;

    private GameplayParsingService gameplayParsingService;

    private PlaceholderService placeholderService;

    private AdminDetector adminDetector;

    private SmurfDetector smurfDetector;

    // Legacy runtime fields (kept for compatibility during refactor)

    private MessageService messageService;

    private File adminConfigFile;

    private File smurfConfigFile;

    private File storageConfigFile;

    private File lifestealCoreConfigFile;

    private File reviveBeaconConfigFile;

    private File lifestealDropsConfigFile;

    private File lifestealWorldsConfigFile;

    private File lifestealMobsConfigFile;

    private File lifestealKillStreaksConfigFile;

    private YamlConfiguration adminConfig;

    private YamlConfiguration smurfConfig;

    private YamlConfiguration storageConfig;

    private YamlConfiguration lifestealCoreConfig;

    private YamlConfiguration reviveBeaconConfig;

    private YamlConfiguration lifestealDropsConfig;

    private YamlConfiguration lifestealWorldsConfig;

    private YamlConfiguration lifestealMobsConfig;

    private YamlConfiguration lifestealKillStreaksConfig;

    private Storage storage;

    private StorageProvider storageProvider;

    private ProfileRepository profileRepository;

    private BanRepository banRepository;

    private TeamBankRepository teamBankRepository;

    private String storageSummary;

    private ExecutorService storageExecutor;

    private PlayerLookupService playerLookupService;

    private LifestealManager lifestealManager;

    private HeartOverlayManager heartOverlayManager;

    private TopHologramManager topHologramManager;

    private PlaceholderHook placeholderExpansion;

    private KillStreakManager killStreakManager;

    private List<String> zeroHeartCommands;

    private double heartsPerKill;

    private double heartsLostOnDeath;

    private double mobRemoveHeartsGreaterThan;

    private boolean dropHeartOnDeath;

    private boolean dropHeartOnlyWhenKilledByPlayer;

    private boolean dropHeartOnKill;

    private boolean banWhenZeroHearts;

    private boolean globalLifestealEnabled;

    private boolean applyHealthScale;

    private boolean dontRemoveHeartsFromMobs;

    private boolean combatLogoutProtectionEnabled;

    private long combatLogoutTagDurationMillis;

    private boolean smurfDetectionEnabled;

    private boolean restrictSmurfAlertsToAdmins;

    private boolean adminBypassHeartLoss;

    private boolean adminBypassHeartGain;

    private String dropHeartId;

    private String zeroHeartBanMessage;

    private String zeroHeartKickMessage;

    private Map<String, Double> heartsPerKillOverrides;

    private Map<String, Double> heartsLostOnDeathOverrides;

    private Map<String, Boolean> banWhenZeroOverrides;

    private Map<EntityType, MobReward> mobRewards;

    private Set<String> enabledWorlds = Collections.emptySet();

    private Set<String> disabledWorlds = Collections.emptySet();

    private KillStreakSettings killStreakSettings;

    private int dropHeartAmount;

    private PlayerListener playerListener;

    private BeaconSpawnService beaconSpawnService;

    private BeaconScheduleService beaconScheduleService;

    private PluginAccessor beaconPluginAccessor;

    private TeamKillBypassService teamKillBypassService;

    private TeamsApiTeamResolver teamsApiTeamResolver;

    private TeamBankService teamBankService;

    private com.skyblockexp.ezlifesteal.service.TeamBankAdminService teamBankAdminService;


    public void initializeCoreState() {

        EzLifestealPlugin.HEART_KEY = new NamespacedKey(plugin, "heart_id");

        configLoader = new ConfigLoader(plugin);

        configService = new ConfigService(plugin, configLoader);

        configState.setConfigLoader(configLoader);

        storageService = new StorageService(plugin, registry, configLoader);

        integrationService = new IntegrationService(this, registry);

        recipeService = new RecipeService(this);

        managerService = new ManagerService(

                this,

                new AdminSettingsFactory(),

                new LifestealSettingsFactory(),

                new SmurfSettingsFactory(),

                new GameplayStateApplier(),

                new ManagerStateInitializer(plugin)

        );

        hologramService = new HologramService(this, plugin);

        overlayService = new OverlayService(this, plugin);

        listenerService = new ListenerService(this, plugin);
        placeholderService = new PlaceholderService(this, plugin);
        killStreakParsingService = new KillStreakParsingService(this);
        gameplayParsingService = new GameplayParsingService(this);
        playerLookupService = new PlayerLookupService(plugin);
        teamKillBypassService = new TeamKillBypassService(this);
        teamsApiTeamResolver = new TeamsApiTeamResolver(this);
        teamBankService = new TeamBankService(new RuntimePluginFacade(plugin, this), teamsApiTeamResolver);
        teamBankAdminService = new com.skyblockexp.ezlifesteal.service.TeamBankAdminService(
                new RuntimePluginFacade(plugin, this), teamsApiTeamResolver, getStorageExecutor());
        managerState.setPlayerLookupService(playerLookupService);
        registry.setPlayerLookupService(playerLookupService);
    }

    public void setupCoreListeners(PluginAccessor pluginAccessor) {
        if (listenerService != null) {
            listenerService.setupCoreListeners(pluginAccessor);
        }
    }

    public void clearHeartRecipesSafely() {
        try {
            clearRegisteredHeartRecipes();
        }
        catch (Throwable ignored) {
        }
    }

    public void shutdownManagers() {
        requireManagerService().shutdownManagers();
        compatibilityAdapter.syncManagerFields();
        compatibilityAdapter.clearPlaceholderField();
    }

    public void closeStorage() {
        if (storageService != null) {
            storageService.closeStorage();
        }
        else {
            PluginLifecycleSupport.closeStorage(storage, getLogger());
        }
        storage = null;
        storageProvider = null;
        profileRepository = null;
        banRepository = null;
        teamBankRepository = null;
    }

    public void shutdownStorageExecutor() {
        if (storageService != null) {
            storageService.shutdownStorageExecutor();
        }
        else {
            PluginLifecycleSupport.shutdownExecutor(storageExecutor, 5, TimeUnit.SECONDS);
        }
    }

    public void initializeStorageExecutor() {
        if (storageService != null) {
            storageService.initializeStorageExecutor();
            storageExecutor = storageService.getStorageExecutor();
        }
        else {
            if (storageExecutor != null) {
                storageExecutor.shutdownNow();
            }
            storageExecutor = Executors.newSingleThreadExecutor(r -> {
                final Thread thread = new Thread(r, "EzLifesteal-Storage");
                thread.setDaemon(true);
                return thread;
            });
        }
    }

    public void reloadManagerState() {
        if (lifestealManager != null) {
            lifestealManager.saveAllSync();
        }
        heartRegistry = new com.skyblockexp.ezlifesteal.heart.HeartRegistry(heartsConfig);
        setupManagers();
        setupOverlay();
        setupHologram();
        if (placeholderExpansion != null) {
            placeholderExpansion.clearCache();
        }
    }

    public void reloadServices() {
        initializeStorageExecutor();
        reloadAdditionalConfigs();
        setupMessages();
        reloadManagerState();
        registerHeartRecipes();
        setupVault();
    }

    public PlayerLookupService getPlayerLookupService() {
        return playerLookupService;
    }

    public com.skyblockexp.ezlifesteal.heart.HeartRegistry getHeartRegistry() {
        return heartRegistry;
    }

    public Storage getStorage() {
        return storageService != null ? storageService.getStorage() : storage;
    }

    public ProfileRepository getProfileRepository() {
        return storageService != null ? storageService
                .getProfileRepository() : (profileRepository == null ? storage : profileRepository);
    }

    public BanRepository getBanRepository() {
        return storageService != null ? storageService
                .getBanRepository() : (banRepository == null ? storage : banRepository);
    }

    public TeamBankRepository getTeamBankRepository() {
        return storageService != null ? storageService.getTeamBankRepository() : teamBankRepository;
    }

    public TeamBankService getTeamBankService() {
        return teamBankService;
    }

    public TeamsApiTeamResolver getTeamsApiTeamResolver() {
        return teamsApiTeamResolver;
    }

    public PluginContext getPluginContext() {
        return pluginContext;
    }

    public YamlConfiguration getShopConfig() {
        return configState.getShopConfig() != null ? configState.getShopConfig() : shopConfig;
    }

    public void executeZeroHeartCommands(Player victim, Player killer, double remainingHearts) {
        if (zeroHeartCommands == null || zeroHeartCommands.isEmpty()) {
            return;
        }
        final String victimName = victim.getName() == null ? victim.getUniqueId().toString() : victim.getName();
        final String victimUuid = victim.getUniqueId().toString();
        final String killerName =
                killer == null ? "" : (killer.getName() == null ? killer.getUniqueId().toString() : killer.getName());
        final String killerUuid = killer == null ? "" : killer.getUniqueId().toString();
        final String heartsStr =
                remainingHearts % 1 == 0 ? Integer.toString((int) remainingHearts) : String.format(Locale.US,
                "%.1f", remainingHearts);
        for (String raw : zeroHeartCommands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String cmd = raw;
            cmd = cmd.replace("%victim%", victimName);
            cmd = cmd.replace("%player%", victimName);
            cmd = cmd.replace("%victim_uuid%", victimUuid);
            cmd = cmd.replace("%player_uuid%", victimUuid);

            cmd = cmd.replace("%killer%", killerName);
            cmd = cmd.replace("%killer_uuid%", killerUuid);
            cmd = cmd.replace("%remaining_hearts%", heartsStr);
            cmd = cmd.replace("%hearts%", heartsStr);
            final String toDispatch = cmd;
            SchedulerAdapter.run(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toDispatch));
        }
    }

    public void ensureAdditionalConfigFiles() {
        if (configService != null) {
            configService.ensureAdditionalConfigFiles();
            return;
        }
        configLoader.ensureResources(List.of(
                "admin.yml",
                "smurf.yml",
                "storage.yml",
                "lifesteal-core.yml",
                "lifesteal-drops.yml",
                "lifesteal-worlds.yml",
                "lifesteal-mobs.yml",
                "lifesteal-killstreaks.yml",
                "hearts.yml",
                "shop.yml",
                "features.yml",
                "revive-beacon.yml"
        ));
        // fallback language resources
        configLoader.ensureResources(List.of(
                "languages/en.yml",
                "languages/nl.yml",
                "languages/es.yml",
                "languages/fr.yml",
                "languages/de.yml",
                "languages/pt.yml"
        ));
    }

    private void ensureLanguageResources() {
        configLoader.ensureResources(List.of(
                "languages/en.yml",
                "languages/nl.yml",
                "languages/es.yml",
                "languages/fr.yml",
                "languages/de.yml",
                "languages/pt.yml"
        ));
    }

    public void reloadAdditionalConfigs() {
        adminConfigFile = configLoader.resolveFile("admin.yml");
        smurfConfigFile = configLoader.resolveFile("smurf.yml");
        storageConfigFile = configLoader.resolveFile("storage.yml");
        lifestealCoreConfigFile = configLoader.resolveFile("lifesteal-core.yml");
        lifestealDropsConfigFile = configLoader.resolveFile("lifesteal-drops.yml");
        lifestealWorldsConfigFile = configLoader.resolveFile("lifesteal-worlds.yml");
        lifestealMobsConfigFile = configLoader.resolveFile("lifesteal-mobs.yml");
        lifestealKillStreaksConfigFile = configLoader.resolveFile("lifesteal-killstreaks.yml");
        heartsConfigFile = configLoader.resolveFile("hearts.yml");
        shopConfigFile = configLoader.resolveFile("shop.yml");
        featureConfigFile = configLoader.resolveFile("features.yml");
        reviveBeaconConfigFile = configLoader.resolveFile("revive-beacon.yml");
        adminConfig = configLoader.load("admin.yml");
        smurfConfig = configLoader.load("smurf.yml");
        storageConfig = configLoader.load("storage.yml");
        lifestealCoreConfig = configLoader.load("lifesteal-core.yml");
        lifestealDropsConfig = configLoader.load("lifesteal-drops.yml");
        lifestealWorldsConfig = configLoader.load("lifesteal-worlds.yml");
        lifestealMobsConfig = configLoader.load("lifesteal-mobs.yml");
        lifestealKillStreaksConfig = configLoader.load("lifesteal-killstreaks.yml");
        heartsConfig = configLoader.load("hearts.yml");
        shopConfig = configLoader.load("shop.yml");
        featureConfig = configLoader.load("features.yml");
        reviveBeaconConfig = configLoader.load("revive-beacon.yml");
        // Initialize config adapters
        adminConfigAdapter = new AdminConfigAdapter(adminConfig, getConfig());
        lifestealConfigAdapter = new LifestealConfigAdapter(
                lifestealCoreConfig,
                List.of(lifestealDropsConfig, lifestealWorldsConfig, lifestealMobsConfig, lifestealKillStreaksConfig,
                        reviveBeaconConfig),
                getConfig()
        );
        smurfConfigAdapter = new SmurfConfigAdapter(smurfConfig, getConfig());

        configState.setAdminConfig(adminConfig);
        configState.setAdminConfigFile(adminConfigFile);
        configState.setSmurfConfig(smurfConfig);
        configState.setSmurfConfigFile(smurfConfigFile);
        configState.setStorageConfig(storageConfig);
        configState.setStorageConfigFile(storageConfigFile);
        configState.setLifestealConfig(lifestealCoreConfig);
        configState.setLifestealConfigFile(lifestealCoreConfigFile);
        configState.setHeartsConfig(heartsConfig);
        configState.setHeartsConfigFile(heartsConfigFile);
        configState.setShopConfig(shopConfig);
        configState.setShopConfigFile(shopConfigFile);
        configState.setFeatureConfig(featureConfig);
        configState.setFeatureConfigFile(featureConfigFile);
        configState.setAdminConfigAdapter(adminConfigAdapter);
        configState.setLifestealConfigAdapter(lifestealConfigAdapter);
        configState.setSmurfConfigAdapter(smurfConfigAdapter);
    }

    public void setupMessages() {
        final String configuredLanguage = getConfig().getString("language", "en");
        final YamlConfiguration languageConfiguration = loadLanguageConfiguration(configuredLanguage);

        String prefix = languageConfiguration.getString("prefix");
        if ((prefix == null || prefix.isBlank()) && getConfig().contains("messages.prefix")) {
            prefix = getConfig().getString("messages.prefix");
        }
        messageService = new MessageService(prefix);

        YamlConfiguration englishConfiguration = null;
        if (!"en".equalsIgnoreCase(activeLanguage)) {
            englishConfiguration = loadLanguageFile("en");
        }
        else {
            englishConfiguration = languageConfiguration;
        }

        registerMessagesFromSection(englishConfiguration);
        if (languageConfiguration != null && languageConfiguration != englishConfiguration) {
            registerMessagesFromSection(languageConfiguration);
        }

        final ConfigurationSection legacySection = getConfig().getConfigurationSection("messages");
        registerMessagesFromSection(legacySection);

    }

    private YamlConfiguration loadLanguageConfiguration(String language) {
        String normalized = normalizeLanguage(language);
        YamlConfiguration configuration = loadLanguageFile(normalized);
        if ((configuration == null || configuration.getKeys(false).isEmpty()) && !"en".equals(normalized)) {
            getLogger().warning("Language '" + language + "' is not available; falling back to English.");
            normalized = "en";
            configuration = loadLanguageFile(normalized);
        }
        if (configuration == null) {
            configuration = new YamlConfiguration();
        }
        activeLanguage = normalized;
        configState.setActiveLanguage(normalized);
        return configuration;
    }

    private YamlConfiguration loadLanguageFile(String language) {
        if (configService != null) {
            return configService.loadLanguageFile(language);
        }
        if (language == null || language.isBlank()) {
            return null;
        }
        final String resourcePath = "languages/" + language + ".yml";
        final File target = new File(getDataFolder(), resourcePath);
        if (!target.exists()) {
            configLoader.ensureResources(List.of(resourcePath));
        }
        if (!target.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(target);
    }

    private String normalizeLanguage(String language) {
        if (configService != null) {
            return configService.normalizeLanguage(language);
        }
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        final int separatorIndex = normalized.indexOf('_');
        if (separatorIndex > 0) {
            normalized = normalized.substring(0, separatorIndex);
        }
        return normalized;
    }

    private void registerMessagesFromSection(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            if ("prefix".equalsIgnoreCase(key)) {
                continue;
            }
            if (section.isString(key)) {
                messageService.register(key, section.getString(key));
            }
        }
    }

    public void setupStorage() {
        if (storageService != null) {
            storageService.setupStorage();
            syncStorageStateFromService();
            return;
        }
        setupLegacyStorage();
    }

    private void syncStorageStateFromService() {
        storageSummary = storageService.getStorageSummary();
        storage = storageService.getStorage();
        profileRepository = storageService.getProfileRepository();
        banRepository = storageService.getBanRepository();
        teamBankRepository = storageService.getTeamBankRepository();
        storageExecutor = storageService.getStorageExecutor();
    }

    private void setupLegacyStorage() {
        if (storageConfig == null) {
            return;
        }

        String configuredType = storageConfig.getString("type");
        if ((configuredType == null || configuredType.isBlank()) && getConfig().contains("storage.type")) {
            configuredType = getConfig().getString("storage.type");
        }

        final String type = configuredType == null || configuredType.isBlank()
                ? "YAML"
                : configuredType.toUpperCase(Locale.ROOT);
        if ("YAML".equals(type) && (configuredType == null || configuredType.isBlank())) {
            getLogger().warning("No storage type configured; defaulting to YAML storage.");
            setStorageValue("type", type);
            saveStorageConfiguration();
        }

        storageSummary = "Unknown storage";
        closeLegacyStorage();
        initializeLegacyStorageProvider(type, configuredType);
        initializeLegacyStorageBridge();
    }

    private void closeLegacyStorage() {
        if (storage == null) {
            return;
        }
        try {
            storage.close();
        }
        catch (StorageException exception) {
            getLogger().warning("Failed to close existing storage: " + exception.getMessage());
        }
    }

    private void initializeLegacyStorageProvider(String type, String configuredType) {
        switch (type) {
            case "MYSQL":
                final ConfigurationSection mysql = getStorageSection("mysql");
                if (mysql == null) {
                    throw new IllegalStateException("MySQL storage selected but no configuration provided");
                }
                final String host = mysql.getString("host", "localhost");
                final int port = mysql.getInt("port", 3306);
                final String database = mysql.getString("database", "lifesteal");
                final String username = mysql.getString("username", "root");
                final String password = mysql.getString("password", "password");
                final boolean useSsl = mysql.getBoolean("use-ssl", false);
                final String table = mysql.getString("table", "lifesteal_players");
                storageProvider = new MySqlStorageProvider(host, port, database, username, password, useSsl, table);
                storageSummary = String.format(
                        Locale.US,
                        "MySQL storage (%s:%d/%s, user=%s, table=%s, SSL=%s)",
                        host,
                        port,
                        database,
                        username,
                        table,
                        useSsl ? "on" : "off"
                );
                break;
            case "YML":
            case "YAML":
                final File dataFolder = new File(getDataFolder(), "data");
                final String fileName = "players.yml";
                storageProvider = new YamlStorageProvider(dataFolder, fileName);
                storageSummary = "YAML storage (" + new File(dataFolder, fileName).getPath() + ")";
                break;
            default:
                final String reportedType = configuredType == null ? type : configuredType;
                getLogger().warning("Unknown storage type '" + reportedType + "', defaulting to YAML storage.");
                final File fallbackFolder = new File(getDataFolder(), "data");
                final String fallbackFile = "players.yml";
                storageProvider = new YamlStorageProvider(fallbackFolder, fallbackFile);
                storageSummary = "YAML storage (fallback for unknown type '" + reportedType + "')";
                break;
        }
    }

    private void initializeLegacyStorageBridge() {
        try {
            storage = new RepositoryBackedStorageBridge(storageProvider);
            profileRepository = storageProvider.profiles();
            banRepository = storageProvider.bans();
            teamBankRepository = storageProvider.teamBanks();
            storage.init();
            reconcileRuntimeBans();
        }
        catch (StorageException exception) {
            getLogger().severe("Failed to initialise storage: " + exception.getMessage());
            storage = null;
            storageProvider = null;
            profileRepository = null;
            banRepository = null;
        }
    }

    private void reconcileRuntimeBans() {
        if (storageService != null) {
            storageService.reconcileRuntimeBans();
            return;
        }

        importRuntimeBansIntoStorage();
        final List<BanRecord> activeBans;
        try {
            activeBans = getBanRepository().loadActiveBans();
        }
        catch (StorageException exception) {
            getLogger().warning("Failed to load active bans from storage: " + exception.getMessage());
            return;
        }
        if (activeBans.isEmpty()) {
            return;
        }
        for (BanRecord record : activeBans) {
            final String playerName = record.getPlayerName();
            final UUID playerUuid = record.getUniqueId();
            if (playerName == null || playerName.isBlank() || playerUuid == null) {
                continue;
            }
            if (!banAdapter.isBanned(playerUuid, playerName)) {
                // The ban is active in storage but absent from Bukkit's ban list.
                // This indicates the player was manually pardoned (e.g. /pardon). Sync the
                // removal to storage so the player is not re-banned on the next restart.
                try {
                    getBanRepository().removeBan(playerUuid);
                }
                catch (StorageException exception) {
                    getLogger().warning("Failed to sync pardon for " + playerName
                            + " into storage: " + exception.getMessage());
                }
            }
        }
    }

    private void importRuntimeBansIntoStorage() {
        if (storageService != null) {
            storageService.importRuntimeBansIntoStorage();
            return;
        }
        final BanRepository repository = getBanRepository();
        if (repository == null) {
            return;
        }
        for (BanEntryView entry : banAdapter.getBanEntries()) {
            final UUID uniqueId = entry.getPlayerId();
            if (uniqueId == null) {
                continue;
            }
            final String playerName = entry.getPlayerName();
            if (playerName == null || playerName.isBlank()) {
                continue;
            }
            try {
                if (repository.loadBan(uniqueId).isPresent()) {
                    continue;
                }
                final Date created = entry.getCreated();
                final Date expiresAt = entry.getExpiration();
                final BanRecord imported = new BanRecord(
                        uniqueId,
                        playerName,
                        entry.getReason(),
                        entry.getSource(),
                        created == null ? Instant.now() : created.toInstant(),
                        expiresAt == null ? null : expiresAt.toInstant(),
                        true
                );
                repository.saveBan(imported);
            }
            catch (StorageException exception) {
                getLogger().warning("Failed to import runtime ban for " + playerName + ": " + exception.getMessage());
            }
        }
    }

    public void setupVault() {
        if (integrationService != null) {
            integrationService.setupVault();
            return;
        }
        // fallback to original behavior when integrationService is not initialised
        final Economy previous = registry.getVaultIntegrationState().getEconomy();
        registry.getVaultIntegrationState().setEconomy(null);
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            if (previous != null) {
                getLogger().info("Vault is unavailable; currency rewards are disabled.");
            }
            return;
        }
        if (!ensureVaultEconomyClassesAvailable()) {
            if (previous != null) {
                getLogger().info("Vault economy classes are unavailable; currency rewards are disabled.");
            }
            else {
                getLogger().fine("Vault economy classes are unavailable; skipping currency integration.");
            }
            return;
        }
        try {
            final RegisteredServiceProvider<Economy> registration =
                    getServer().getServicesManager().getRegistration(Economy.class);
            if (registration == null) {
                if (previous != null) {
                    getLogger().info("No Vault economy provider detected; currency rewards are disabled.");
                }
                return;
            }
            final Economy provider = registration.getProvider();
            if (provider == null) {
                if (previous != null) {
                    getLogger().info("Vault returned a null economy provider; currency rewards are disabled.");
                }
                return;
            }
            registry.getVaultIntegrationState().setEconomy(provider);
            if (previous == null || !previous.getName().equalsIgnoreCase(provider.getName())) {
                getLogger().info("Hooked into Vault economy provider: " + provider.getName() + '.');
            }
        }
        catch (NoClassDefFoundError error) {
            registry.getVaultIntegrationState().setEconomy(null);
            registry.getVaultIntegrationState().setEconomyClassesAvailable(false);
            if (previous != null) {
                getLogger().info("Vault economy classes are unavailable; currency rewards are disabled.");
            }
            else {
                getLogger().fine("Vault economy classes are unavailable; skipping currency integration.");
            }
        }
        catch (Throwable throwable) {
            registry.getVaultIntegrationState().setEconomy(null);
            getLogger().warning("Failed to hook into Vault economy: " + throwable.getMessage());
        }
    }

    public boolean ensureVaultEconomyClassesAvailable() {
        if (integrationService != null) {
            return integrationService.ensureVaultEconomyClassesAvailable();
        }
        if (!registry.getVaultIntegrationState().isEconomyClassesAvailable()) {
            return false;
        }
        try {
            Class.forName("net.milkbowl.vault.economy.Economy", false, getClassLoader());
            return true;
        }
        catch (ClassNotFoundException | LinkageError exception) {
            registry.getVaultIntegrationState().setEconomyClassesAvailable(false);
            return false;
        }
    }

    private void setupManagers() {
        requireManagerService().setupManagers();
        compatibilityAdapter.syncManagerFields();
    }

    private void setupOverlay() {
        requireOverlayService().setupOverlay();
        compatibilityAdapter.syncManagerFields();
    }

    private void setupHologram() {
        requireHologramService().setupHologram();
        compatibilityAdapter.syncManagerFields();
    }

    public void setupPlaceholderExpansion(PluginAccessor pluginAccessor) {
        if (placeholderService != null) {
            placeholderService.setupPlaceholderExpansion(pluginAccessor);
        }
    }

    public PlaceholderHook getPlaceholderExpansion() {
        return placeholderExpansion;
    }

    public BeaconSpawnService getBeaconSpawnService() {
        return beaconSpawnService;
    }

    /**
     * Starts the beacon spawn feature by detecting optional integrations (WorldGuard, EzCountdown),
     * creating the services, registering the listener, and starting the schedule.
     */
    public void startBeaconSpawnFeature(PluginAccessor pluginAccessor) {
        this.beaconPluginAccessor = pluginAccessor;

        // Stop any previous run (in case of reload)
        stopBeaconSpawnFeature();

        if (!pluginAccessor.getBeaconSpawnSettings().enabled()) {
            return;
        }

        final SpawnedBeaconRepository repository = new SpawnedBeaconRepository();
        final BeaconAvailabilityService availabilityService =
                new BeaconAvailabilityService(plugin.getLogger());
        beaconSpawnService = new BeaconSpawnService(repository, availabilityService, plugin.getLogger());

        // Optional WorldGuard integration
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            try {
                final BeaconAreaProtection hook = new WorldGuardBeaconHook(plugin.getLogger());
                beaconSpawnService.setAreaProtection(hook);
                plugin.getLogger().info("BeaconSpawnFeature: WorldGuard protection enabled.");
            } catch (Throwable t) {
                plugin.getLogger().warning("BeaconSpawnFeature: WorldGuard hook failed: " + t.getMessage());
            }
        }

        // Optional EzCountdown integration
        if (Bukkit.getPluginManager().isPluginEnabled("EzCountdown")) {
            try {
                final BeaconCountdownProvider hook = new EzCountdownBeaconHook(plugin.getLogger());
                beaconSpawnService.setCountdownProvider(hook);
                plugin.getLogger().info("BeaconSpawnFeature: EzCountdown integration enabled.");
            } catch (Throwable t) {
                plugin.getLogger().warning("BeaconSpawnFeature: EzCountdown hook failed: " + t.getMessage());
            }
        }

        // Register listener
        final SpawnedBeaconListener listener =
                new SpawnedBeaconListener(beaconSpawnService, pluginAccessor, plugin.getLogger());
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        registerEzCountdownEndEvent(listener);
        plugin.getServer().getPluginManager().registerEvents(new BeaconGuiListener(), plugin);

        // Start schedule
        beaconScheduleService = new BeaconScheduleService(beaconSpawnService, plugin.getLogger());
        beaconScheduleService.start(pluginAccessor);
    }

    @SuppressWarnings("unchecked")
    private void registerEzCountdownEndEvent(SpawnedBeaconListener listener) {
        if (!Bukkit.getPluginManager().isPluginEnabled("EzCountdown")) {
            return;
        }

        try {
            final Class<?> rawEventClass = tryLoadClass(getClassLoader(), EZCOUNTDOWN_END_EVENT_CLASS_NAME);
            if (rawEventClass == null || !Event.class.isAssignableFrom(rawEventClass)) {
                plugin.getLogger().warning("BeaconSpawnFeature: EzCountdown CountdownEndEvent class not found.");
                return;
            }

            final Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
            final EventExecutor executor = (ignored, event) -> listener.onCountdownEnd(event);
            plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    EventPriority.MONITOR,
                    executor,
                    plugin,
                    true
            );
        }
        catch (Throwable throwable) {
            plugin.getLogger().warning("BeaconSpawnFeature: failed to register EzCountdown end listener: "
                    + throwable.getMessage());
        }
    }

    /**
     * Stops the beacon schedule and despawns all active beacons.
     */
    public void stopBeaconSpawnFeature() {
        if (beaconScheduleService != null) {
            beaconScheduleService.stop();
            beaconScheduleService = null;
        }
        if (beaconSpawnService != null && beaconPluginAccessor != null) {
            beaconSpawnService.despawnAll(beaconPluginAccessor);
        }
        beaconSpawnService = null;
    }

    /**
     * Reloads the beacon spawn feature by stopping and restarting with updated settings.
     */
    public void reloadBeaconSpawnFeature(PluginAccessor pluginAccessor) {
        startBeaconSpawnFeature(pluginAccessor);
    }

    public org.bukkit.plugin.PluginDescriptionFile getDescription() {
        return plugin.getDescription();
    }

    public void setPlaceholderExpansion(PlaceholderHook placeholderExpansion) {
        this.placeholderExpansion = placeholderExpansion;
        managerState.setPlaceholderExpansion(placeholderExpansion);
        compatibilityAdapter.setPlaceholderField(placeholderExpansion);
    }

    public void logStartupSummary() {
        final List<String> lines = new ArrayList<>();
        final String version = getDescription().getVersion();
        if (version == null || version.isBlank()) {
            lines.add("EzLifesteal plugin enabled.");
        }
        else {
            lines.add("EzLifesteal v" + version + " ready.");
        }
        lines
                .add("Storage backend: "
                        + (storageService != null ? storageService.getStorageSummary() : storageSummary) + ".");
        lines.add("Messages language: " + activeLanguage + ".");
        if (globalLifestealEnabled) {
            final StringBuilder lifesteal = new StringBuilder("Global lifesteal: enabled (");
            lifesteal.append(formatDouble(heartsPerKill)).append(" hearts/kill, ");
            lifesteal.append(formatDouble(heartsLostOnDeath)).append(" hearts lost on death");
            if (banWhenZeroHearts) {
                lifesteal.append(", zero-heart bans enabled");
            }
            lifesteal.append(").");
            lines.add(lifesteal.toString());
        }
        else {
            lines.add("Global lifesteal: disabled.");
        }
        lines.add(describeWorldRestrictions());
        final boolean overlayEnabled;
        if (overlayService != null) {
            overlayEnabled = overlayService.isEnabled();
        }
        else {
            final HeartOverlayManager overlay = managerState.getHeartOverlayManager();
            overlayEnabled = overlay != null && overlay.isEnabled();
        }
        lines.add("Heart overlay: " + (overlayEnabled ? "enabled" : "disabled") + ".");

        final boolean hologramPlaced;
        if (hologramService != null) {
            hologramPlaced = hologramService.hasHologram();
        }
        else {
            final TopHologramManager top = managerState.getTopHologramManager();
            hologramPlaced = top != null && top.hasHologram();
        }
        lines.add("Leaderboard hologram: " + (hologramPlaced ? "placed" : "not placed") + ".");
        lines.add("PlaceholderAPI expansion: " + (placeholderExpansion != null ? "registered." : "not registered."));
        lines.add(killStreakSettings != null ? killStreakSettings.summary() : "Kill streaks: disabled.");
        if (combatLogoutProtectionEnabled) {
            final double tagSeconds = combatLogoutTagDurationMillis / 1000.0;
            lines.add("Combat logout protection: enabled (" + formatDouble(tagSeconds) + "s tag window).");
        }
        else {
            lines.add("Combat logout protection: disabled.");
        }
        String smurfStatus = smurfDetectionEnabled ? "enabled" : "disabled";
        if (smurfDetectionEnabled && restrictSmurfAlertsToAdmins) {
            smurfStatus += " (alerts restricted to admins)";
        }
        lines.add("Smurf detection: " + smurfStatus + ".");
        final String overrideSummary = buildWorldOverrideSummary();
        if (overrideSummary != null && !overrideSummary.isBlank()) {
            lines.add(overrideSummary);
        }
        for (String line : lines) {
            getLogger().info(line);
        }
    }

    public boolean ensureSeasonsClasses() {
        final var integrationState = registry.getSeasonsIntegrationState();
        if (integrationState.getApiClass() != null
                && integrationState.getIntegrationClass() != null
                && integrationState.getProfileClass() != null) {
            return true;
        }
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(SEASONS_PLUGIN_NAME);
        if (plugin == null) {
            return false;
        }
        final ClassLoader loader = plugin.getClass().getClassLoader();
        try {
            integrationState.setApiClass(tryLoadClass(loader, SEASONS_API_CLASS_NAME, LEGACY_SEASONS_API_CLASS_NAME));
            integrationState
                    .setIntegrationClass(tryLoadClass(loader,
                            SEASONS_INTEGRATION_CLASS_NAME, LEGACY_SEASONS_INTEGRATION_CLASS_NAME));
            if (integrationState.getApiClass() == null || integrationState.getIntegrationClass() == null) {
                integrationState.clearLoadedClasses();
                return false;
            }
            integrationState.setProfileClass(resolveSeasonsProfileClass(integrationState.getIntegrationClass()));
            return integrationState.getProfileClass() != null;
        }
        catch (ReflectiveOperationException | LinkageError exception) {
            integrationState.clearLoadedClasses();
            return false;
        }
    }


    private Class<?> resolveSeasonsProfileClass(Class<?> integrationClass) throws ClassNotFoundException {
        for (Class<?> nested : integrationClass.getDeclaredClasses()) {
            if ("Profile".equals(nested.getSimpleName())) {
                return nested;
            }
        }
        final ClassLoader loader = integrationClass.getClassLoader();
        final String name = integrationClass.getName() + "$Profile";
        return Class.forName(name, false, loader);
    }

    private Class<?> tryLoadClass(ClassLoader loader, String... candidates) throws ClassNotFoundException {
        ClassNotFoundException lastFailure = null;
        for (String candidate : candidates) {
            try {
                return Class.forName(candidate, false, loader);
            }
            catch (ClassNotFoundException exception) {
                lastFailure = exception;
            }
            catch (LinkageError error) {
                lastFailure = new ClassNotFoundException(
                        "Candidate class is present but not loadable on this runtime: " + candidate,
                        error);
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new ClassNotFoundException("No class candidates were provided.");
    }

    public void clearRegisteredHeartRecipes() {
        requireRecipeService().clearRegisteredHeartRecipes();
    }

    // Compatibility helpers used by runtime services
    public void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
    }

    public void clearRegisteredHeartRecipesInternal() {
        if (registeredHeartRecipes == null || registeredHeartRecipes.isEmpty()) {
            return;
        }
        for (NamespacedKey key : new java.util.ArrayList<>(registeredHeartRecipes)) {
            try {
                SchedulerAdapter.run(plugin, () -> Bukkit.getServer().removeRecipe(key));
            }
            catch (Throwable ignored) {
            }
        }
        registeredHeartRecipes.clear();
    }

    // Helper for migrated services to add registered recipe keys
    public void addRegisteredHeartRecipe(NamespacedKey key) {
        if (key == null) {
            return;
        }
        registeredHeartRecipes.add(key);
    }

    // Helper to create namespaced keys using the plugin instance
    public NamespacedKey createNamespacedKey(String key) {
        return new NamespacedKey(plugin, key);
    }

    // Internal wrappers for RecipeService compatibility
    public void registerHeartRecipesInternal() {
        registerHeartRecipes();
    }

    public boolean isHeartRecipesEnabledInternal() {
        return isHeartRecipesEnabled();
    }

    public void registerHeartRecipes() {
        requireRecipeService().registerHeartRecipes();
    }

    public boolean isHeartRecipesEnabled() {
        if (heartsConfig == null) {
            return false;
        }

        boolean enabled = true;
        if (featureConfig != null && featureConfig.contains("heart-recipes.enabled")) {
            enabled = featureConfig.getBoolean("heart-recipes.enabled", true);
        }
        if (heartsConfig.contains("recipes.enabled")) {
            enabled = heartsConfig.getBoolean("recipes.enabled", enabled);
        }
        if (!enabled) {
            return false;
        }

        final ConfigurationSection recipesSection = heartsConfig.getConfigurationSection("recipes");
        if (recipesSection == null) {
            return false;
        }
        return !recipesSection.getKeys(false).isEmpty();
    }

    public ExecutorService getStorageExecutor() {
        return storageService != null ? storageService.getStorageExecutor() : storageExecutor;
    }

    public Registry getRegistry() {
        return registry;
    }

    public com.skyblockexp.ezlifesteal.config.AdminConfigAdapter getAdminConfigAdapter() {
        return adminConfigAdapter;
    }

    public com.skyblockexp.ezlifesteal.config.SmurfConfigAdapter getSmurfConfigAdapter() {
        return smurfConfigAdapter;
    }

    public void setAdminDetector(AdminDetector detector) {
        this.adminDetector = detector;
    }

    public void setSmurfDetector(SmurfDetector detector) {
        this.smurfDetector = detector;
    }

    public String getZeroHeartBanMessage() {
        return zeroHeartBanMessage;
    }

    public String getZeroHeartKickMessage() {
        return zeroHeartKickMessage;
    }

    public void setPlayerListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
        registry.setPlayerListener(playerListener);
    }

    public String getWorldOverrideSummary() {
        return buildWorldOverrideSummary();
    }

    public LifestealManager getLifestealManager() {
        final LifestealManager manager = managerState.getLifestealManager();
        return manager != null ? manager : lifestealManager;
    }

    public LifestealConfigAdapter getLifestealConfigAdapter() {
        final LifestealConfigAdapter adapter = configState.getLifestealConfigAdapter();
        return adapter != null ? adapter : lifestealConfigAdapter;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public TopHologramManager getTopHologramManager() {
        if (hologramService != null) {
            final TopHologramManager mgr = hologramService.getManager();
            if (mgr != null) {
                return mgr;
            }
        }
        TopHologramManager manager = managerState.getTopHologramManager();
        if (manager == null) {
            manager = new TopHologramManager(plugin);
            managerState.setTopHologramManager(manager);
        }
        return manager;
    }

    public SmurfDetector getSmurfDetector() {
        return smurfDetector;
    }

    public boolean addSmurfExemption(UUID uniqueId) {
        if (smurfDetector == null) {
            return false;
        }
        final boolean added = smurfDetector.addExemptPlayer(uniqueId);
        if (added) {
            persistSmurfExemptions();
        }
        return added;
    }

    public boolean removeSmurfExemption(UUID uniqueId) {
        if (smurfDetector == null) {
            return false;
        }
        final boolean removed = smurfDetector.removeExemptPlayer(uniqueId);
        if (removed) {
            persistSmurfExemptions();
        }
        return removed;
    }

    public void persistSmurfExemptions() {
        if (smurfDetector == null) {
            return;
        }
        final List<String> serialized = smurfDetector.getExemptPlayers().stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        if (smurfConfig == null || smurfConfigFile == null) {
            getLogger().warning("Smurf configuration is not loaded; unable to persist exemptions.");
            return;
        }
        smurfConfig.set("exempted-players", serialized);
        try {
            smurfConfig.save(smurfConfigFile);
        }
        catch (IOException exception) {
            getLogger().severe("Failed to save smurf.yml: " + exception.getMessage());
        }
    }

    public AdminDetector getAdminDetector() {
        return adminDetector;
    }

    public boolean simulatePlayerDeath(Player victim, Player killer) {
        if (playerListener == null || victim == null) {
            return false;
        }
        playerListener.handlePlayerDeath(victim, killer);
        return true;
    }

    public boolean simulatePlayerKill(Player killer) {
        if (killer == null) {
            return false;
        }
        if (!isGlobalLifestealEnabled()) {
            return false;
        }
        if (!isLifestealEnabledInWorld(killer.getWorld().getName())) {
            return false;
        }
        final LifestealManager manager = getLifestealManager();
        final LifestealProfile killerProfile = manager.getOrCreateProfile(killer.getUniqueId());
        final AdminDetector detector = getAdminDetector();
        final boolean killerIsAdmin = detector != null && detector.isAdmin(killer);
        final boolean applyHeartGain = !(killerIsAdmin && isAdminBypassHeartGain());
        if (applyHeartGain) {
            if (dropHeartOnKill && heartRegistry != null) {
                com.skyblockexp.ezlifesteal.heart.Heart heart = heartRegistry.getById(dropHeartId);
                if (heart == null) {
                    // try to match by tier using hearts-per-kill rounded
                    final int tierGuess = (int) Math.max(1, Math.round(getHeartsPerKill(killer.getWorld().getName())));
                    heart = heartRegistry.getByTier(tierGuess);
                }
                if (heart != null) {
                    final com.skyblockexp.ezlifesteal.heart.Heart selected = heart;
                    final int giveAmount = Math.max(1, dropHeartAmount);
                    AdapterSupport.runForPlayer(plugin, killer, () -> {
                        final ItemStack stack = selected.createItemStack();
                        for (int i = 0; i < giveAmount; i++) {
                            final ItemStack toGive = stack.clone();
                            final Map<Integer, ItemStack> leftover = killer.getInventory().addItem(toGive);
                            if (!leftover.isEmpty()) {
                                AdapterSupport.dropItemLeftoversAtPlayer(plugin, killer, leftover);
                            }
                        }
                    });
                }
                else {
                    // fallback to applying hearts directly if no heart definition found
                    final double heartsPerKillAmount = getHeartsPerKill(killer.getWorld().getName());
                    killerProfile.addHearts(heartsPerKillAmount, manager.getMaxHearts());
                    manager.saveProfileAsync(killerProfile).whenComplete((unused, throwable) -> {
                        if (throwable != null) {
                            getLogger()
                                    .severe("Failed to save killer profile for " + killer.getName()
                                            + ": " + throwable.getMessage());
                        }
                    });
                }
            }
            else {
                final double heartsPerKillAmount = getHeartsPerKill(killer.getWorld().getName());
                killerProfile.addHearts(heartsPerKillAmount, manager.getMaxHearts());
                manager.saveProfileAsync(killerProfile).whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        getLogger()
                                .severe("Failed to save killer profile for " + killer.getName()
                                        + ": " + throwable.getMessage());
                    }
                });
            }
        }
        AdapterSupport.runForPlayer(plugin, killer, () -> {
            manager.applyHearts(killer, killerProfile);
            sendHeartStatus(killer, killerProfile.getHearts());
        });
        requestTopHologramUpdate();
        return true;
    }

    public double getHeartsPerKill() {
        return gameplayState.getHeartRulesState().getHeartsPerKill();
    }

    public double getHeartsPerKill(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return gameplayState.getHeartRulesState().getHeartsPerKill();
        }
        return gameplayState.getWorldRulesState().getHeartsPerKillOverrides()
                .getOrDefault(normalizeWorldName(worldName),
                gameplayState.getHeartRulesState().getHeartsPerKill());
    }

    public double getHeartsLostOnDeath() {
        return gameplayState.getHeartRulesState().getHeartsLostOnDeath();
    }

    public double getHeartsLostOnDeath(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return gameplayState.getHeartRulesState().getHeartsLostOnDeath();
        }
        return gameplayState.getWorldRulesState().getHeartsLostOnDeathOverrides()
                .getOrDefault(normalizeWorldName(worldName),
                gameplayState.getHeartRulesState().getHeartsLostOnDeath());
    }

    public boolean isDropHeartOnKill() {
        return gameplayState.getDropRulesState().isDropHeartOnKill();
    }

    public boolean isDropHeartOnDeath() {
        return gameplayState.getDropRulesState().isDropHeartOnDeath();
    }

    public boolean isDropHeartOnlyWhenKilledByPlayer() {
        return gameplayState.getDropRulesState().isDropHeartOnlyWhenKilledByPlayer();
    }

    public String getDropHeartId() {
        return gameplayState.getDropRulesState().getDropHeartId();
    }

    public int getDropHeartAmount() {
        return gameplayState.getDropRulesState().getDropHeartAmount();
    }

    public static SanitizedHeartBounds sanitizeHeartBounds(double minHearts, double defaultHearts, double maxHearts) {
        final double sanitizedMin = Math.max(0.0, minHearts);
        final double sanitizedMax = Math.max(sanitizedMin, maxHearts);
        final double sanitizedDefault = Math.max(sanitizedMin, Math.min(defaultHearts, sanitizedMax));
        final boolean adjusted = Double.compare(sanitizedMin, minHearts) != 0
                || Double.compare(sanitizedDefault, defaultHearts) != 0
                || Double.compare(sanitizedMax, maxHearts) != 0;
        return new SanitizedHeartBounds(sanitizedMin, sanitizedDefault, sanitizedMax, adjusted);
    }

    public static record SanitizedHeartBounds(double minHearts, double defaultHearts, double maxHearts,
            boolean adjusted) {
        public boolean adjusted() {
            return adjusted;
        }
    }

    public boolean isBanWhenZeroHearts() {
        return gameplayState.getHeartRulesState().isBanWhenZeroHearts();
    }

    public boolean isBanWhenZeroHearts(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return gameplayState.getHeartRulesState().isBanWhenZeroHearts();
        }
        return gameplayState.getWorldRulesState().getBanWhenZeroOverrides().getOrDefault(normalizeWorldName(worldName),
                gameplayState.getHeartRulesState().isBanWhenZeroHearts());
    }

    public boolean isGlobalLifestealEnabled() {
        return gameplayState.getHeartRulesState().isGlobalLifestealEnabled();
    }

    public boolean isTeamKillBypassEnabled() {
        return gameplayState.getHeartRulesState().isTeamKillBypassWithTeamsApi();
    }

    public List<String> getTeamKillBypassExemptWorlds() {
        return gameplayState.getHeartRulesState().getTeamKillBypassExemptWorlds();
    }

    public int getTeamKillBypassMinTeamSize() {
        return gameplayState.getHeartRulesState().getTeamKillBypassMinTeamSize();
    }

    public boolean isTeamBankEnabled() {
        return gameplayState.getHeartRulesState().isTeamBankEnabled();
    }

    public double getTeamBankMaxHearts() {
        return gameplayState.getHeartRulesState().getTeamBankMaxHearts();
    }

    public double getTeamBankMaxHeartsForTeam(UUID teamId) {
        if (teamId == null) {
            return getTeamBankMaxHearts();
        }
        final Map<String, Double> perTeamOverrides = gameplayState.getHeartRulesState().getTeamBankPerTeamMaxHearts();
        final Double override = perTeamOverrides.get(teamId.toString());
        if (override != null && override > 0) {
            return override;
        }
        // Also try team name via resolver if available
        if (teamsApiTeamResolver != null) {
            final Optional<String> teamName = teamsApiTeamResolver.resolveTeamName(teamId);
            if (teamName.isPresent()) {
                final Double nameOverride = perTeamOverrides.get(teamName.get());
                if (nameOverride != null && nameOverride > 0) {
                    return nameOverride;
                }
            }
        }
        return getTeamBankMaxHearts();
    }

    public com.skyblockexp.ezlifesteal.service.TeamBankAdminService getTeamBankAdminService() {
        return teamBankAdminService;
    }

    public boolean shouldBypassForTeamKill(Player killer, Player victim) {
        return teamKillBypassService != null && teamKillBypassService.shouldBypass(killer, victim);
    }

    public boolean isAdminBypassHeartLoss() {
        return gameplayState.getHeartRulesState().isAdminBypassHeartLoss();
    }

    public boolean isDontRemoveHeartsFromMobs() {
        return gameplayState.getMobRulesState().isDontRemoveHeartsFromMobs();
    }

    public double getMobRemoveHeartsGreaterThan() {
        return gameplayState.getMobRulesState().getMobRemoveHeartsGreaterThan();
    }

    public boolean isCombatLogoutProtectionEnabled() {
        return combatLogoutProtectionEnabled;
    }

    public long getCombatLogoutTagDurationMillis() {
        return combatLogoutTagDurationMillis;
    }

    public boolean isAdminBypassHeartGain() {
        return gameplayState.getHeartRulesState().isAdminBypassHeartGain();
    }

    public boolean isRestrictSmurfAlertsToAdmins() {
        return restrictSmurfAlertsToAdmins;
    }

    public Set<String> getEnabledWorlds() {
        return gameplayState.getWorldRulesState().getEnabledWorlds();
    }

    public Set<String> getDisabledWorlds() {
        return gameplayState.getWorldRulesState().getDisabledWorlds();
    }

    public MobReward getMobReward(EntityType entityType) {
        if (entityType == null) {
            return null;
        }
        return gameplayState.getMobRulesState().getMobRewards().get(entityType);
    }

    public Optional<Economy> getEconomy() {
        return Optional.ofNullable(registry.getVaultIntegrationState().getEconomy());
    }

    public KillStreakManager getKillStreakManager() {
        final KillStreakManager manager = managerState.getKillStreakManager();
        return manager != null ? manager : killStreakManager;
    }

    public KillStreakSettings getKillStreakSettings() {
        return killStreakSettings;
    }

    public boolean isLifestealEnabledInWorld(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return gameplayState.getWorldRulesState().getEnabledWorlds().isEmpty();
        }
        final String normalized = worldName.toLowerCase(Locale.ROOT);
        if (!gameplayState.getWorldRulesState().getEnabledWorlds().isEmpty()
                && !gameplayState.getWorldRulesState().getEnabledWorlds().contains(normalized)) {
            return false;
        }
        return !gameplayState.getWorldRulesState().getDisabledWorlds().contains(normalized);
    }

    public void sendHeartStatus(Player player, double hearts) {
        if (overlayService != null) {
            overlayService.sendHeartStatus(player, hearts);
            return;
        }
        final HeartOverlayManager overlay = managerState.getHeartOverlayManager();
        if (overlay == null) {
            return;
        }
        if (!isGlobalLifestealEnabled()) {
            overlay.clear(player.getUniqueId());
            return;
        }
        overlay.sendHeartStatus(player, hearts);
    }

    public void requestTopHologramUpdate() {
        if (hologramService != null) {
            hologramService.requestUpdate();
            return;
        }
        final TopHologramManager mgr = getTopHologramManager();
        if (mgr != null) {
            mgr.requestUpdate();
        }
    }

    public void clearHeartStatus(UUID uniqueId) {
        if (overlayService != null) {
            overlayService.clear(uniqueId);
            return;
        }
        final HeartOverlayManager overlay = managerState.getHeartOverlayManager();
        if (overlay != null) {
            overlay.clear(uniqueId);
        }
    }

    private boolean getAdminBoolean(String key, boolean defaultValue) {
        return resolveLegacyConfig().getBoolean(adminConfig, "admin-detection", key, defaultValue);
    }

    private String getAdminString(String key, String defaultValue) {
        return resolveLegacyConfig().getString(adminConfig, "admin-detection", key, defaultValue);
    }

    private List<String> getAdminStringList(String key) {
        return resolveLegacyConfig().getStringList(adminConfig, "admin-detection", key);
    }

    private boolean getSmurfBoolean(String key, boolean defaultValue) {
        return resolveLegacyConfig().getBoolean(smurfConfig, "smurf-detection", key, defaultValue);
    }

    private int getSmurfInt(String key, int defaultValue) {
        return resolveLegacyConfig().getInt(smurfConfig, "smurf-detection", key, defaultValue);
    }

    private long getSmurfLong(String key, long defaultValue) {
        return resolveLegacyConfig().getLong(smurfConfig, "smurf-detection", key, defaultValue);
    }

    private String getSmurfString(String key, String defaultValue) {
        return resolveLegacyConfig().getString(smurfConfig, "smurf-detection", key, defaultValue);
    }

    private List<String> getSmurfStringList(String key) {
        return resolveLegacyConfig().getStringList(smurfConfig, "smurf-detection", key);
    }

    private LegacyConfigResolver resolveLegacyConfig() {
        return new LegacyConfigResolver(getConfig());
    }

    private Set<String> parseWorldList(List<String> worldNames) {
        if (worldNames == null || worldNames.isEmpty()) {
            return new HashSet<>();
        }
        final Set<String> parsed = new HashSet<>();
        for (String name : worldNames) {
            if (name == null) {
                continue;
            }
            final String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return parsed;
    }

    private double getLifestealDouble(String path, double defaultValue) {
        return lifestealConfigAdapter.getDouble(path, defaultValue);
    }

    public boolean getLifestealBoolean(String path, boolean defaultValue) {
        return lifestealConfigAdapter.getBoolean(path, defaultValue);
    }

    private String getLifestealString(String path, String defaultValue) {
        return lifestealConfigAdapter.getString(path, defaultValue);
    }

    private List<String> getLifestealStringList(String path) {
        return lifestealConfigAdapter.getStringList(path);
    }

    private ConfigurationSection getLifestealSection(String path) {
        return lifestealConfigAdapter.getSection(path);
    }

    public void setLifestealValue(String path, Object value) {
        if (lifestealCoreConfig != null) {
            lifestealCoreConfig.set(path, value);
        }
        else {
            getConfig().set("lifesteal." + path, value);
        }
    }

    public void saveLifestealSettings() {
        if (lifestealCoreConfig != null && lifestealCoreConfigFile != null) {
            configLoader.save(lifestealCoreConfig, "lifesteal-core.yml");
        }
        else {
            plugin.saveConfig();
        }
    }

    private void setStorageValue(String path, Object value) {
        if (storageConfig != null) {
            storageConfig.set(path, value);
        }
        else {
            getConfig().set("storage." + path, value);
        }
    }

    private void saveStorageConfiguration() {
        if (storageConfig != null && storageConfigFile != null) {
            configLoader.save(storageConfig, "storage.yml");
        }
        else {
            plugin.saveConfig();
        }
    }

    private ConfigurationSection getStorageSection(String path) {
        ConfigurationSection section = null;
        if (storageConfig != null) {
            section = path == null ? storageConfig : storageConfig.getConfigurationSection(path);
        }
        if (section == null) {
            final ConfigurationSection legacy = getConfig().getConfigurationSection("storage");
            if (legacy != null) {
                section = path == null ? legacy : legacy.getConfigurationSection(path);
            }
        }
        return section;
    }

    public ConfigurationSection getActionBarSection() {
        if (featureConfig != null) {
            return featureConfig.getConfigurationSection("action-bar");
        }
        final ConfigurationSection legacy = getConfig().getConfigurationSection("lifesteal");
        return legacy == null ? null : legacy.getConfigurationSection("action-bar");
    }

    public ConfigurationSection getHologramSection(boolean create) {
        if (featureConfig != null) {
            ConfigurationSection section = featureConfig.getConfigurationSection("hologram");
            if (section == null && create) {
                section = featureConfig.createSection("hologram");
            }
            return section;
        }
        ConfigurationSection section = getConfig().getConfigurationSection("hologram");
        if (section == null && create) {
            section = getConfig().createSection("hologram");
        }
        return section;
    }

    public void saveHologramSettings() {
        saveFeaturesConfiguration();
    }

    private void saveFeaturesConfiguration() {
        if (featureConfig != null && featureConfigFile != null) {
            configLoader.save(featureConfig, "features.yml");
        }
        else {
            plugin.saveConfig();
        }
    }

    public void parseWorldOverrides() {
        final GameplayParsingService.WorldOverrides parsed = requireGameplayParsingService().parseWorldOverrides();
        heartsPerKillOverrides = parsed.heartsPerKillOverrides();
        heartsLostOnDeathOverrides = parsed.heartsLostOnDeathOverrides();
        banWhenZeroOverrides = parsed.banWhenZeroOverrides();
        gameplayState.getWorldRulesState().setHeartsPerKillOverrides(heartsPerKillOverrides);
        gameplayState.getWorldRulesState().setHeartsLostOnDeathOverrides(heartsLostOnDeathOverrides);
        gameplayState.getWorldRulesState().setBanWhenZeroOverrides(banWhenZeroOverrides);
    }

    public void parseMobRewards() {
        mobRewards = requireGameplayParsingService().parseMobRewards();
        gameplayState.getMobRulesState().setMobRewards(mobRewards);
    }

    public KillStreakSettings parseKillStreakSettings() {
        return requireKillStreakParsingService().parseKillStreakSettings();
    }

    private KillStreakReward parseKillStreakReward(String key, ConfigurationSection rewardSection) {
        return requireKillStreakParsingService().parseKillStreakReward(key, rewardSection);
    }

    private List<ItemStack> parseRewardItems(ConfigurationSection rewardSection, String key) {
        return requireKillStreakParsingService().parseRewardItems(rewardSection, key);
    }

    private ItemStack parseRewardItem(Object raw, String key) {
        return requireKillStreakParsingService().parseRewardItem(raw, key);
    }

    private ItemStack parseRewardItemMap(Map<?, ?> map, String key) {
        return requireKillStreakParsingService().parseRewardItemMap(map, key);
    }

    private ItemStack parseRewardItemString(String value, String key) {
        return requireKillStreakParsingService().parseRewardItemString(value, key);
    }

    private String describeWorldRestrictions() {
        if (!enabledWorlds.isEmpty()) {
            return "World restrictions: enabled only in " + formatWorldList(enabledWorlds) + ".";
        }
        if (!disabledWorlds.isEmpty()) {
            return "World restrictions: disabled in " + formatWorldList(disabledWorlds) + ".";
        }
        return "World restrictions: none (all worlds inherit the global setting).";
    }

    private String formatWorldList(Set<String> worlds) {
        if (worlds == null || worlds.isEmpty()) {
            return "";
        }
        return String.join(", ", new TreeSet<>(worlds));
    }

    private String buildWorldOverrideSummary() {
        final Set<String> worlds = new TreeSet<>();
        addNonBlankWorldKeys(worlds, heartsPerKillOverrides);
        addNonBlankWorldKeys(worlds, heartsLostOnDeathOverrides);
        addNonBlankWorldKeys(worlds, banWhenZeroOverrides);
        if (worlds.isEmpty()) {
            return "No world-specific lifesteal overrides are configured.";
        }
        final List<String> details = new ArrayList<>();
        for (String world : worlds) {
            final List<String> parts = new ArrayList<>();
            if (heartsPerKillOverrides.containsKey(world)) {
                parts.add("kill=" + formatDouble(heartsPerKillOverrides.get(world)));
            }
            if (heartsLostOnDeathOverrides.containsKey(world)) {
                parts.add("death=" + formatDouble(heartsLostOnDeathOverrides.get(world)));
            }
            if (banWhenZeroOverrides.containsKey(world)) {
                parts.add("ban=" + (banWhenZeroOverrides.get(world) ? "on" : "off"));
            }
            if (!parts.isEmpty()) {
                details.add(world + ": " + String.join(", ", parts));
            }
        }
        if (details.isEmpty()) {
            return "No world-specific lifesteal overrides are configured.";
        }
        return "World overrides loaded (" + details.size() + "): " + String.join("; ", details);
    }

    private String formatDouble(double value) {
        return value % 1 == 0 ? Integer.toString((int) value) : String.format(Locale.US, "%.2f", value);
    }

    private void addNonBlankWorldKeys(Set<String> destination, Map<String, ?> source) {
        if (destination == null || source == null || source.isEmpty()) {
            return;
        }
        for (String key : source.keySet()) {
            if (key != null && !key.isBlank()) {
                destination.add(key);
            }
        }
    }

    public String normalizeWorldName(String worldName) {
        return worldName == null ? "" : worldName.trim().toLowerCase(Locale.ROOT);
    }

    private ManagerService requireManagerService() {
        return Objects.requireNonNull(managerService,
                "initializeCoreState() must be called before manager lifecycle methods.");
    }

    private OverlayService requireOverlayService() {
        return Objects.requireNonNull(overlayService,
                "initializeCoreState() must be called before overlay lifecycle methods.");
    }

    private HologramService requireHologramService() {
        return Objects.requireNonNull(hologramService,
                "initializeCoreState() must be called before hologram lifecycle methods.");
    }

    private RecipeService requireRecipeService() {
        return Objects.requireNonNull(recipeService,
                "initializeCoreState() must be called before recipe lifecycle methods.");
    }

    private KillStreakParsingService requireKillStreakParsingService() {
        return Objects.requireNonNull(killStreakParsingService,
                "initializeCoreState() must be called before kill streak parsing.");
    }

    private GameplayParsingService requireGameplayParsingService() {
        return Objects.requireNonNull(gameplayParsingService,
                "initializeCoreState() must be called before gameplay parsing.");
    }

    void setLegacyLifestealManager(LifestealManager lifestealManager) {
        this.lifestealManager = lifestealManager;
    }

    void setLegacyHeartOverlayManager(HeartOverlayManager heartOverlayManager) {
        this.heartOverlayManager = heartOverlayManager;
    }

    void setLegacyTopHologramManager(TopHologramManager topHologramManager) {
        this.topHologramManager = topHologramManager;
    }

    void setLegacyKillStreakManager(KillStreakManager killStreakManager) {
        this.killStreakManager = killStreakManager;
    }

    void setLegacyPlaceholderExpansion(PlaceholderHook placeholderExpansion) {
        this.placeholderExpansion = placeholderExpansion;
    }
}
