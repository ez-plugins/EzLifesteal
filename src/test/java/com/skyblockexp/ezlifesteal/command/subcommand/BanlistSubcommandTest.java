package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BanlistSubcommandTest {

    @Test
    void executeReturnsWhenPermissionDenied() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealCommand context = context(plugin, false, Runnable::run);

        boolean result = new BanlistSubcommand().execute(mock(CommandSender.class), null, "lifesteal",
                new String[]{"banlist"}, context);

        assertTrue(result);
        verify(context).requirePermissionPublic(any(), eq("lifesteal.admin.banlist"), eq("lifesteal.admin"));
        verify(context, never()).getPluginAccessorPublic();
    }

    @Test
    void executeRejectsInvalidPageArgument() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "abc"},
                context);

        assertTrue(result);
        verify(messageService).sendMessage(sender, "top-invalid-page");
    }

    @Test
    void executeRejectsNonPositivePageArgument() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "0"},
                context);

        assertTrue(result);
        verify(messageService).sendMessage(sender, "top-invalid-page");
    }

    @Test
    void executeServerBanListSendsEmptyWhenNoMatchingSource() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        BanEntry otherSource = mock(BanEntry.class);
        when(otherSource.getSource()).thenReturn("Console");

        BanEntry sourceThrows = mock(BanEntry.class);
        when(sourceThrows.getSource()).thenThrow(new IllegalStateException("boom"));

        BanList banList = mock(BanList.class);
        when(banList.getEntries()).thenReturn(Set.of(otherSource, sourceThrows));

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist"},
                    context);

            assertTrue(result);
            verify(messageService).sendMessage(sender, "banlist-empty");
        }
    }

    @Test
    void executeServerBanListPaginatesAndShowsFooterNextPage() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        List<BanEntry> entries = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            com.destroystokyo.paper.profile.PlayerProfile profile =
                    mock(com.destroystokyo.paper.profile.PlayerProfile.class);
            when(profile.getName()).thenReturn("player-" + i);
            BanEntry entry = mock(BanEntry.class);
            when(entry.getSource()).thenReturn("EzLifesteal");
            when(entry.getBanTarget()).thenReturn(profile);
            when(entry.getReason()).thenReturn("reason-" + i);
            when(entry.getCreated()).thenReturn(new Date(1_000L + i));
            when(entry.getExpiration()).thenReturn(new Date(2_000L + i));
            entries.add(entry);
        }

        BanList banList = mock(BanList.class);
        when(banList.getEntries()).thenReturn(Set.copyOf(entries));

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                    context);

            assertTrue(result);
            verify(messageService).sendMessage(eq(sender), eq("banlist-header"), anyMap());
            verify(messageService, atLeastOnce()).sendMessage(eq(sender), eq("banlist-entry"), anyMap());
            verify(messageService).sendMessage(eq(sender), eq("banlist-footer"), anyMap());
        }
    }

    @Test
    void executeServerBanListSortHandlesNullCreatedCombinations() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        com.destroystokyo.paper.profile.PlayerProfile firstProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(firstProfile.getName()).thenReturn("first");
        BanEntry first = mock(BanEntry.class);
        when(first.getSource()).thenReturn("EzLifesteal");
        when(first.getBanTarget()).thenReturn(firstProfile);
        when(first.getReason()).thenReturn("r1");
        when(first.getCreated()).thenReturn(null);
        when(first.getExpiration()).thenReturn(null);

        com.destroystokyo.paper.profile.PlayerProfile secondProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(secondProfile.getName()).thenReturn("second");
        BanEntry second = mock(BanEntry.class);
        when(second.getSource()).thenReturn("EzLifesteal");
        when(second.getBanTarget()).thenReturn(secondProfile);
        when(second.getReason()).thenReturn("r2");
        when(second.getCreated()).thenReturn(new Date(2_000L));
        when(second.getExpiration()).thenReturn(null);

        com.destroystokyo.paper.profile.PlayerProfile thirdProfile =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(thirdProfile.getName()).thenReturn("third");
        BanEntry third = mock(BanEntry.class);
        when(third.getSource()).thenReturn("EzLifesteal");
        when(third.getBanTarget()).thenReturn(thirdProfile);
        when(third.getReason()).thenReturn("r3");
        when(third.getCreated()).thenReturn(null);
        when(third.getExpiration()).thenReturn(null);

        BanEntry nullSource = mock(BanEntry.class);
        when(nullSource.getSource()).thenReturn(null);

        BanList banList = mock(BanList.class);
        when(banList.getEntries()).thenReturn(Set.of(first, second, third, nullSource));

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                    context);

            assertTrue(result);
            verify(messageService).sendMessage(eq(sender), eq("banlist-footer-end"));
        }
    }

    @Test
    void executeServerBanListSendsFooterEndAndHandlesNullFields() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        BanEntry entry = mock(BanEntry.class);
        when(entry.getSource()).thenReturn("EzLifesteal");
        when(entry.getBanTarget()).thenReturn(null);
        when(entry.getReason()).thenReturn(null);
        when(entry.getCreated()).thenReturn(null);
        when(entry.getExpiration()).thenReturn(null);

        BanList banList = mock(BanList.class);
        when(banList.getEntries()).thenReturn(Set.of(entry));

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                    context);

            assertTrue(result);
            verify(messageService).sendMessage(eq(sender), eq("banlist-footer-end"));
        }
    }

    @Test
    void executeServerBanListFailureLogsAndSendsStorageError() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        Logger logger = mock(Logger.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getLogger()).thenReturn(logger);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenThrow(new RuntimeException("fail"));

            boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                    context);

            assertTrue(result);
            verify(logger).severe(anyString());
            verify(messageService).sendMessage(sender, "storage-error");
        }
    }

    @Test
    void executeRepositoryPathHandlesEmptyRecords() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        BanRepository repository = mock(BanRepository.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getBanRepository()).thenReturn(repository);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        when(repository.loadActiveBans()).thenReturn(List.of());

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(messageService, timeout(1500)).sendMessage(sender, "banlist-empty");
    }

    @Test
    void executeRepositoryPathHandlesNullRecords() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        BanRepository repository = mock(BanRepository.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getBanRepository()).thenReturn(repository);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        when(repository.loadActiveBans()).thenReturn(null);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(messageService, timeout(1500)).sendMessage(sender, "banlist-empty");
    }

    @Test
    void executeRepositoryPathHandlesStorageFailure() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        Logger logger = mock(Logger.class);
        BanRepository repository = mock(BanRepository.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getBanRepository()).thenReturn(repository);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        when(repository.loadActiveBans()).thenThrow(new StorageException("db unavailable"));

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(logger, timeout(1500)).severe(anyString());
        verify(messageService, timeout(1500)).sendMessage(sender, "storage-error");
    }

    @Test
    void executeRepositoryPathFormatsEntriesAndSendsFooterEndOnLastPage() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        BanRepository repository = mock(BanRepository.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getBanRepository()).thenReturn(repository);

        List<BanRecord> records = List.of(
                new BanRecord(UUID.randomUUID(), "", null, "EzLifesteal", Instant.parse("2026-01-01T00:00:00Z"), null,
                        true),
                new BanRecord(UUID.randomUUID(), null, "reason2", "EzLifesteal", Instant.parse("2026-01-01T12:00:00Z"),
                        null, true),
                new BanRecord(
                        UUID.randomUUID(),
                        "good-name",
                        "reason",
                        "EzLifesteal",
                        Instant.parse("2026-01-02T00:00:00Z"),
                        Instant.parse("2026-01-03T00:00:00Z"),
                        true
                )
        );
        when(repository.loadActiveBans()).thenReturn(records);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(messageService, timeout(1500)).sendMessage(eq(sender), eq("banlist-header"), anyMap());
        verify(messageService, timeout(1500).atLeastOnce()).sendMessage(eq(sender), eq("banlist-entry"), anyMap());
        verify(messageService, timeout(1500)).sendMessage(eq(sender), eq("banlist-footer-end"));
    }

    @Test
    void executeRepositoryPathShowsNextPageFooterAndNullCreatedAtFormatting() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        BanRepository repository = mock(BanRepository.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getBanRepository()).thenReturn(repository);

        List<BanRecord> records = new ArrayList<>();
        BanRecord nullCreated = mock(BanRecord.class);
        when(nullCreated.getPlayerName()).thenReturn("named-player");
        when(nullCreated.getReason()).thenReturn("test-reason");
        when(nullCreated.getCreatedAt()).thenReturn(null);
        when(nullCreated.getExpiresAt()).thenReturn(Instant.parse("2026-01-04T00:00:00Z"));
        records.add(nullCreated);
        for (int i = 0; i < 10; i++) {
            records.add(new BanRecord(
                    UUID.randomUUID(),
                    "player-" + i,
                    "reason-" + i,
                    "EzLifesteal",
                    Instant.parse("2026-01-05T00:00:00Z"),
                    null,
                    true
            ));
        }
        when(repository.loadActiveBans()).thenReturn(records);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(messageService, timeout(1500)).sendMessage(eq(sender), eq("banlist-footer"), anyMap());
    }

    private LifestealCommand context(PluginAccessor plugin, boolean hasPermission, Executor executor) {
        LifestealCommand context = mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(hasPermission);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getMainThreadExecutorPublic()).thenReturn(executor);
        return context;
    }
}
