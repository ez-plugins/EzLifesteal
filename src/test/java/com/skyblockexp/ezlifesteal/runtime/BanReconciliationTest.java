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

        BanEntry blankName = mock(BanEntry.class);
        when(blankName.getTarget()).thenReturn("   ");

        BanEntry missingUuid = mock(BanEntry.class);
        when(missingUuid.getTarget()).thenReturn("NoUuidPlayer");

        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of(blankName, missingUuid));

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getUniqueId()).thenReturn(null);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(nameBanList);
            bukkit.when(() -> Bukkit.getOfflinePlayer("NoUuidPlayer")).thenReturn(offlinePlayer);

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

        BanEntry entry = mock(BanEntry.class);
        when(entry.getTarget()).thenReturn("BannedGuy");
        when(entry.getReason()).thenReturn("reason");
        when(entry.getSource()).thenReturn("EzLifesteal");
        when(entry.getCreated()).thenReturn(Date.from(Instant.parse("2026-01-01T00:00:00Z")));
        when(entry.getExpiration()).thenReturn(null);

        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of(entry));

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getUniqueId()).thenReturn(uniqueId);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(nameBanList);
            bukkit.when(() -> Bukkit.getOfflinePlayer("BannedGuy")).thenReturn(offlinePlayer);

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
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(nameBanList);

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

        BanEntry entry = mock(BanEntry.class);
        when(entry.getTarget()).thenReturn("BannedGuy");
        when(entry.getReason()).thenReturn("reason");
        when(entry.getSource()).thenReturn("EzLifesteal");
        when(entry.getCreated()).thenReturn(Date.from(Instant.parse("2026-01-01T00:00:00Z")));
        when(entry.getExpiration()).thenReturn(null);

        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of(entry));

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getUniqueId()).thenReturn(uniqueId);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(nameBanList);
            bukkit.when(() -> Bukkit.getOfflinePlayer("BannedGuy")).thenReturn(offlinePlayer);

            invokeReconcileRuntimeBans(services);
        }

        verify(repository).saveBan(any(BanRecord.class));
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
