package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import com.skyblockexp.lifesteal.seasons.api.events.SeasonResetEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SeasonResetListenerTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkitTestHelper.startServer();
    }

    @AfterEach
    void tearDown() {
        MockBukkitTestHelper.stopServer();
    }

    @Test
    void ignoresEventsThatAreNotSeasonResetEvents() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        Event event = new NonSeasonEvent();

        SeasonResetListener listener = new SeasonResetListener(plugin);

        listener.onSeasonReset(event);

        verifyNoInteractions(plugin);
    }

    @Test
    void seasonResetLogsWarningAndSkipsResetWhenManagerIsUnavailable() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        Event event = new TestSeasonResetEvent();
        Logger logger = mock(Logger.class);

        when(plugin.getLifestealManager()).thenReturn(null);
        when(plugin.getLogger()).thenReturn(logger);

        SeasonResetListener listener = new SeasonResetListener(plugin);

        listener.onSeasonReset(event);

        verify(logger, times(1)).warning(contains("Lifesteal manager is not ready"));
        verify(plugin, never()).requestTopHologramUpdate();
    }

    @Test
    void seasonResetCallsResetAllHeartsAndSchedulesHologramUpdateAndBroadcastOnSuccess() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        Event event = new TestSeasonResetEvent();
        String broadcastMessage = "[EzLifesteal] Hearts were reset for the new season.";

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("season-reset-test"));
        when(plugin.getMessageService()).thenReturn(messageService);
        when(messageService.format(eq("season-reset-broadcast"), any())).thenReturn(broadcastMessage);
        when(manager.resetAllHeartsAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));

        SeasonResetListener listener = new SeasonResetListener(plugin);

        try (MockedStatic<SchedulerAdapter> schedulerMock = mockStatic(SchedulerAdapter.class);
             MockedStatic<org.bukkit.Bukkit> bukkitMock = mockStatic(org.bukkit.Bukkit.class)) {
            schedulerMock.when(() -> SchedulerAdapter.run(eq(plugin.getPlugin()), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable runnable = invocation.getArgument(1);
                        runnable.run();
                        return null;
                    });

            listener.onSeasonReset(event);

            verify(messageService, times(1)).format(eq("season-reset-broadcast"), any());
            bukkitMock.verify(() -> org.bukkit.Bukkit.broadcastMessage(broadcastMessage), times(1));
        }

        verify(manager, times(1)).resetAllHeartsAsync();
        verify(plugin, times(1)).requestTopHologramUpdate();
    }

    @Test
    void seasonResetLogsUnderlyingCauseWhenResetCompletesWithCompletionException() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        Event event = new TestSeasonResetEvent();
        Logger logger = mock(Logger.class);
        IllegalStateException rootCause = new IllegalStateException("boom");

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new CompletionException(rootCause));

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(logger);
        when(manager.resetAllHeartsAsync()).thenReturn(failedFuture);
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));

        SeasonResetListener listener = new SeasonResetListener(plugin);

        assertDoesNotThrow(() -> listener.onSeasonReset(event));

        verify(manager, times(1)).resetAllHeartsAsync();
        verify(logger, times(1)).log(eq(Level.SEVERE), contains("Failed to reset all hearts"), eq(rootCause));
        verify(plugin, never()).requestTopHologramUpdate();
    }

    @Test
    void seasonResetIsTriggeredByRealEzSeasonsSeasonResetEventClass() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        SeasonResetEvent event = new SeasonResetEvent(1000L, 2000L, 3000L, "test");
        String broadcastMessage = "[EzLifesteal] Hearts were reset for the new season.";

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("season-reset-real-event-test"));
        when(plugin.getMessageService()).thenReturn(messageService);
        when(messageService.format(eq("season-reset-broadcast"), any())).thenReturn(broadcastMessage);
        when(manager.resetAllHeartsAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));

        SeasonResetListener listener = new SeasonResetListener(plugin);

        try (MockedStatic<SchedulerAdapter> schedulerMock = mockStatic(SchedulerAdapter.class);
             MockedStatic<org.bukkit.Bukkit> bukkitMock = mockStatic(org.bukkit.Bukkit.class)) {
            schedulerMock.when(() -> SchedulerAdapter.run(eq(plugin.getPlugin()), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable runnable = invocation.getArgument(1);
                        runnable.run();
                        return null;
                    });

            listener.onSeasonReset(event);

            verify(manager, times(1)).resetAllHeartsAsync();
            verify(plugin, times(1)).requestTopHologramUpdate();
            bukkitMock.verify(() -> org.bukkit.Bukkit.broadcastMessage(broadcastMessage), times(1));
        }
    }

    @Test
    void pluginStartupRemainsStableWhenEzSeasonsClassesAreUnavailable() {
        EzLifestealPlugin plugin = assertDoesNotThrow(() -> MockBukkit.load(EzLifestealPlugin.class));

        assertNotNull(plugin);
        assertTrue(plugin.isEnabled());
    }

    private static final class TestSeasonResetEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    private static final class NonSeasonEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }
}
