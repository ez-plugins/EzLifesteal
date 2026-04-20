package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigRuntimeService;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.gui.ShopGuiListener;
import com.skyblockexp.ezlifesteal.gui.SmurfGuiListener;
import com.skyblockexp.ezlifesteal.heart.RecipeRuntimeService;
import com.skyblockexp.ezlifesteal.integration.IntegrationRuntimeService;
import com.skyblockexp.ezlifesteal.listener.MobListener;
import com.skyblockexp.ezlifesteal.listener.PlayerListener;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginRuntimeCoordinatorTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("dependencyAvailabilityScenarios")
    void initializePluginGracefullyRegistersCommandsAndListenersAcrossDependencyAvailability(
            String scenarioName) throws Exception {
        Collaborators c = collaborators();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
             MockedConstruction<Metrics> metrics = mockConstruction(Metrics.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(c.pluginManager);

            assertDoesNotThrow(() -> c.coordinator.initializePlugin(),
                    "Plugin initialization should never crash for scenario: " + scenarioName);

            ArgumentCaptor<Listener> listenerCaptor = ArgumentCaptor.forClass(Listener.class);
            verify(c.pluginManager, times(4)).registerEvents(listenerCaptor.capture(), eq(c.plugin));
            Set<Class<?>> listenerTypes = listenerCaptor.getAllValues().stream()
                    .map(Object::getClass)
                    .collect(Collectors.toSet());

            assertEquals(Set.of(PlayerListener.class, MobListener.class, SmurfGuiListener.class, ShopGuiListener.class),
                    listenerTypes,
                    "Core listeners should still be registered for scenario: " + scenarioName);
            verify(c.commandRegistrationService).start();
            verify(c.runtime).setupPlaceholderExpansion(c.pluginAccessor);
            assertEquals(1, metrics.constructed().size());
        }
    }

    @Test
    void initializePluginExecutesStartupSequenceAndRegistersExpectedCoreListeners() throws Exception {
        Collaborators c = collaborators();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
             MockedConstruction<Metrics> metrics = mockConstruction(Metrics.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(c.pluginManager);

            c.coordinator.initializePlugin();

            InOrder order = inOrder(c.runtime, c.configRuntimeService, c.storageRuntimeService,
                    c.integrationRuntimeService, c.recipeRuntimeService, c.commandRegistrationService);
            order.verify(c.runtime).initializeCoreState();
            order.verify(c.configRuntimeService).start();
            order.verify(c.storageRuntimeService).start();
            order.verify(c.runtime).reloadManagerState();
            order.verify(c.integrationRuntimeService).start();
            order.verify(c.recipeRuntimeService).start();
            order.verify(c.commandRegistrationService).start();
            order.verify(c.runtime).setupPlaceholderExpansion(c.pluginAccessor);
            order.verify(c.runtime).logStartupSummary();

            verify(c.runtime).setPlayerListener(any(PlayerListener.class));

            ArgumentCaptor<Listener> listenerCaptor = ArgumentCaptor.forClass(Listener.class);
            verify(c.pluginManager, times(4)).registerEvents(listenerCaptor.capture(), eq(c.plugin));
            Set<Class<?>> listenerTypes = listenerCaptor.getAllValues().stream()
                    .map(Object::getClass)
                    .collect(Collectors.toSet());

            assertEquals(Set.of(PlayerListener.class, MobListener.class, SmurfGuiListener.class, ShopGuiListener.class),
                    listenerTypes);
            assertEquals(1, metrics.constructed().size());
        }
    }

    @Test
    void reloadPluginWithInitiatorStoresInitiatorAndSendsReloadedMessage() throws Exception {
        Collaborators c = collaborators();
        CommandSender initiator = mock(CommandSender.class);

        c.coordinator.reloadPlugin(initiator);

        InOrder order = inOrder(c.pluginContext, c.configRuntimeService, c.storageRuntimeService, c.runtime,
                c.integrationRuntimeService, c.recipeRuntimeService, c.commandRegistrationService, c.messageService);
        order.verify(c.pluginContext).setLastReloadInitiator(initiator);
        order.verify(c.configRuntimeService).reload();
        order.verify(c.storageRuntimeService).reload();
        order.verify(c.runtime).reloadManagerState();
        order.verify(c.integrationRuntimeService).reload();
        order.verify(c.recipeRuntimeService).reload();
        order.verify(c.commandRegistrationService).reload();
        order.verify(c.runtime).setupPlaceholderExpansion(c.pluginAccessor);
        order.verify(c.runtime).logStartupSummary();
        order.verify(c.messageService).sendMessage(initiator, "reloaded");
    }

    @Test
    void reloadPluginWithNullInitiatorSkipsReloadedMessage() throws Exception {
        Collaborators c = collaborators();

        c.coordinator.reloadPlugin(null);

        verify(c.pluginContext).setLastReloadInitiator(null);
        verify(c.messageService, never()).sendMessage(any(CommandSender.class), eq("reloaded"));
    }

    @Test
    void shutdownPluginCallsStopFlowInExpectedOrder() throws Exception {
        Collaborators c = collaborators();

        c.coordinator.shutdownPlugin();

        InOrder order = inOrder(c.recipeRuntimeService, c.integrationRuntimeService, c.runtime,
                c.storageRuntimeService);
        order.verify(c.recipeRuntimeService).stop();
        order.verify(c.integrationRuntimeService).stop();
        order.verify(c.runtime).shutdownManagers();
        order.verify(c.storageRuntimeService).stop();
    }

    @Test
    void initializePluginWhenMetricsInitializationFailsLogsWarningWithoutCrashing() throws Exception {
        Collaborators c = collaborators();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
             MockedConstruction<Metrics> ignored = mockConstruction(Metrics.class,
                     (mock, context) -> {
                         throw new RuntimeException("boom");
                     })) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(c.pluginManager);

            assertDoesNotThrow(() -> c.coordinator.initializePlugin());

            verify(c.logger).warning(startsWith("Failed to initialise bStats metrics:"));
            verify(c.runtime).logStartupSummary();
        }
    }

    private Collaborators collaborators() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        ConfigRuntimeService configRuntimeService = mock(ConfigRuntimeService.class);
        StorageRuntimeService storageRuntimeService = mock(StorageRuntimeService.class);
        IntegrationRuntimeService integrationRuntimeService = mock(IntegrationRuntimeService.class);
        RecipeRuntimeService recipeRuntimeService = mock(RecipeRuntimeService.class);
        CommandRegistrationService commandRegistrationService = mock(CommandRegistrationService.class);
        PluginContext pluginContext = mock(PluginContext.class);
        MessageService messageService = mock(MessageService.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Logger logger = mock(Logger.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(runtime.getZeroHeartBanMessage()).thenReturn("ban");
        when(runtime.getZeroHeartKickMessage()).thenReturn("kick");
        when(runtime.isCombatLogoutProtectionEnabled()).thenReturn(true);
        when(runtime.getCombatLogoutTagDurationMillis()).thenReturn(1_500L);
        when(runtime.getPluginContext()).thenReturn(pluginContext);
        when(runtime.getMessageService()).thenReturn(messageService);
        when(runtime.getWorldOverrideSummary()).thenReturn("");
        // Ensure setupCoreListeners on the mocked runtime simulates the legacy behavior
        org.mockito.Mockito.doAnswer(invocation -> {
            PlayerListener pl = mock(PlayerListener.class);
            runtime.setPlayerListener(pl);
            // simulate plugin manager registering events for the same listeners
            pluginManager.registerEvents(pl, plugin);
            pluginManager.registerEvents(mock(MobListener.class), plugin);
            pluginManager.registerEvents(mock(SmurfGuiListener.class), plugin);
            pluginManager.registerEvents(mock(ShopGuiListener.class), plugin);
            return null;
        }).when(runtime).setupCoreListeners(any());

        PluginRuntimeCoordinator coordinator = new PluginRuntimeCoordinator(plugin, new Registry());

        setField(coordinator, "runtime", runtime);
        setField(coordinator, "pluginAccessor", pluginAccessor);
        setField(coordinator, "configRuntimeService", configRuntimeService);
        setField(coordinator, "storageRuntimeService", storageRuntimeService);
        setField(coordinator, "integrationRuntimeService", integrationRuntimeService);
        setField(coordinator, "recipeRuntimeService", recipeRuntimeService);
        setField(coordinator, "commandRegistrationService", commandRegistrationService);

        return new Collaborators(coordinator, plugin, runtime, pluginAccessor, configRuntimeService,
                storageRuntimeService, integrationRuntimeService, recipeRuntimeService,
                commandRegistrationService, pluginContext, messageService, pluginManager, logger);
    }

    private static Stream<String> dependencyAvailabilityScenarios() {
        return Stream.of(
                "all absent",
                "vault only",
                "placeholderapi only",
                "seasons only",
                "all present"
        );
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record Collaborators(PluginRuntimeCoordinator coordinator,
                                 EzLifestealPlugin plugin,
                                 DefaultPluginRuntimeServices runtime,
                                 PluginAccessor pluginAccessor,
                                 ConfigRuntimeService configRuntimeService,
                                 StorageRuntimeService storageRuntimeService,
                                 IntegrationRuntimeService integrationRuntimeService,
                                 RecipeRuntimeService recipeRuntimeService,
                                 CommandRegistrationService commandRegistrationService,
                                 PluginContext pluginContext,
                                 MessageService messageService,
                                 PluginManager pluginManager,
                                 Logger logger) {
    }
}
