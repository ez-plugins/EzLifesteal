package com.skyblockexp.ezlifesteal.runtime;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class PluginContextTest {

    @Test
    void constructorStoresSameRegistryReference() {
        Registry registry = new Registry();

        PluginContext context = new PluginContext(registry);

        assertSame(registry, context.getRegistry());
    }

    @Test
    void setLastReloadInitiatorThenGetReturnsSameSender() {
        PluginContext context = new PluginContext(new Registry());
        CommandSender sender = mock(CommandSender.class);

        context.setLastReloadInitiator(sender);

        assertSame(sender, context.getLastReloadInitiator());
    }

    @Test
    void setLastReloadInitiatorNullClearsPriorValue() {
        Registry registry = new Registry();
        PluginContext context = new PluginContext(registry);
        CommandSender sender = mock(CommandSender.class);
        context.setLastReloadInitiator(sender);

        context.setLastReloadInitiator(null);

        assertNull(context.getLastReloadInitiator());
        assertSame(registry, context.getRegistry());
    }
}
