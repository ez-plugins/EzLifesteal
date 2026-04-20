package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesZeroHeartCommandsTest {

    @Test
    void executeZeroHeartCommandsDoesNothingWhenCommandsAreEmptyOrNull() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        Player victim = player("Victim", UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            setField(services, "zeroHeartCommands", List.of());
            services.executeZeroHeartCommands(victim, null, 1.0);

            setField(services, "zeroHeartCommands", null);
            services.executeZeroHeartCommands(victim, null, 1.0);

            scheduler.verifyNoInteractions();
            bukkit.verifyNoInteractions();
        }
    }

    @Test
    void executeZeroHeartCommandsReplacesVictimPlaceholdersAndFormatsHearts() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        Player victim = player("Victim", UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        Player killer = player("Killer", UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

        setField(services, "zeroHeartCommands", List.of(
                "msg %victim% id=%player_uuid% hearts=%remaining_hearts%",
                "scoreboard players set %victim% hearts %hearts%"
        ));

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).thenAnswer(invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return null;
            });
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            bukkit.when(() -> Bukkit.dispatchCommand(eq(console), anyString())).thenReturn(true);

            services.executeZeroHeartCommands(victim, killer, 2.0);

            bukkit.verify(() -> Bukkit.dispatchCommand(console,
                    "msg Victim id=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa hearts=2"));
            bukkit.verify(() -> Bukkit.dispatchCommand(console,
                    "scoreboard players set Victim hearts 2"));

            services.executeZeroHeartCommands(victim, killer, 2.5);

            bukkit.verify(() -> Bukkit.dispatchCommand(console,
                    "msg Victim id=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa hearts=2.5"));
            bukkit.verify(() -> Bukkit.dispatchCommand(console,
                    "scoreboard players set Victim hearts 2.5"));
        }
    }

    @Test
    void executeZeroHeartCommandsSkipsBlankAndNullCommandEntries() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        Player victim = player("Victim", UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        setField(services, "zeroHeartCommands", Arrays.asList(
                "   ",
                null,
                "say victim=%victim%"
        ));

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).thenAnswer(invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return null;
            });
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            bukkit.when(() -> Bukkit.dispatchCommand(eq(console), anyString())).thenReturn(true);

            services.executeZeroHeartCommands(victim, null, 3.0);

            bukkit.verify(() -> Bukkit.dispatchCommand(console, "say victim=Victim"), times(1));
            bukkit.verify(() -> Bukkit.dispatchCommand(eq(console), anyString()), times(1));
        }
    }

    @Test
    void executeZeroHeartCommandsReplacesNullKillerPlaceholdersWithEmptyStrings() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        Player victim = player("Victim", UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        setField(services, "zeroHeartCommands", Arrays.asList(
                "say victim=%victim% killer=%killer% killerUuid=%killer_uuid%"
        ));

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).thenAnswer(invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return null;
            });
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            bukkit.when(() -> Bukkit.dispatchCommand(eq(console), anyString())).thenReturn(true);

            services.executeZeroHeartCommands(victim, null, 3.0);

            bukkit.verify(() -> Bukkit.dispatchCommand(console,
                    "say victim=Victim killer= killerUuid="), times(1));
            bukkit.verify(() -> Bukkit.dispatchCommand(eq(console), anyString()), times(1));
        }
    }

    private DefaultPluginRuntimeServices runtimeWithBasicPlugin() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        return new DefaultPluginRuntimeServices(plugin, new Registry());
    }

    private static Player player(String name, UUID uuid) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
