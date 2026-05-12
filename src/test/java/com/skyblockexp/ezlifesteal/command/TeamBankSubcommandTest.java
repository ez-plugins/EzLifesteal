package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.command.subcommand.TeamBankSubcommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.TeamBankService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamBankSubcommandTest {

    private PluginAccessor plugin;
    private TeamBankService teamBankService;
    private LifestealCommand context;
    private MessageService messageService;
    private Command command;

    @BeforeEach
    void setUp() {
        plugin = mock(PluginAccessor.class);
        teamBankService = mock(TeamBankService.class);
        messageService = mock(MessageService.class);
        command = mock(Command.class);

        when(plugin.getTeamBankService()).thenReturn(teamBankService);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(messageService.getMessage(anyString())).thenReturn(null);

        context = new LifestealCommand(plugin);
    }

    private Player mockPlayer() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.isOnline()).thenReturn(true);
        return player;
    }

    @Test
    void execute_nonPlayerSender_sendsMessage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        CommandSender consoleSender = mock(CommandSender.class);

        subcommand.execute(consoleSender, command, "lifesteal", new String[]{"teambank", "balance"}, context);

        verify(consoleSender).sendMessage("This command can only be used by players.");
    }

    @Test
    void execute_storageUnavailable_sendsUnavailableMessage() {
        when(plugin.getTeamBankService()).thenReturn(null);
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "balance"}, context);

        verify(player).sendMessage("Team bank storage is unavailable.");
    }

    @Test
    void execute_noSubAction_sendsUsage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank"}, context);

        verify(player).sendMessage("Usage: /lifesteal teambank <balance|deposit|withdraw> [amount]");
    }

    @Test
    void execute_unknownAction_sendsUsage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "unknown"}, context);

        verify(player).sendMessage("Usage: /lifesteal teambank <balance|deposit|withdraw> [amount]");
    }

    @Test
    void execute_balance_invokesBankService() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        TeamBankService.Result result = mock(TeamBankService.Result.class);
        when(result.status()).thenReturn(TeamBankService.Status.SUCCESS);
        when(result.teamName()).thenReturn("Alpha");
        when(result.bankHearts()).thenReturn(10.0D);
        when(result.playerHearts()).thenReturn(20.0D);
        when(teamBankService.balance(player)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "balance"}, context);

        verify(teamBankService).balance(player);
    }

    @Test
    void execute_deposit_missingAmount_sendsUsage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "deposit"}, context);

        verify(player).sendMessage("Usage: /lifesteal teambank deposit <amount>");
    }

    @Test
    void execute_deposit_invalidAmount_sendsInvalidMessage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "deposit", "abc"}, context);

        verify(player).sendMessage("Please enter a valid heart amount greater than zero.");
    }

    @Test
    void execute_deposit_negativeAmount_sendsInvalidMessage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "deposit", "-5"}, context);

        verify(player).sendMessage("Please enter a valid heart amount greater than zero.");
    }

    @Test
    void execute_deposit_zeroAmount_sendsInvalidMessage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "deposit", "0"}, context);

        verify(player).sendMessage("Please enter a valid heart amount greater than zero.");
    }

    @Test
    void execute_deposit_validAmount_invokesBankService() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        TeamBankService.Result result = mock(TeamBankService.Result.class);
        when(result.status()).thenReturn(TeamBankService.Status.SUCCESS);
        when(result.teamName()).thenReturn("Alpha");
        when(result.bankHearts()).thenReturn(5.0D);
        when(result.playerHearts()).thenReturn(15.0D);
        when(teamBankService.deposit(eq(player), eq(5.0D)))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "deposit", "5"}, context);

        verify(teamBankService).deposit(eq(player), eq(5.0D));
    }

    @Test
    void execute_withdraw_missingAmount_sendsUsage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "withdraw"}, context);

        verify(player).sendMessage("Usage: /lifesteal teambank withdraw <amount>");
    }

    @Test
    void execute_withdraw_validAmount_invokesBankService() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        TeamBankService.Result result = mock(TeamBankService.Result.class);
        when(result.status()).thenReturn(TeamBankService.Status.SUCCESS);
        when(result.teamName()).thenReturn("Alpha");
        when(result.bankHearts()).thenReturn(3.0D);
        when(result.playerHearts()).thenReturn(17.0D);
        when(teamBankService.withdraw(eq(player), eq(2.0D)))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "withdraw", "2"}, context);

        verify(teamBankService).withdraw(eq(player), eq(2.0D));
    }

    @Test
    void execute_balance_teamUnavailable_sendsMessage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        TeamBankService.Result result = mock(TeamBankService.Result.class);
        when(result.status()).thenReturn(TeamBankService.Status.TEAM_UNAVAILABLE);
        when(teamBankService.balance(player)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "balance"}, context);

        verify(teamBankService).balance(player);
    }

    @Test
    void execute_deposit_insufficientHearts_sendsMessage() {
        TeamBankSubcommand subcommand = new TeamBankSubcommand();
        Player player = mockPlayer();

        TeamBankService.Result result = mock(TeamBankService.Result.class);
        when(result.status()).thenReturn(TeamBankService.Status.INSUFFICIENT_PLAYER_HEARTS);
        when(teamBankService.deposit(eq(player), eq(3.0D)))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));

        subcommand.execute(player, command, "lifesteal", new String[]{"teambank", "deposit", "3"}, context);

        verify(teamBankService).deposit(eq(player), eq(3.0D));
    }
}
