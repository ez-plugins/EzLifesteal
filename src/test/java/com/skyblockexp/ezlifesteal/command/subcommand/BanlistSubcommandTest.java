package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.util.ban.BanEntryView;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
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

        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(banAdapter.getBanEntries()).thenReturn(Set.of(
                new BanEntryView(null, "player", "reason", "Console", null, null)
        ));

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist"},
                context);

        assertTrue(result);
        verify(messageService).sendMessage(sender, "banlist-empty");
    }

    @Test
    void executeServerBanListPaginatesAndShowsFooterNextPage() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        Set<BanEntryView> entries = new LinkedHashSet<>();
        for (int i = 0; i < 11; i++) {
            entries.add(new BanEntryView(UUID.randomUUID(), "player-" + i, "reason-" + i, "EzLifesteal",
                    new Date(1_000L + i), new Date(2_000L + i)));
        }

        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(banAdapter.getBanEntries()).thenReturn(entries);

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(messageService).sendMessage(eq(sender), eq("banlist-header"), anyMap());
        verify(messageService, atLeastOnce()).sendMessage(eq(sender), eq("banlist-entry"), anyMap());
        verify(messageService).sendMessage(eq(sender), eq("banlist-footer"), anyMap());
    }

    @Test
    void executeServerBanListSortHandlesNullCreatedCombinations() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        BanEntryView first = new BanEntryView(UUID.randomUUID(), "first", "r1", "EzLifesteal", null, null);
        BanEntryView second = new BanEntryView(UUID.randomUUID(), "second", "r2", "EzLifesteal",
                new Date(2_000L), null);
        BanEntryView third = new BanEntryView(UUID.randomUUID(), "third", "r3", "EzLifesteal", null, null);
        BanEntryView nullSource = new BanEntryView(UUID.randomUUID(), "other", null, null, null, null);

        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(banAdapter.getBanEntries()).thenReturn(new LinkedHashSet<>(List.of(first, second, third, nullSource)));

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(messageService).sendMessage(eq(sender), eq("banlist-footer-end"));
    }

    @Test
    void executeServerBanListSendsFooterEndAndHandlesNullFields() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(banAdapter.getBanEntries()).thenReturn(Set.of(
                new BanEntryView(null, null, null, "EzLifesteal", null, null)
        ));

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(messageService).sendMessage(eq(sender), eq("banlist-footer-end"));
    }

    @Test
    void executeServerBanListFailureLogsAndSendsStorageError() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        Logger logger = mock(Logger.class);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getLogger()).thenReturn(logger);

        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(banAdapter.getBanEntries()).thenThrow(new RuntimeException("fail"));

        LifestealCommand context = context(plugin, true, Runnable::run);
        CommandSender sender = mock(CommandSender.class);

        boolean result = new BanlistSubcommand().execute(sender, null, "lifesteal", new String[]{"banlist", "1"},
                context);

        assertTrue(result);
        verify(logger).severe(anyString());
        verify(messageService).sendMessage(sender, "storage-error");
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
