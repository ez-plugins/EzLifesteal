package com.skyblockexp.ezlifesteal.command;

import java.util.Arrays;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeartsCommandTest {

    @Test
    void delegatesToLifestealHeartsSubcommandWithoutArgs() {
        LifestealCommand lifestealCommand = mock(LifestealCommand.class);
        HeartsCommand heartsCommand = new HeartsCommand(lifestealCommand);
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);

        when(lifestealCommand.onCommand(any(), any(), anyString(), any(String[].class))).thenReturn(true);

        boolean handled = heartsCommand.onCommand(sender, command, "hearts", new String[0]);

        assertTrue(handled);
        verify(lifestealCommand).onCommand(eq(sender), eq(command), eq("hearts"),
                argThat(args -> Arrays.equals(args, new String[]{"hearts"})));
    }

    @Test
    void delegatesToLifestealHeartsSubcommandWithForwardedArgs() {
        LifestealCommand lifestealCommand = mock(LifestealCommand.class);
        HeartsCommand heartsCommand = new HeartsCommand(lifestealCommand);
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);

        when(lifestealCommand.onCommand(any(), any(), anyString(), any(String[].class))).thenReturn(true);

        boolean handled = heartsCommand.onCommand(sender, command, "hearts", new String[]{"Notch"});

        assertTrue(handled);
        verify(lifestealCommand).onCommand(eq(sender), eq(command), eq("hearts"),
                argThat(args -> Arrays.equals(args, new String[]{"hearts", "Notch"})));
    }
}
