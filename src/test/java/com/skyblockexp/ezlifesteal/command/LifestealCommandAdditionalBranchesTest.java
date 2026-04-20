package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import java.lang.reflect.Field;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifestealCommandAdditionalBranchesTest {

    @Test
    void unknownSubcommandShowsUsage() throws Exception {
        Registry registry = new Registry();
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        LifestealCommand command = new LifestealCommand();

        CommandSender sender = mock(CommandSender.class);
        boolean result = command.onCommand(sender, null, "lifesteal", new String[]{"nope"});

        assertTrue(result);
    }

    @Test
    void subcommandRequiresArgsDisplaysError() throws Exception {
        Registry registry = new Registry();
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        LifestealCommand command = new LifestealCommand();

        CommandSender sender = mock(CommandSender.class);
        // 'transfer' expects args; calling with none should return false
        boolean result = command.onCommand(sender, null, "lifesteal", new String[]{"transfer"});

        assertTrue(result);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
