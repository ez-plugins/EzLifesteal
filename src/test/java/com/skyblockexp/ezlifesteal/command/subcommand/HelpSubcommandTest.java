package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.command.MessageCapturingSender;
import java.util.Set;
import org.bukkit.command.Command;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class HelpSubcommandTest {

    @Test
    void executeSendsPerSubcommandDescriptions() {
        HelpSubcommand subcommand = new HelpSubcommand();
        LifestealCommand context = new LifestealCommand();
        MessageCapturingSender sender = new MessageCapturingSender(Set.of("lifesteal.command.base"));

        subcommand.execute(sender.getProxy(), mock(Command.class), "lifesteal", new String[]{"help"}, context);

        assertTrue(sender.getMessages().stream().anyMatch(
                message -> message.contains("lifesteal") && message.contains("help")));
        assertTrue(sender.getMessages().stream().anyMatch(message -> message.contains("Show available subcommands")));
    }

    @Test
    void executeSupportsPagination() {
        HelpSubcommand subcommand = new HelpSubcommand();
        LifestealCommand context = new LifestealCommand();
        MessageCapturingSender sender = new MessageCapturingSender();

        subcommand.execute(sender.getProxy(), mock(Command.class), "lifesteal", new String[]{"help", "2"}, context);

        assertTrue(sender.getMessages().stream().anyMatch(message -> message.contains("Page 2/")));
    }
}
