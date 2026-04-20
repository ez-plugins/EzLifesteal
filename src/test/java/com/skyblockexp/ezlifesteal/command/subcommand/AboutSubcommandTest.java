package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.command.MessageCapturingSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AboutSubcommandTest {

    @Test
    void executeRejectsNonPlayerSender() {
        AboutSubcommand subcommand = new AboutSubcommand();
        LifestealCommand context = new LifestealCommand();
        MessageCapturingSender sender = new MessageCapturingSender();

        subcommand.execute(sender.getProxy(), mock(Command.class), "lifesteal", new String[]{"about"}, context);

        assertTrue(sender.getMessages().stream().anyMatch(message -> message.contains("only be used by players")));
    }

    @Test
    void executeSendsPluginInfoToPlayer() {
        AboutSubcommand subcommand = new AboutSubcommand();
        LifestealCommand context = new LifestealCommand();
        Player player = mock(Player.class);

        subcommand.execute(player, mock(Command.class), "lifesteal", new String[]{"about"}, context);

        verify(player, atLeastOnce()).sendMessage(contains("Version:"));
    }
}
