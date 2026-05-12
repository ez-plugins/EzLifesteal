package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesBansTest {

    @Test
    void reconcileRuntimeBansAddsMissingBansToBukkit() throws Exception {
        Logger logger = mock(Logger.class);
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getDataFolder()).thenReturn(new java.io.File("build/tmp/runtime-bans-tests"));

        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        // Prepare a BanRepository that reports one active ban
        BanRepository banRepo = mock(BanRepository.class);
        BanRecord record = new BanRecord(UUID.randomUUID(), "targetPlayer", "the reason", "system",
                Instant.now(), null, true);
        when(banRepo.loadActiveBans()).thenReturn(List.of(record));
        setField(services, "banRepository", banRepo);

        // Mock Bukkit bans API: no existing entries and an empty BanList for import step
        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of());
        com.destroystokyo.paper.profile.PlayerProfile targetProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(nameBanList.isBanned(targetProfile)).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(nameBanList);
            bukkit.when(() -> Bukkit.createProfile(eq(record.getUniqueId()), eq("targetPlayer"))).thenReturn(targetProfile);

            // call private reconcileRuntimeBans
            Method reconc = DefaultPluginRuntimeServices.class.getDeclaredMethod("reconcileRuntimeBans");
            reconc.setAccessible(true);
            reconc.invoke(services);
        }

        verify(nameBanList).addBan(eq(targetProfile), eq("the reason"), isNull(java.util.Date.class), eq("system"));
    }

    @Test
    void importRuntimeBansImportsBukkitBansIntoStorage() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getDataFolder()).thenReturn(new java.io.File("build/tmp/runtime-bans-tests"));

        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        // Create a BanEntry mock that represents a Bukkit ban
        UUID expectedUuid = UUID.randomUUID();
        com.destroystokyo.paper.profile.PlayerProfile importedProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(importedProfile.getId()).thenReturn(expectedUuid);
        when(importedProfile.getName()).thenReturn("importedPlayer");

        BanEntry entry = mock(BanEntry.class);
        when(entry.getBanTarget()).thenReturn(importedProfile);
        when(entry.getReason()).thenReturn("import-reason");
        when(entry.getSource()).thenReturn("console");
        when(entry.getCreated()).thenReturn(new Date());
        when(entry.getExpiration()).thenReturn(null);

        BanList nameBanList = mock(BanList.class);
        when(nameBanList.getBanEntries()).thenReturn(Set.of(entry));

        // Prepare repository to accept a save when load returns empty
        BanRepository banRepo = mock(BanRepository.class);
        when(banRepo.loadBan(expectedUuid)).thenReturn(Optional.empty());
        setField(services, "banRepository", banRepo);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(nameBanList);

            Method importMethod = DefaultPluginRuntimeServices.class.getDeclaredMethod("importRuntimeBansIntoStorage");
            importMethod.setAccessible(true);
            importMethod.invoke(services);
        }

        verify(banRepo).loadBan(expectedUuid);
        verify(banRepo).saveBan(any(BanRecord.class));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
