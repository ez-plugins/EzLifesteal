package com.skyblockexp.ezlifesteal.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerLookupServiceTest {

    @Test
    void nullAndBlankIdentifierReturnEmptyImmediately() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerLookupService service = new PlayerLookupService(plugin);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            Optional<UUID> nullResult = service.lookupUniqueId(null).join();
            Optional<UUID> blankResult = service.lookupUniqueId("   ").join();

            assertTrue(nullResult.isEmpty());
            assertTrue(blankResult.isEmpty());
            bukkit.verifyNoInteractions();
            verify(plugin, never()).getLogger();
        }
    }

    @Test
    void uuidStringPathReturnsParsedUuidWithoutSchedulerUse() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerLookupService service = new PlayerLookupService(plugin);
        UUID expected = UUID.randomUUID();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            Optional<UUID> result = service.lookupUniqueId(expected.toString()).join();

            assertEquals(Optional.of(expected), result);
            bukkit.verify(Bukkit::getAsyncScheduler, times(0));
        }
    }

    @Test
    void exactOnlinePlayerNameReturnsOnlineUuid() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerLookupService service = new PlayerLookupService(plugin);
        Player player = mock(Player.class);
        UUID expected = UUID.randomUUID();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Alice")).thenReturn(player);
            when(player.getUniqueId()).thenReturn(expected);

            Optional<UUID> result = service.lookupUniqueId("Alice").join();

            assertEquals(Optional.of(expected), result);
            bukkit.verify(() -> Bukkit.getOfflinePlayerIfCached("Alice"), times(0));
            bukkit.verify(Bukkit::getAsyncScheduler, times(0));
        }
    }

    @Test
    void cachedOfflinePlayerPathReturnsCachedUuid() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerLookupService service = new PlayerLookupService(plugin);
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        UUID expected = UUID.randomUUID();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayerIfCached("Bob")).thenReturn(offlinePlayer);
            when(offlinePlayer.getUniqueId()).thenReturn(expected);

            Optional<UUID> result = service.lookupUniqueId("Bob").join();

            assertEquals(Optional.of(expected), result);
            bukkit.verify(Bukkit::getAsyncScheduler, times(0));
        }
    }

    @Test
    void bukkitCreateProfileReflectionFailureLogsWarningAndCompletesEmpty() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        PlayerLookupService service = new PlayerLookupService(plugin);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Charlie")).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayerIfCached("Charlie")).thenReturn(null);
            bukkit.when(() -> Bukkit.createProfile("Charlie")).thenThrow(new RuntimeException("boom"));

            Optional<UUID> result = service.lookupUniqueId("Charlie").join();

            assertTrue(result.isEmpty());
            verify(logger)
                    .warning(org.mockito.ArgumentMatchers.contains("Failed to create lookup profile for 'Charlie'"));
            bukkit.verify(Bukkit::getAsyncScheduler, times(0));
        }
    }

    @Test
    void asyncResolvePathCompletesWithResolvedUuidWhenProfileCompletionSucceeds() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerLookupService service = new PlayerLookupService(plugin);
        AsyncScheduler scheduler = mock(AsyncScheduler.class);
        ControllableAsyncRunner asyncRunner = new ControllableAsyncRunner();
        UUID expected = UUID.randomUUID();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Diana")).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayerIfCached("Diana")).thenReturn(null);
            PlayerProfile profile = mock(PlayerProfile.class);
            when(profile.complete(true)).thenReturn(true);
            when(profile.getId()).thenReturn(expected);
            bukkit.when(() -> Bukkit.createProfile("Diana")).thenReturn(profile);
            bukkit.when(Bukkit::getAsyncScheduler).thenReturn(scheduler);

            org.mockito.Mockito.doAnswer(invocation -> {
                Consumer<ScheduledTask> consumer = invocation.getArgument(1);
                asyncRunner.capture(consumer);
                return mock(ScheduledTask.class);
            }).when(scheduler).runNow(eq(plugin), any());

            CompletableFuture<Optional<UUID>> future = service.lookupUniqueId("Diana");
            assertFalse(future.isDone());

            asyncRunner.runCaptured();

            assertEquals(Optional.of(expected), future.join());
        }
    }

    @Test
    void asyncResolveFailureLogsWarningAndCompletesEmpty() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        PlayerLookupService service = new PlayerLookupService(plugin);
        AsyncScheduler scheduler = mock(AsyncScheduler.class);
        ControllableAsyncRunner asyncRunner = new ControllableAsyncRunner();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Eve")).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayerIfCached("Eve")).thenReturn(null);
            PlayerProfile profile = mock(PlayerProfile.class);
            when(profile.complete(true)).thenThrow(new RuntimeException("resolve failed"));
            bukkit.when(() -> Bukkit.createProfile("Eve")).thenReturn(profile);
            bukkit.when(Bukkit::getAsyncScheduler).thenReturn(scheduler);

            org.mockito.Mockito.doAnswer(invocation -> {
                Consumer<ScheduledTask> consumer = invocation.getArgument(1);
                asyncRunner.capture(consumer);
                return mock(ScheduledTask.class);
            }).when(scheduler).runNow(eq(plugin), any());

            CompletableFuture<Optional<UUID>> future = service.lookupUniqueId("Eve");
            asyncRunner.runCaptured();

            assertTrue(future.join().isEmpty());
            verify(logger).warning(org.mockito.ArgumentMatchers.contains("Failed to resolve player identifier 'Eve'"));
        }
    }

    @Test
    void runtimeErrorInPrimaryLookupFallsBackToCachedLookupWithoutLeakingPartialData() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerLookupService service = new PlayerLookupService(plugin);
        OfflinePlayer cachedPlayer = mock(OfflinePlayer.class);
        UUID expected = UUID.randomUUID();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Frank")).thenThrow(new RuntimeException("primary failed"));
            bukkit.when(() -> Bukkit.getOfflinePlayerIfCached("Frank")).thenReturn(cachedPlayer);
            when(cachedPlayer.getUniqueId()).thenReturn(expected);

            Optional<UUID> result = service.lookupUniqueId("Frank").join();

            assertEquals(Optional.of(expected), result);
            bukkit.verify(Bukkit::getAsyncScheduler, times(0));
        }
    }

    @Test
    void runtimeErrorInSecondaryLookupFallsBackToAsyncResolutionWithoutPartialDataLeak() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerLookupService service = new PlayerLookupService(plugin);
        AsyncScheduler scheduler = mock(AsyncScheduler.class);
        ControllableAsyncRunner asyncRunner = new ControllableAsyncRunner();
        UUID resolved = UUID.randomUUID();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Grace")).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayerIfCached("Grace"))
                    .thenThrow(new RuntimeException("secondary failed"));
            PlayerProfile profile = mock(PlayerProfile.class);
            when(profile.complete(true)).thenReturn(true);
            when(profile.getId()).thenReturn(resolved);
            bukkit.when(() -> Bukkit.createProfile("Grace")).thenReturn(profile);
            bukkit.when(Bukkit::getAsyncScheduler).thenReturn(scheduler);

            org.mockito.Mockito.doAnswer(invocation -> {
                Consumer<ScheduledTask> consumer = invocation.getArgument(1);
                asyncRunner.capture(consumer);
                return mock(ScheduledTask.class);
            }).when(scheduler).runNow(eq(plugin), any());

            CompletableFuture<Optional<UUID>> future = service.lookupUniqueId("Grace");
            asyncRunner.runCaptured();

            assertEquals(Optional.of(resolved), future.join());
        }
    }

    @Test
    void runtimeErrorsInBothPrimaryAndSecondaryLookupsCompleteEmptyWhenAsyncPathFails() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        PlayerLookupService service = new PlayerLookupService(plugin);
        AsyncScheduler scheduler = mock(AsyncScheduler.class);
        ControllableAsyncRunner asyncRunner = new ControllableAsyncRunner();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Heidi")).thenThrow(new RuntimeException("primary failed"));
            bukkit.when(() -> Bukkit.getOfflinePlayerIfCached("Heidi"))
                    .thenThrow(new RuntimeException("secondary failed"));
            PlayerProfile profile = mock(PlayerProfile.class);
            when(profile.complete(true)).thenThrow(new RuntimeException("async failed"));
            bukkit.when(() -> Bukkit.createProfile("Heidi")).thenReturn(profile);
            bukkit.when(Bukkit::getAsyncScheduler).thenReturn(scheduler);

            org.mockito.Mockito.doAnswer(invocation -> {
                Consumer<ScheduledTask> consumer = invocation.getArgument(1);
                asyncRunner.capture(consumer);
                return mock(ScheduledTask.class);
            }).when(scheduler).runNow(eq(plugin), any());

            CompletableFuture<Optional<UUID>> future = service.lookupUniqueId("Heidi");
            asyncRunner.runCaptured();

            assertTrue(future.join().isEmpty());
            verify(logger)
                    .warning(org.mockito.ArgumentMatchers.contains("Failed to resolve player identifier 'Heidi'"));
        }
    }

    private static final class ControllableAsyncRunner {
        private Consumer<ScheduledTask> callback;

        void capture(Consumer<ScheduledTask> callback) {
            this.callback = callback;
        }

        void runCaptured() {
            callback.accept(mock(ScheduledTask.class));
        }
    }

}
