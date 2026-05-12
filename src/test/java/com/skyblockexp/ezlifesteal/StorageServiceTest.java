package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.service.StorageService;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageServiceTest {

    @Test
    void initializeAndShutdownExecutor() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        Registry registry = mock(Registry.class);
        ConfigLoader loader = mock(ConfigLoader.class);

        StorageService service = new StorageService(plugin, registry, loader);

        service.initializeStorageExecutor();
        assertNotNull(service.getStorageExecutor());
        var future = service.getStorageExecutor().submit(() -> Thread.currentThread().getName());
        String threadName = future.get();
        assertEquals("EzLifesteal-Storage", threadName);

        service.shutdownStorageExecutor();
        assertTrue(service.getStorageExecutor() == null
                || service.getStorageExecutor().isShutdown()
                || service.getStorageExecutor().isTerminated());
    }

    @Test
    void setupStorageDefaultsToYamlAndInitializesRepositories() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = mock(Registry.class);

        File dataDir = Files.createTempDirectory("ezlifesteal-test").toFile();
        dataDir.deleteOnExit();
        when(plugin.getDataFolder()).thenReturn(dataDir);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());

        File storageFile = new File(dataDir, "storage.yml");
        storageFile.getParentFile().mkdirs();
        storageFile.createNewFile();

        ConfigLoader loader = new ConfigLoader(plugin);

        StorageService service = new StorageService(plugin, registry, loader) {
            @Override
            public void reconcileRuntimeBans() {
                // not needed in this scenario
            }
        };
        service.initializeStorageExecutor();
        service.setupStorage();

        String summary = service.getStorageSummary();
        assertNotNull(summary);
        assertTrue(summary.toLowerCase().contains("yaml"));

        ProfileRepository profiles = service.getProfileRepository();
        BanRepository bans = service.getBanRepository();
        assertNotNull(profiles);
        assertNotNull(bans);

        service.closeStorage();
        service.shutdownStorageExecutor();
    }

    @Test
    void setupStorageFallsBackToYamlForUnknownType() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        YamlConfiguration pluginConfig = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(pluginConfig);

        File dataDir = Files.createTempDirectory("ezlifesteal-storage-unknown").toFile();
        when(plugin.getDataFolder()).thenReturn(dataDir);

        YamlConfiguration storageConfig = new YamlConfiguration();
        storageConfig.set("type", "custom-db");

        ConfigLoader loader = mock(ConfigLoader.class);
        when(loader.resolveFile("storage.yml")).thenReturn(new File(dataDir, "storage.yml"));
        when(loader.load("storage.yml")).thenReturn(storageConfig);

        StorageService service = new StorageService(plugin, mock(Registry.class), loader) {
            @Override
            public void reconcileRuntimeBans() {
                // avoid Bukkit interactions in this test
            }
        };

        service.setupStorage();

        assertTrue(service.getStorageSummary().contains("fallback for unknown type 'custom-db'"));
    }

    @Test
    void reconcileRuntimeBansAddsOnlyMissingNamedBans() throws Exception {
        StorageService service = new StorageService(mock(EzLifestealPlugin.class), mock(Registry.class),
                mock(ConfigLoader.class));
        BanRepository repository = mock(BanRepository.class);

        BanRecord newBan = new BanRecord(UUID.randomUUID(), "MissingPlayer", "reason", "console",
                Instant.now(), null, true);
        BanRecord existingBan = new BanRecord(UUID.randomUUID(), "ExistingPlayer", "reason", "console",
                Instant.now(), null, true);
        BanRecord blankPlayer = new BanRecord(UUID.randomUUID(), " ", "reason", "console",
                Instant.now(), null, true);

        when(repository.loadActiveBans()).thenReturn(List.of(newBan, existingBan, blankPlayer));

        setField(service, "banRepository", repository);

        BanList banList = mock(BanList.class);
        com.destroystokyo.paper.profile.PlayerProfile missingProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        com.destroystokyo.paper.profile.PlayerProfile existingProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(banList.isBanned(missingProfile)).thenReturn(false);
        when(banList.isBanned(existingProfile)).thenReturn(true);
        when(banList.getBanEntries()).thenReturn(java.util.Set.of());

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);
            bukkit.when(() -> Bukkit.createProfile(newBan.getUniqueId(), "MissingPlayer")).thenReturn(missingProfile);
            bukkit.when(() -> Bukkit.createProfile(existingBan.getUniqueId(), "ExistingPlayer")).thenReturn(existingProfile);

            service.reconcileRuntimeBans();

            long addBanCalls = Mockito.mockingDetails(banList).getInvocations().stream()
                    .filter(invocation -> invocation.getMethod().getName().equals("addBan"))
                    .count();
            assertEquals(1, addBanCalls);
        }
    }

    @Test
    void importRuntimeBansIntoStoragePersistsSupportedEntries() throws Exception {
        StorageService service = new StorageService(mock(EzLifestealPlugin.class), mock(Registry.class),
                mock(ConfigLoader.class));
        BanRepository repository = mock(BanRepository.class);
        setField(service, "banRepository", repository);

        UUID validUuid = UUID.randomUUID();
        com.destroystokyo.paper.profile.PlayerProfile validProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(validProfile.getId()).thenReturn(validUuid);
        when(validProfile.getName()).thenReturn("KnownPlayer");

        BanEntry validEntry = mock(BanEntry.class);
        when(validEntry.getBanTarget()).thenReturn(validProfile);
        when(validEntry.getReason()).thenReturn("r");
        when(validEntry.getSource()).thenReturn("console");

        com.destroystokyo.paper.profile.PlayerProfile blankProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(blankProfile.getId()).thenReturn(UUID.randomUUID());
        when(blankProfile.getName()).thenReturn("  ");

        BanEntry blankEntry = mock(BanEntry.class);
        when(blankEntry.getBanTarget()).thenReturn(blankProfile);

        BanList banList = mock(BanList.class);
        when(banList.getBanEntries()).thenReturn(Set.of(validEntry, blankEntry));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            service.importRuntimeBansIntoStorage();

            verify(repository, times(1)).saveBan(any(BanRecord.class));
        }
    }

    @Test
    void reconcileRuntimeBansGracefullyHandlesRepositoryErrors() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        StorageService service = new StorageService(plugin, mock(Registry.class), mock(ConfigLoader.class));
        BanRepository repository = mock(BanRepository.class);
        when(repository.loadActiveBans()).thenThrow(new StorageException("load-failed"));
        setField(service, "banRepository", repository);

        BanList emptyBanList = mock(BanList.class);
        when(emptyBanList.getBanEntries()).thenReturn(java.util.Set.of());
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(emptyBanList);
            service.reconcileRuntimeBans();
        }

        verify(repository, times(1)).loadActiveBans();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
