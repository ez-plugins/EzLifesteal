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
    void reconcileRuntimeBansSyncsPardonsFromBukkit() throws Exception {
        StorageService service = new StorageService(mock(EzLifestealPlugin.class), mock(Registry.class),
                mock(ConfigLoader.class));
        BanRepository repository = mock(BanRepository.class);

        UUID pardoned = UUID.randomUUID();
        UUID stillBanned = UUID.randomUUID();
        BanRecord pardonedRecord = new BanRecord(pardoned, "PardonedPlayer", "reason", "console",
                Instant.now(), null, true);
        BanRecord stillBannedRecord = new BanRecord(stillBanned, "StillBanned", "reason", "console",
                Instant.now(), null, true);
        BanRecord blankPlayer = new BanRecord(UUID.randomUUID(), " ", "reason", "console",
                Instant.now(), null, true);

        when(repository.loadActiveBans()).thenReturn(List.of(pardonedRecord, stillBannedRecord, blankPlayer));

        setField(service, "banRepository", repository);

        BanList banList = mock(BanList.class);
        com.destroystokyo.paper.profile.PlayerProfile pardonedProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        com.destroystokyo.paper.profile.PlayerProfile stillBannedProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(banList.isBanned(pardonedProfile)).thenReturn(false);
        when(banList.isBanned(stillBannedProfile)).thenReturn(true);
        when(banList.getBanEntries()).thenReturn(java.util.Set.of());

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);
            bukkit.when(() -> Bukkit.createProfile(pardoned, "PardonedPlayer")).thenReturn(pardonedProfile);
            bukkit.when(() -> Bukkit.createProfile(stillBanned, "StillBanned")).thenReturn(stillBannedProfile);

            service.reconcileRuntimeBans();

            verify(repository).removeBan(pardoned);
            verify(repository, Mockito.never()).removeBan(stillBanned);
            long addBanCalls = Mockito.mockingDetails(banList).getInvocations().stream()
                    .filter(invocation -> invocation.getMethod().getName().equals("addBan"))
                    .count();
            assertEquals(0, addBanCalls);
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

    @Test
    void reconcileRuntimeBans_nullBanRepository_skipsLoadActiveBans() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        StorageService service = new StorageService(plugin, mock(Registry.class), mock(ConfigLoader.class));
        // banRepository is null by default (not injected)

        BanList emptyBanList = mock(BanList.class);
        when(emptyBanList.getBanEntries()).thenReturn(java.util.Set.of());
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(emptyBanList);
            // should not throw; early return after importRuntimeBansIntoStorage
            service.reconcileRuntimeBans();
        }
    }

    @Test
    void reconcileRuntimeBans_emptyActiveBans_doesNotCallRemoveBan() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        StorageService service = new StorageService(plugin, mock(Registry.class), mock(ConfigLoader.class));
        BanRepository repository = mock(BanRepository.class);
        when(repository.loadActiveBans()).thenReturn(List.of());
        setField(service, "banRepository", repository);

        BanList emptyBanList = mock(BanList.class);
        when(emptyBanList.getBanEntries()).thenReturn(java.util.Set.of());
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(emptyBanList);
            service.reconcileRuntimeBans();
        }
        Mockito.verify(repository, Mockito.never()).removeBan(any());
    }

    @Test
    void reconcileRuntimeBans_nullPlayerNameRecord_isSkipped() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        StorageService service = new StorageService(plugin, mock(Registry.class), mock(ConfigLoader.class));
        BanRepository repository = mock(BanRepository.class);

        // Record with null playerName should be skipped
        BanRecord nullNameRecord = new BanRecord(UUID.randomUUID(), null, "r", "s",
                java.time.Instant.now(), null, true);
        when(repository.loadActiveBans()).thenReturn(List.of(nullNameRecord));
        setField(service, "banRepository", repository);

        BanList emptyBanList = mock(BanList.class);
        when(emptyBanList.getBanEntries()).thenReturn(java.util.Set.of());
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(emptyBanList);
            service.reconcileRuntimeBans();
        }
        Mockito.verify(repository, Mockito.never()).removeBan(any());
    }

    @Test
    void reconcileRuntimeBans_removeBanThrows_logsWarning() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        java.util.logging.Logger logger = java.util.logging.Logger.getAnonymousLogger();
        when(plugin.getLogger()).thenReturn(logger);
        StorageService service = new StorageService(plugin, mock(Registry.class), mock(ConfigLoader.class));
        BanRepository repository = mock(BanRepository.class);

        UUID bannedUuid = UUID.randomUUID();
        BanRecord record = new BanRecord(bannedUuid, "TestPlayer", "reason", "console",
                java.time.Instant.now(), null, true);
        when(repository.loadActiveBans()).thenReturn(List.of(record));
        Mockito.doThrow(new StorageException("delete-failed")).when(repository).removeBan(bannedUuid);
        setField(service, "banRepository", repository);

        com.destroystokyo.paper.profile.PlayerProfile profile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        BanList banList = mock(BanList.class);
        when(banList.getBanEntries()).thenReturn(java.util.Set.of());
        when(banList.isBanned(any())).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);
            bukkit.when(() -> Bukkit.createProfile(any(UUID.class), any())).thenReturn(profile);
            // Should not throw — exception is caught and logged
            service.reconcileRuntimeBans();
        }
        Mockito.verify(repository, times(1)).removeBan(bannedUuid);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
