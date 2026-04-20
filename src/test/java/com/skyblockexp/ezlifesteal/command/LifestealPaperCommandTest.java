package com.skyblockexp.ezlifesteal.command;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifestealPaperCommandTest {

    @Test
    void executeBridgesToExistingLifestealExecutorWhenPermissionCheckPasses() {
        Plugin plugin = mock(Plugin.class);
        LifestealCommand executor = mock(LifestealCommand.class);
        LifestealPaperCommand command = new LifestealPaperCommand(plugin, executor, null);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.command.base")).thenReturn(true);
        when(executor.onCommand(eq(sender), eq(command), eq("lifesteal"), any())).thenReturn(true);

        boolean result = command.execute(sender, "lifesteal", new String[]{"reload"});

        assertTrue(result);
        verify(executor).onCommand(eq(sender), eq(command), eq("lifesteal"), any());
    }

    @Test
    void tabCompletionDelegatesToConfiguredTabCompleterForBothOverloads() {
        Plugin plugin = mock(Plugin.class);
        LifestealCommand executor = mock(LifestealCommand.class);
        TabCompleter tabCompleter = mock(TabCompleter.class);
        LifestealPaperCommand command = new LifestealPaperCommand(plugin, executor, tabCompleter);

        CommandSender sender = mock(CommandSender.class);
        Location location = mock(Location.class);
        List<String> suggestions = List.of("reload");
        when(tabCompleter.onTabComplete(eq(sender), eq(command), eq("ls"), any())).thenReturn(suggestions);

        List<String> defaultOverload = command.tabComplete(sender, "ls", new String[]{"re"});
        List<String> locationOverload = command.tabComplete(sender, "ls", new String[]{"re"}, location);

        assertEquals(suggestions, defaultOverload);
        assertEquals(suggestions, locationOverload);
    }

    @Test
    void getPluginReturnsOwningPluginReference() {
        Plugin plugin = mock(Plugin.class);
        LifestealPaperCommand command = new LifestealPaperCommand(plugin, mock(LifestealCommand.class), null);

        assertSame(plugin, command.getPlugin());
    }
}
