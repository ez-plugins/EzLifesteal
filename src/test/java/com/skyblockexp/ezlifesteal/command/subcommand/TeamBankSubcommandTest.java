package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.TeamBankService;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamBankSubcommandTest {

    @Test
    void sendsInvalidAmountMessage() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        MessageService messages = Mockito.mock(MessageService.class);
        TeamBankService service = Mockito.mock(TeamBankService.class);
        Player player = Mockito.mock(Player.class);
        when(plugin.getMessageService()).thenReturn(messages);
        when(plugin.getTeamBankService()).thenReturn(service);
        when(messages.getMessage("team-bank-invalid-amount")).thenReturn("configured");

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);

        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        subcommand.execute(player, null, "lifesteal", new String[]{"teambank", "deposit", "abc"}, context);

        verify(messages).sendMessage(eq((CommandSender) player), eq("team-bank-invalid-amount"));
    }

    @Test
    void sendsBalanceMessageOnSuccess() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        MessageService messages = Mockito.mock(MessageService.class);
        TeamBankService service = Mockito.mock(TeamBankService.class);
        Player player = Mockito.mock(Player.class);
        when(plugin.getMessageService()).thenReturn(messages);
        when(plugin.getTeamBankService()).thenReturn(service);
        when(service.balance(player)).thenReturn(CompletableFuture.completedFuture(
                new TeamBankService.Result(TeamBankService.Status.SUCCESS, 12.0D, 25.0D, "Alpha")
        ));

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);

        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        subcommand.execute(player, null, "lifesteal", new String[]{"teambank", "balance"}, context);

        verify(messages).sendMessage(eq((CommandSender) player), eq("team-bank-balance"), any());
    }
}
