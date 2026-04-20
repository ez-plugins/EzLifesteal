package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifestealCommandErrorsTest {

    @Test
    void addCommand_withInvalidNumber_sendsModifyInvalidAmount() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService ms = mock(MessageService.class);
        PlayerLookupService pls = mock(PlayerLookupService.class);
        org.bukkit.plugin.java.JavaPlugin javaPlugin = mock(org.bukkit.plugin.java.JavaPlugin.class);
        when(plugin.getMessageService()).thenReturn(ms);
        when(plugin.getPlayerLookupService()).thenReturn(pls);
        when(plugin.getPlugin()).thenReturn(javaPlugin);

        LifestealCommand cmd = new LifestealCommand(plugin);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        boolean result = cmd.onCommand(sender, mock(Command.class), "lifesteal", new String[]{"add", "somePlayer",
            "not-a-number"});

        verify(ms).sendMessage(sender, "modify-invalid-amount");
    }

    @Test
    void hologramPlace_byNonPlayer_sendsPlayerOnlyMessage() {
        LifestealCommand cmd = new LifestealCommand();

        CommandSender sender = mock(CommandSender.class);

        boolean result = cmd.onCommand(sender, mock(Command.class), "lifesteal", new String[]{"hologram", "place"});

        verify(sender).sendMessage("This command can only be used by players.");
    }
}
