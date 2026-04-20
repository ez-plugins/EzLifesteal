package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.command.HeartsCommand;
import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.command.LifestealPaperCommand;
import com.skyblockexp.ezlifesteal.command.LifestealTabCompleter;
import com.skyblockexp.ezlifesteal.command.ReviveCommand;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class CommandRegistrationServiceTest {

    public interface CommandMapServer {
        CommandMap getCommandMap();
    }

    @Test
    void startSetsExecutorAndTabCompleterWhenPluginCommandIsPresent() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        PluginCommand pluginCommand = mock(PluginCommand.class);
        PluginCommand heartsPluginCommand = mock(PluginCommand.class);
        PluginCommand revivePluginCommand = mock(PluginCommand.class);

        Logger logger = mock(Logger.class);
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        CommandMap commandMap = mock(CommandMap.class);

        Server server = mock(Server.class, withSettings().extraInterfaces(CommandMapServer.class));
        when(((CommandMapServer) server).getCommandMap()).thenReturn(commandMap);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDescription()).thenReturn(description);
        when(description.getName()).thenReturn("EzLifesteal");
        when(plugin.getCommand("lifesteal")).thenReturn(pluginCommand);
        when(plugin.getCommand("hearts")).thenReturn(heartsPluginCommand);
        when(plugin.getCommand("revive")).thenReturn(revivePluginCommand);
        when(plugin.getLogger()).thenReturn(logger);

        CommandRegistrationService service = new CommandRegistrationService(plugin, pluginAccessor);
        service.start();

        verify(pluginCommand).setExecutor(any(LifestealCommand.class));
        verify(pluginCommand).setTabCompleter(any(LifestealTabCompleter.class));
        verify(heartsPluginCommand).setExecutor(any(HeartsCommand.class));
        verify(revivePluginCommand).setExecutor(any(ReviveCommand.class));
        verify(logger, never()).severe(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void whenPaperRegistrationSucceedsAndPluginCommandMissingNoSevereIsLogged() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);

        Logger logger = mock(Logger.class);
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        CommandMap commandMap = mock(CommandMap.class);

        Server server = mock(Server.class, withSettings().extraInterfaces(CommandMapServer.class));
        when(((CommandMapServer) server).getCommandMap()).thenReturn(commandMap);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDescription()).thenReturn(description);
        when(description.getName()).thenReturn("EzLifesteal");
        when(plugin.getCommand("lifesteal")).thenReturn(null);
        when(plugin.getLogger()).thenReturn(logger);

        CommandRegistrationService service = new CommandRegistrationService(plugin, pluginAccessor);
        service.start();

        verify(commandMap).register(eq("ezlifesteal"), any(LifestealPaperCommand.class));
        verify(logger, never()).severe(contains("Unable to register /lifesteal command"));
    }

    @ParameterizedTest
    @MethodSource("paperRegistrationFallbackErrors")
    void paperRegistrationFallbackErrorsLogSevereWhenPluginCommandMissing(Consumer<CommandMap> triggerError) {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);

        Logger logger = mock(Logger.class);
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        CommandMap commandMap = mock(CommandMap.class);

        triggerError.accept(commandMap);

        Server server = mock(Server.class, withSettings().extraInterfaces(CommandMapServer.class));
        when(((CommandMapServer) server).getCommandMap()).thenReturn(commandMap);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDescription()).thenReturn(description);
        when(description.getName()).thenReturn("EzLifesteal");
        when(plugin.getCommand("lifesteal")).thenReturn(null);
        when(plugin.getLogger()).thenReturn(logger);

        CommandRegistrationService service = new CommandRegistrationService(plugin, pluginAccessor);
        service.start();

        verify(logger).severe("Unable to register /lifesteal command; command definition is missing from plugin.yml");
    }

    static Stream<Consumer<CommandMap>> paperRegistrationFallbackErrors() {
        return Stream.of(
                commandMap -> doThrow(new NoSuchMethodError("register overload not available"))
                        .when(commandMap).register(any(), any(LifestealPaperCommand.class)),
                commandMap -> doThrow(new UnsupportedOperationException("registration unsupported"))
                        .when(commandMap).register(any(), any(LifestealPaperCommand.class))
        );
    }

    @Test
    void missingPaperCommandMapApiFallsBackAndLogsSevereWhenPluginYmlCommandMissing() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);

        Logger logger = mock(Logger.class);
        Server serverWithoutPaperApi = mock(Server.class);
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);

        when(plugin.getServer()).thenReturn(serverWithoutPaperApi);
        when(plugin.getDescription()).thenReturn(description);
        when(description.getName()).thenReturn("EzLifesteal");
        when(plugin.getCommand("lifesteal")).thenReturn(null);
        when(plugin.getLogger()).thenReturn(logger);

        CommandRegistrationService service = new CommandRegistrationService(plugin, pluginAccessor);
        service.start();

        verify(logger).severe("Unable to register /lifesteal command; command definition is missing from plugin.yml");
    }

    @Test
    void unexpectedThrowableFromPaperRegistrationLogsWarningAndFallsBackSafely() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);

        Logger logger = mock(Logger.class);
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        CommandMap commandMap = mock(CommandMap.class);

        doThrow(new IllegalStateException("boom")).when(commandMap)
                .register(any(), any(LifestealPaperCommand.class));

        Server server = mock(Server.class, withSettings().extraInterfaces(CommandMapServer.class));
        when(((CommandMapServer) server).getCommandMap()).thenReturn(commandMap);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDescription()).thenReturn(description);
        when(description.getName()).thenReturn("EzLifesteal");
        when(plugin.getCommand("lifesteal")).thenReturn(null);
        when(plugin.getLogger()).thenReturn(logger);

        CommandRegistrationService service = new CommandRegistrationService(plugin, pluginAccessor);
        service.start();

        verify(logger).warning("Failed to register Paper command handler: boom");
        verify(logger).severe("Unable to register /lifesteal command; command definition is missing from plugin.yml");
    }

    @Test
    void reloadDelegatesToStartBehavior() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        PluginCommand pluginCommand = mock(PluginCommand.class);
        PluginCommand heartsPluginCommand = mock(PluginCommand.class);
        PluginCommand revivePluginCommand = mock(PluginCommand.class);

        Logger logger = mock(Logger.class);
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        CommandMap commandMap = mock(CommandMap.class);

        Server server = mock(Server.class, withSettings().extraInterfaces(CommandMapServer.class));
        when(((CommandMapServer) server).getCommandMap()).thenReturn(commandMap);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDescription()).thenReturn(description);
        when(description.getName()).thenReturn("EzLifesteal");
        when(plugin.getCommand("lifesteal")).thenReturn(pluginCommand);
        when(plugin.getCommand("hearts")).thenReturn(heartsPluginCommand);
        when(plugin.getCommand("revive")).thenReturn(revivePluginCommand);
        when(plugin.getLogger()).thenReturn(logger);

        CommandRegistrationService service = new CommandRegistrationService(plugin, pluginAccessor);
        service.reload();

        verify(pluginCommand).setExecutor(any(LifestealCommand.class));
        verify(pluginCommand).setTabCompleter(any(LifestealTabCompleter.class));
        verify(heartsPluginCommand).setExecutor(any(HeartsCommand.class));
        verify(revivePluginCommand).setExecutor(any(ReviveCommand.class));
    }
}
