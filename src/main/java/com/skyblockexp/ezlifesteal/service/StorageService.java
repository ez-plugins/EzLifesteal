package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
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
import com.skyblockexp.ezlifesteal.util.PluginLifecycleSupport;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class StorageService {

    private final EzLifestealPlugin plugin;

    private final com.skyblockexp.ezlifesteal.runtime.Registry registry;

    private final ConfigLoader configLoader;


    private Storage storage;

    private StorageProvider storageProvider;

    private ProfileRepository profileRepository;

    private BanRepository banRepository;

    private TeamBankRepository teamBankRepository;

    private ExecutorService storageExecutor;

    private String storageSummary = "Unknown storage";

    private YamlConfiguration storageConfig;

    private File storageConfigFile;


    public StorageService(EzLifestealPlugin plugin, com.skyblockexp.ezlifesteal.runtime.Registry registry,
            ConfigLoader configLoader) {
        this.plugin = plugin;
        this.registry = registry;
        this.configLoader = configLoader;
    }

    public void initializeStorageExecutor() {
        if (storageExecutor != null) {
            storageExecutor.shutdownNow();
        }
        storageExecutor = Executors.newSingleThreadExecutor(r -> {
            final Thread thread = new Thread(r, "EzLifesteal-Storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void shutdownStorageExecutor() {
        PluginLifecycleSupport.shutdownExecutor(storageExecutor, 5, TimeUnit.SECONDS);
    }

    public void setupStorage() {
        storageConfigFile = configLoader.resolveFile("storage.yml");
        storageConfig = configLoader.load("storage.yml");

        final String configuredType = resolveConfiguredStorageType();
        final String normalizedType = normalizeStorageType(configuredType);

        closeStorage();
        storageSummary = "Unknown storage";
        storageProvider = createStorageProvider(normalizedType, configuredType);
        initializeStorageBridge();
    }

    private String resolveConfiguredStorageType() {
        String configuredType = Optional.ofNullable(storageConfig)
                .filter(config -> config.contains("type"))
                .map(config -> config.getString("type"))
                .orElse(null);

        if ((configuredType == null || configuredType.isBlank()) && plugin.getConfig().contains("storage.type")) {
            configuredType = plugin.getConfig().getString("storage.type");
        }
        return configuredType;
    }

    private String normalizeStorageType(String configuredType) {
        if (configuredType != null && !configuredType.isBlank()) {
            return configuredType.toUpperCase(Locale.ROOT);
        }

        plugin.getLogger().warning("No storage type configured; defaulting to YAML storage.");
        if (storageConfig != null) {
            storageConfig.set("type", "YAML");
            try {
                storageConfig.save(storageConfigFile);
            }
            catch (Exception ignored) {
                // best-effort persistence only
            }
        }
        return "YAML";
    }

    private StorageProvider createStorageProvider(String normalizedType, String configuredType) {
        return switch (normalizedType) {
            case "MYSQL" -> createMySqlProviderOrFallback();
            case "YML", "YAML" -> createYamlProvider("players.yml", null);
            default -> {
                final String reportedType = configuredType == null ? normalizedType : configuredType;
                plugin.getLogger().warning("Unknown storage type '" + reportedType + "', defaulting to YAML storage.");
                yield createYamlProvider("players.yml", "fallback for unknown type '" + reportedType + "'");
            }
        };
    }

    private StorageProvider createMySqlProviderOrFallback() {
        final ConfigurationSection mysql = getStorageSection("mysql");
        if (mysql == null) {
            plugin.getLogger().warning("No MySQL configuration section found; defaulting to YAML storage.");
            return createYamlProvider("players.yml", null);
        }

        final String host = mysql.getString("host", "localhost");
        final int port = mysql.getInt("port", 3306);
        final String database = mysql.getString("database", "lifesteal");
        final String username = mysql.getString("username", "root");
        final String password = mysql.getString("password", "password");
        final boolean useSsl = mysql.getBoolean("use-ssl", false);
        final String table = mysql.getString("table", "lifesteal_players");

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
        return new MySqlStorageProvider(host, port, database, username, password, useSsl, table);
    }

    private StorageProvider createYamlProvider(String fileName, String reason) {
        final File dataFolder = new File(plugin.getDataFolder(), "data");
        final File dataFile = new File(dataFolder, fileName);
        storageSummary = reason == null
                ? "YAML storage (" + dataFile.getPath() + ")"
                : "YAML storage (" + reason + ")";
        return new YamlStorageProvider(dataFolder, fileName);
    }

    private void initializeStorageBridge() {
        try {
            storage = new RepositoryBackedStorageBridge(storageProvider);
            profileRepository = storageProvider.profiles();
            banRepository = storageProvider.bans();
            teamBankRepository = storageProvider.teamBanks();
            storage.init();
            reconcileRuntimeBans();
        }
        catch (StorageException exception) {
            plugin.getLogger().severe("Failed to initialise storage: " + exception.getMessage());
            storage = null;
            storageProvider = null;
            profileRepository = null;
            banRepository = null;
            teamBankRepository = null;
        }
    }

    public void reconcileRuntimeBans() {
        importRuntimeBansIntoStorage();
        if (getBanRepository() == null) {
            return;
        }
        final List<BanRecord> activeBans;
        try {
            activeBans = getBanRepository().loadActiveBans();
        }
        catch (StorageException exception) {
            plugin.getLogger().warning("Failed to load active bans from storage: " + exception.getMessage());
            return;
        }
        if (activeBans.isEmpty()) {
            return;
        }
        final BanList<PlayerProfile> profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
        for (BanRecord record : activeBans) {
            final String playerName = record.getPlayerName();
            final UUID playerUuid = record.getUniqueId();
            if (playerName == null || playerName.isBlank() || playerUuid == null) {
                continue;
            }
            final Date expiresAt = record.getExpiresAt() == null ? null : Date.from(record.getExpiresAt());
            final PlayerProfile profile = Bukkit.createProfile(playerUuid, playerName);
            if (!profileBanList.isBanned(profile)) {
                profileBanList.addBan(profile, record.getReason(), expiresAt, record.getSource());
            }
        }
    }

    public void importRuntimeBansIntoStorage() {
        final BanRepository repository = getBanRepository();
        if (repository == null) {
            return;
        }
        final BanList<PlayerProfile> profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
        for (BanEntry<PlayerProfile> entry : profileBanList.getBanEntries()) {
            persistBanEntry(repository, entry);
        }
    }

    private void persistBanEntry(BanRepository repository, BanEntry<PlayerProfile> entry) {
        final PlayerProfile profile = entry.getBanTarget();
        final UUID uniqueId = profile.getId();
        if (uniqueId == null) {
            return;
        }
        final String playerName = profile.getName();
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        try {
            final Date created = entry.getCreated();
            final Date expiresAt = entry.getExpiration();
            final BanRecord imported = new BanRecord(
                    uniqueId,
                    playerName,
                    entry.getReason(),
                    entry.getSource(),
                    created == null ? java.time.Instant.now() : created.toInstant(),
                    expiresAt == null ? null : expiresAt.toInstant(),
                    true
            );
            repository.saveBan(imported);
        }
        catch (StorageException exception) {
            plugin.getLogger()
                    .warning("Failed to import ban for " + playerName + " into storage: " + exception.getMessage());
        }
    }

    public void closeStorage() {
        PluginLifecycleSupport.closeStorage(storage, plugin.getLogger());
        storage = null;
        storageProvider = null;
        profileRepository = null;
        banRepository = null;
        teamBankRepository = null;
    }

    public Storage getStorage() {
        return storage;
    }

    public ProfileRepository getProfileRepository() {
        return profileRepository == null ? storage : profileRepository;
    }

    public BanRepository getBanRepository() {
        return banRepository == null ? storage : banRepository;
    }

    public ExecutorService getStorageExecutor() {
        return storageExecutor;
    }

    public TeamBankRepository getTeamBankRepository() {
        return teamBankRepository;
    }

    public String getStorageSummary() {
        return storageSummary;
    }

    private ConfigurationSection getStorageSection(String path) {
        ConfigurationSection section = null;
        if (storageConfig != null) {
            section = storageConfig.getConfigurationSection(path);
        }
        if (section == null) {
            final ConfigurationSection legacy = plugin.getConfig().getConfigurationSection("storage");
            if (legacy != null) {
                section = legacy.getConfigurationSection(path);
            }
        }
        return section;
    }
}
