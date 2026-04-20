package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.HeartWithdrawService;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WithdrawSubcommandTest {

    @Test
    void withdrawSuccessfulSendsHeartStatusToPlayer() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        LifestealManager manager = Mockito.mock(LifestealManager.class);
        HeartRegistry heartRegistry = Mockito.mock(HeartRegistry.class);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getHeartRegistry()).thenReturn(heartRegistry);

        HeartWithdrawService service = Mockito.mock(HeartWithdrawService.class);
        when(service.withdraw(any(), any(), any())).thenReturn(
                HeartWithdrawService.WithdrawResult.success("basic", 9.0D)
        );

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.formatPublic(anyDouble())).thenReturn("9");

        Player sender = Mockito.mock(Player.class);

        WithdrawSubcommand subcommand = new WithdrawSubcommand(service);
        subcommand.execute(sender, Mockito.mock(Command.class), "lifesteal", new String[]{"withdraw"}, context);

        verify(plugin).sendHeartStatus(sender, 9.0D);
        verify(sender).sendMessage(anyString());
    }
}
