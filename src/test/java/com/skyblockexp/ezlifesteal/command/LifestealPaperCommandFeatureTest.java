package com.skyblockexp.ezlifesteal.command;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;

public class LifestealPaperCommandFeatureTest {

    @Test
    void dispatchesToExecutor() {
        LifestealCommand executor = Mockito.mock(LifestealCommand.class);
        org.bukkit.plugin.Plugin plugin = Mockito.mock(org.bukkit.plugin.Plugin.class);
        LifestealPaperCommand cmd = new LifestealPaperCommand(plugin, executor, null);

        MessageCapturingSender sender = new MessageCapturingSender();
        boolean result = cmd.execute(sender.getProxy(), "lifesteal", new String[]{"test"});

        // execute() returns whatever executor.onCommand returns; default mock returns false
        verify(executor).onCommand(Mockito.any(CommandSender.class), Mockito.any(), Mockito.eq("lifesteal"),
                Mockito.any());
    }
}
