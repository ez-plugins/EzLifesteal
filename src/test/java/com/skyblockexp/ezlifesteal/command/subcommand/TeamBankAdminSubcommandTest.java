package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService.AdminResult;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService.AdminStatus;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService.TransferResult;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService.TransferStatus;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamBankAdminSubcommandTest {

    private PluginAccessor plugin;
    private MessageService messages;
    private TeamBankAdminService service;
    private CommandSender sender;
    private LifestealCommand context;
    private TeamBankAdminSubcommand subcommand;

    @BeforeEach
    void setUp() {
        plugin = mock(PluginAccessor.class);
        messages = mock(MessageService.class);
        service = mock(TeamBankAdminService.class);
        sender = mock(CommandSender.class);
        context = mock(LifestealCommand.class);

        when(plugin.getMessageService()).thenReturn(messages);
        when(plugin.getTeamBankAdminService()).thenReturn(service);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);

        subcommand = new TeamBankAdminSubcommand();
    }

    // -------------------------------------------------------------------------
    // Permission / guard branches
    // -------------------------------------------------------------------------

    @Test
    void execute_noPermission_returnsTrueWithoutDelegating() {
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        subcommand.execute(sender, null, "lifesteal", new String[]{"teambank-admin"}, context);
        // no interaction with the service
        verify(service, org.mockito.Mockito.never()).adminBalance(anyString());
    }

    @Test
    void execute_serviceNull_sendsUnavailableMessage() {
        when(plugin.getTeamBankAdminService()).thenReturn(null);
        subcommand.execute(sender, null, "lifesteal", new String[]{"teambank-admin"}, context);
        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_tooFewArgs_sendsUsage() {
        subcommand.execute(sender, null, "lifesteal", new String[]{"teambank-admin"}, context);
        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_unknownAction_sendsUsage() {
        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "purge"}, context);
        verify(sender).sendMessage(anyString());
    }

    // -------------------------------------------------------------------------
    // balance
    // -------------------------------------------------------------------------

    @Test
    void execute_balance_tooFewArgs_sendsUsage() {
        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "balance"}, context);
        verify(sender).sendMessage(anyString());
        verify(service, org.mockito.Mockito.never()).adminBalance(anyString());
    }

    @Test
    void execute_balance_success_sendsBalanceMessage() {
        AdminResult result = new AdminResult(AdminStatus.SUCCESS, "Alpha", 30.0D);
        when(service.adminBalance("Alpha")).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "balance", "Alpha"}, context);

        verify(messages).sendMessage(eq(sender), eq("team-bank-admin-balance"), any());
    }

    @Test
    void execute_balance_disabled_sendsDirect() {
        AdminResult result = new AdminResult(AdminStatus.DISABLED, "Alpha", 0.0D);
        when(service.adminBalance("Alpha")).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "balance", "Alpha"}, context);

        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_balance_teamNotFound_sendsNotFoundMessage() {
        AdminResult result = new AdminResult(AdminStatus.TEAM_NOT_FOUND, "Unknown", 0.0D);
        when(service.adminBalance("Unknown")).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "balance", "Unknown"}, context);

        verify(messages).sendMessage(eq(sender), eq("team-bank-admin-team-not-found"), any());
    }

    @Test
    void execute_balance_storageUnavailable_sendsDirectMessage() {
        AdminResult result = new AdminResult(AdminStatus.STORAGE_UNAVAILABLE, "Alpha", 0.0D);
        when(service.adminBalance("Alpha")).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "balance", "Alpha"}, context);

        verify(sender).sendMessage(anyString());
    }

    // -------------------------------------------------------------------------
    // deposit
    // -------------------------------------------------------------------------

    @Test
    void execute_deposit_tooFewArgs_sendsUsage() {
        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "deposit", "Alpha"}, context);
        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_deposit_invalidAmountString_sendsErrorAndReturns() {
        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "deposit", "Alpha", "abc"}, context);
        verify(sender).sendMessage(anyString());
        verify(service, org.mockito.Mockito.never()).adminDeposit(anyString(), any(double.class));
    }

    @Test
    void execute_deposit_zeroAmount_sendsErrorAndReturns() {
        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "deposit", "Alpha", "0"}, context);
        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_deposit_success_sendsDepositMessage() {
        AdminResult result = new AdminResult(AdminStatus.SUCCESS, "Alpha", 30.0D);
        when(service.adminDeposit("Alpha", 10.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "deposit", "Alpha", "10"}, context);

        verify(messages).sendMessage(eq(sender), eq("team-bank-admin-deposit"), any());
    }

    @Test
    void execute_deposit_bankCapExceeded_sendsDirectMessage() {
        AdminResult result = new AdminResult(AdminStatus.BANK_CAP_EXCEEDED, "Alpha", 20.0D);
        when(service.adminDeposit("Alpha", 10.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "deposit", "Alpha", "10"}, context);

        verify(sender).sendMessage(anyString());
    }

    // -------------------------------------------------------------------------
    // withdraw
    // -------------------------------------------------------------------------

    @Test
    void execute_withdraw_tooFewArgs_sendsUsage() {
        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "withdraw", "Alpha"}, context);
        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_withdraw_success_sendsWithdrawMessage() {
        AdminResult result = new AdminResult(AdminStatus.SUCCESS, "Alpha", 10.0D);
        when(service.adminWithdraw("Alpha", 5.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "withdraw", "Alpha", "5"}, context);

        verify(messages).sendMessage(eq(sender), eq("team-bank-admin-withdraw"), any());
    }

    @Test
    void execute_withdraw_insufficientHearts_sendsDirectMessage() {
        AdminResult result = new AdminResult(AdminStatus.INSUFFICIENT_BANK_HEARTS, "Alpha", 5.0D);
        when(service.adminWithdraw("Alpha", 50.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "withdraw", "Alpha", "50"}, context);

        verify(sender).sendMessage(anyString());
    }

    // -------------------------------------------------------------------------
    // reset
    // -------------------------------------------------------------------------

    @Test
    void execute_reset_tooFewArgs_sendsUsage() {
        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "reset"}, context);
        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_reset_success_sendsResetMessage() {
        AdminResult result = new AdminResult(AdminStatus.SUCCESS, "Alpha", 0.0D);
        when(service.adminReset("Alpha")).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "reset", "Alpha"}, context);

        verify(messages).sendMessage(eq(sender), eq("team-bank-admin-reset"), any());
    }

    // -------------------------------------------------------------------------
    // transfer
    // -------------------------------------------------------------------------

    @Test
    void execute_transfer_tooFewArgs_sendsUsage() {
        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "transfer", "Alpha", "Beta"}, context);
        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_transfer_success_sendsTransferMessage() {
        TransferResult result = new TransferResult(TransferStatus.SUCCESS, "Alpha", "Beta", 10.0D, 15.0D);
        when(service.adminTransfer("Alpha", "Beta", 5.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "transfer", "Alpha", "Beta", "5"}, context);

        verify(messages).sendMessage(eq(sender), eq("team-bank-admin-transfer"), any());
    }

    @Test
    void execute_transfer_fromTeamNotFound_sendsNotFoundMessage() {
        TransferResult result = new TransferResult(TransferStatus.FROM_TEAM_NOT_FOUND, "Unknown", "Beta", 0.0D, 0.0D);
        when(service.adminTransfer("Unknown", "Beta", 5.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "transfer", "Unknown", "Beta", "5"}, context);

        verify(messages).sendMessage(eq(sender), eq("team-bank-admin-team-not-found"), any());
    }

    @Test
    void execute_transfer_toTeamNotFound_sendsNotFoundMessage() {
        TransferResult result = new TransferResult(TransferStatus.TO_TEAM_NOT_FOUND, "Alpha", "Unknown", 0.0D, 0.0D);
        when(service.adminTransfer("Alpha", "Unknown", 5.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "transfer", "Alpha", "Unknown", "5"}, context);

        verify(messages).sendMessage(eq(sender), eq("team-bank-admin-team-not-found"), any());
    }

    @Test
    void execute_transfer_disabled_sendsDirectMessage() {
        TransferResult result = new TransferResult(TransferStatus.DISABLED, "Alpha", "Beta", 0.0D, 0.0D);
        when(service.adminTransfer("Alpha", "Beta", 5.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "transfer", "Alpha", "Beta", "5"}, context);

        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_transfer_insufficientHearts_sendsDirectMessage() {
        TransferResult result = new TransferResult(TransferStatus.INSUFFICIENT_BANK_HEARTS, "Alpha", "Beta", 0.0D, 0.0D);
        when(service.adminTransfer("Alpha", "Beta", 5.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "transfer", "Alpha", "Beta", "5"}, context);

        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_transfer_bankCapExceeded_sendsDirectMessage() {
        TransferResult result = new TransferResult(TransferStatus.BANK_CAP_EXCEEDED, "Alpha", "Beta", 0.0D, 0.0D);
        when(service.adminTransfer("Alpha", "Beta", 5.0D)).thenReturn(CompletableFuture.completedFuture(result));

        subcommand.execute(sender, null, "lifesteal",
                new String[]{"teambank-admin", "transfer", "Alpha", "Beta", "5"}, context);

        verify(sender).sendMessage(anyString());
    }
}
