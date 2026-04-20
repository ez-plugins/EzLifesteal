package com.skyblockexp.ezlifesteal.command;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeartsCommandFeatureTest {

    @Test
    void delegatesLabelAndAppendsHeartsSubcommandPrefix() {
        LifestealCommand lifestealCommand = mock(LifestealCommand.class);
        HeartsCommand heartsCommand = new HeartsCommand(lifestealCommand);

        org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
        org.bukkit.command.Command command = mock(org.bukkit.command.Command.class);
        when(lifestealCommand.onCommand(any(), any(), anyString(), any(String[].class))).thenReturn(true);

        boolean handled = heartsCommand.onCommand(sender, command, "hearts", new String[]{"targetPlayer"});

        assertTrue(handled);
        verify(lifestealCommand).onCommand(eq(sender), eq(command), eq("hearts"),
                argThat(args -> Arrays.equals(args, new String[]{"hearts", "targetPlayer"})));
    }
}
