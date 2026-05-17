package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BanReconciliationTest {

    @Test
    void importRuntimeBansSkipsEntriesWithMissingPlayerNameOrUuid() throws Exception {
        Logger logger = mock(Logger.class);
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(logger);
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        BanRepository repository = mock(BanRepository.class);
        when(repository.loadActiveBans()).thenReturn(List.of());
        setField(services, "banRepository", repository);

        com.destroystokyo.paper.profile.PlayerProfile blankNameProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(blankNameProfile.getName()).thenReturn("   ");
        when(blankNameProfile.getId()).thenReturn(UUID.randomUUID());
        BanEntry blankName = mock(BanEntry.class);
        when(blankName.getBanTarget()).thenReturn(blankNameProfile);

        com.destroystokyo.paper.profile.PlayerProfile missingUuidProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(missingUuidProfile.getName()).thenReturn("NoUuidPlayer");
        when(missingUuidProfile.getId()).thenReturn(null);
        BanEntry missingUuid = mock(BanEntry.class);
        when(missingUuid.getBanTarget()).thenReturn(missingUuidProfile);

        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of(blankName, missingUuid));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(nameBanList);

            invokeReconcileRuntimeBans(services);
        }

        verify(repository, never()).saveBan(any(BanRecord.class));
        verify(repository, never()).loadBan(any(UUID.class));
    }

    @Test
    void importRuntimeBansSkipsWhenRepositoryAlreadyContainsBan() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        BanRepository repository = mock(BanRepository.class);
        UUID uniqueId = UUID.randomUUID();
        when(repository.loadBan(uniqueId)).thenReturn(Optional.of(mock(BanRecord.class)));
        when(repository.loadActiveBans()).thenReturn(List.of());
        setField(services, "banRepository", repository);

        com.destroystokyo.paper.profile.PlayerProfile bannedProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(bannedProfile.getId()).thenReturn(uniqueId);
        when(bannedProfile.getName()).thenReturn("BannedGuy");
        BanEntry entry = mock(BanEntry.class);
        when(entry.getBanTarget()).thenReturn(bannedProfile);
        when(entry.getReason()).thenReturn("reason");
        when(entry.getSource()).thenReturn("EzLifesteal");
        when(entry.getCreated()).thenReturn(Date.from(Instant.parse("2026-01-01T00:00:00Z")));
        when(entry.getExpiration()).thenReturn(null);

        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of(entry));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(nameBanList);

            invokeReconcileRuntimeBans(services);
        }

        verify(repository).loadBan(uniqueId);
        verify(repository, never()).saveBan(any(BanRecord.class));
    }

    @Test
    void reconcileRuntimeBansLogsWarningWhenLoadingActiveBansFails() throws Exception {
        Logger logger = mock(Logger.class);
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(logger);
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        BanRepository repository = mock(BanRepository.class);
        when(repository.loadActiveBans()).thenThrow(new StorageException("boom-load-active"));
        setField(services, "banRepository", repository);

        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(nameBanList);

            invokeReconcileRuntimeBans(services);
        }

        verify(logger).warning(contains("Failed to load active bans from storage: boom-load-active"));
    }

    @Test
    void importsRuntimeBansWhenMissingFromRepository() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        BanRepository repository = mock(BanRepository.class);
        UUID uniqueId = UUID.randomUUID();
        when(repository.loadBan(uniqueId)).thenReturn(Optional.empty());
        when(repository.loadActiveBans()).thenReturn(List.of());
        setField(services, "banRepository", repository);

        com.destroystokyo.paper.profile.PlayerProfile importedProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(importedProfile.getId()).thenReturn(uniqueId);
        when(importedProfile.getName()).thenReturn("BannedGuy");
        BanEntry entry = mock(BanEntry.class);
        when(entry.getBanTarget()).thenReturn(importedProfile);
        when(entry.getReason()).thenReturn("reason");
        when(entry.getSource()).thenReturn("EzLifesteal");
        when(entry.getCreated()).thenReturn(Date.from(Instant.parse("2026-01-01T00:00:00Z")));
        when(entry.getExpiration()).thenReturn(null);

        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of(entry));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(nameBanList);

            invokeReconcileRuntimeBans(services);
        }

        verify(repository).saveBan(any(BanRecord.class));
    }

    @Test
    void reconcileRuntimeBansSyncsPardonsFromBukkit() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        BanRepository repository = mock(BanRepository.class);
        UUID pardoned = UUID.randomUUID();
        UUID stillBanned = UUID.randomUUID();
        BanRecord pardonedRecord = new BanRecord(pardoned, "PardonedPlayer", "reason", "EzLifesteal",
                Instant.parse("2026-01-01T00:00:00Z"), null, true);
        BanRecord stillBannedRecord = new BanRecord(stillBanned, "StillBanned", "reason", "EzLifesteal",
                Instant.parse("2026-01-01T00:00:00Z"), null, true);
        when(repository.loadActiveBans()).thenReturn(List.of(pardonedRecord, stillBannedRecord));
        setField(services, "banRepository", repository);

        com.destroystokyo.paper.profile.PlayerProfile pardonedProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        com.destroystokyo.paper.profile.PlayerProfile stillBannedProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        BanList profileBanList = mock(BanList.class);
        when(profileBanList.getBanEntries()).thenReturn(Set.of());
        when(profileBanList.isBanned(pardonedProfile)).thenReturn(false);
        when(profileBanList.isBanned(stillBannedProfile)).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(profileBanList);
            bukkit.when(() -> Bukkit.createProfile(pardoned, "PardonedPlayer")).thenReturn(pardonedProfile);
            bukkit.when(() -> Bukkit.createProfile(stillBanned, "StillBanned")).thenReturn(stillBannedProfile);

            invokeReconcileRuntimeBans(services);
        }

        verify(repository).removeBan(pardoned);
        verify(repository, never()).removeBan(stillBanned);
        verify(profileBanList, never()).addBan(any(com.destroystokyo.paper.profile.PlayerProfile.class), any(), any(java.time.Instant.class), any());
    }

    private static void invokeReconcileRuntimeBans(DefaultPluginRuntimeServices services) throws Exception {
        Method reconcile = DefaultPluginRuntimeServices.class.getDeclaredMethod("reconcileRuntimeBans");
        reconcile.setAccessible(true);
        reconcile.invoke(services);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
