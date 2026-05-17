package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.integration.TeamsApiTeamResolver;
import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService.AdminResult;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService.TransferResult;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamBankAdminServiceTest {

    private static final Executor SYNC = Runnable::run;

    private PluginAccessor plugin;
    private TeamsApiTeamResolver resolver;
    private TeamBankRepository repo;
    private TeamBankAdminService service;

    private final UUID teamId = UUID.randomUUID();
    private final TeamsApiTeamResolver.TeamContext ctx =
            new TeamsApiTeamResolver.TeamContext(teamId, "Alpha");

    @BeforeEach
    void setUp() throws StorageException {
        plugin = mock(PluginAccessor.class);
        resolver = mock(TeamsApiTeamResolver.class);
        repo = mock(TeamBankRepository.class);

        when(plugin.isTeamBankEnabled()).thenReturn(true);
        when(plugin.getTeamBankRepository()).thenReturn(repo);
        when(plugin.getTeamBankMaxHeartsForTeam(teamId)).thenReturn(100.0D);
        when(resolver.resolveTeamByName("Alpha")).thenReturn(Optional.of(ctx));
        when(repo.loadAccount(teamId)).thenReturn(Optional.of(new TeamBankAccount(teamId, 20.0D)));

        service = new TeamBankAdminService(plugin, resolver, SYNC);
    }

    // -------------------------------------------------------------------------
    // DISABLED short-circuit
    // -------------------------------------------------------------------------

    @Test
    void adminBalance_disabled_returnsDisabled() {
        when(plugin.isTeamBankEnabled()).thenReturn(false);
        TeamBankAdminService.AdminResult result = service.adminBalance("Alpha").join();
        assertEquals(TeamBankAdminService.AdminStatus.DISABLED, result.status());
    }

    @Test
    void adminDeposit_disabled_returnsDisabled() {
        when(plugin.isTeamBankEnabled()).thenReturn(false);
        TeamBankAdminService.AdminResult result = service.adminDeposit("Alpha", 5.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.DISABLED, result.status());
    }

    @Test
    void adminWithdraw_disabled_returnsDisabled() {
        when(plugin.isTeamBankEnabled()).thenReturn(false);
        TeamBankAdminService.AdminResult result = service.adminWithdraw("Alpha", 5.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.DISABLED, result.status());
    }

    @Test
    void adminReset_disabled_returnsDisabled() {
        when(plugin.isTeamBankEnabled()).thenReturn(false);
        TeamBankAdminService.AdminResult result = service.adminReset("Alpha").join();
        assertEquals(TeamBankAdminService.AdminStatus.DISABLED, result.status());
    }

    @Test
    void adminTransfer_disabled_returnsDisabled() {
        when(plugin.isTeamBankEnabled()).thenReturn(false);
        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", 5.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.DISABLED, result.status());
    }

    // -------------------------------------------------------------------------
    // INVALID_AMOUNT
    // -------------------------------------------------------------------------

    @Test
    void adminDeposit_zeroAmount_returnsInvalidAmount() {
        TeamBankAdminService.AdminResult result = service.adminDeposit("Alpha", 0.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.INVALID_AMOUNT, result.status());
    }

    @Test
    void adminDeposit_negativeAmount_returnsInvalidAmount() {
        TeamBankAdminService.AdminResult result = service.adminDeposit("Alpha", -5.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.INVALID_AMOUNT, result.status());
    }

    @Test
    void adminDeposit_nanAmount_returnsInvalidAmount() {
        TeamBankAdminService.AdminResult result = service.adminDeposit("Alpha", Double.NaN).join();
        assertEquals(TeamBankAdminService.AdminStatus.INVALID_AMOUNT, result.status());
    }

    @Test
    void adminWithdraw_zeroAmount_returnsInvalidAmount() {
        TeamBankAdminService.AdminResult result = service.adminWithdraw("Alpha", 0.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.INVALID_AMOUNT, result.status());
    }

    @Test
    void adminTransfer_negativeAmount_returnsInvalidAmount() {
        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", -1.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.INVALID_AMOUNT, result.status());
    }

    @Test
    void adminTransfer_infiniteAmount_returnsInvalidAmount() {
        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", Double.POSITIVE_INFINITY).join();
        assertEquals(TeamBankAdminService.TransferStatus.INVALID_AMOUNT, result.status());
    }

    // -------------------------------------------------------------------------
    // TEAM_NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    void adminBalance_teamNotFound_returnsTeamNotFound() throws Exception {
        when(resolver.resolveTeamByName("Unknown")).thenReturn(Optional.empty());
        TeamBankAdminService.AdminResult result = service.adminBalance("Unknown").join();
        assertEquals(TeamBankAdminService.AdminStatus.TEAM_NOT_FOUND, result.status());
    }

    @Test
    void adminDeposit_teamNotFound_returnsTeamNotFound() throws Exception {
        when(resolver.resolveTeamByName("Unknown")).thenReturn(Optional.empty());
        TeamBankAdminService.AdminResult result = service.adminDeposit("Unknown", 5.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.TEAM_NOT_FOUND, result.status());
    }

    @Test
    void adminWithdraw_teamNotFound_returnsTeamNotFound() throws Exception {
        when(resolver.resolveTeamByName("Unknown")).thenReturn(Optional.empty());
        TeamBankAdminService.AdminResult result = service.adminWithdraw("Unknown", 5.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.TEAM_NOT_FOUND, result.status());
    }

    @Test
    void adminReset_teamNotFound_returnsTeamNotFound() throws Exception {
        when(resolver.resolveTeamByName("Unknown")).thenReturn(Optional.empty());
        TeamBankAdminService.AdminResult result = service.adminReset("Unknown").join();
        assertEquals(TeamBankAdminService.AdminStatus.TEAM_NOT_FOUND, result.status());
    }

    @Test
    void adminTransfer_nullResolverMakesFromTeamNotFound() {
        TeamBankAdminService noResolver = new TeamBankAdminService(plugin, null, SYNC);
        TeamBankAdminService.TransferResult result = noResolver.adminTransfer("Alpha", "Beta", 5.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.FROM_TEAM_NOT_FOUND, result.status());
    }

    @Test
    void adminTransfer_toTeamNotFound_returnsToTeamNotFound() throws Exception {
        UUID betaId = UUID.randomUUID();
        when(resolver.resolveTeamByName("Beta")).thenReturn(Optional.empty());
        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", 5.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.TO_TEAM_NOT_FOUND, result.status());
    }

    // -------------------------------------------------------------------------
    // STORAGE_UNAVAILABLE
    // -------------------------------------------------------------------------

    @Test
    void adminBalance_storageUnavailable_returnsStorageUnavailable() {
        when(plugin.getTeamBankRepository()).thenReturn(null);
        TeamBankAdminService.AdminResult result = service.adminBalance("Alpha").join();
        assertEquals(TeamBankAdminService.AdminStatus.STORAGE_UNAVAILABLE, result.status());
    }

    @Test
    void adminDeposit_storageUnavailable_returnsStorageUnavailable() {
        when(plugin.getTeamBankRepository()).thenReturn(null);
        TeamBankAdminService.AdminResult result = service.adminDeposit("Alpha", 5.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.STORAGE_UNAVAILABLE, result.status());
    }

    @Test
    void adminWithdraw_storageUnavailable_returnsStorageUnavailable() {
        when(plugin.getTeamBankRepository()).thenReturn(null);
        TeamBankAdminService.AdminResult result = service.adminWithdraw("Alpha", 5.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.STORAGE_UNAVAILABLE, result.status());
    }

    @Test
    void adminReset_storageUnavailable_returnsStorageUnavailable() {
        when(plugin.getTeamBankRepository()).thenReturn(null);
        TeamBankAdminService.AdminResult result = service.adminReset("Alpha").join();
        assertEquals(TeamBankAdminService.AdminStatus.STORAGE_UNAVAILABLE, result.status());
    }

    @Test
    void adminTransfer_storageUnavailable_returnsStorageUnavailable() throws Exception {
        UUID betaId = UUID.randomUUID();
        TeamsApiTeamResolver.TeamContext betaCtx = new TeamsApiTeamResolver.TeamContext(betaId, "Beta");
        when(resolver.resolveTeamByName("Beta")).thenReturn(Optional.of(betaCtx));
        when(plugin.getTeamBankRepository()).thenReturn(null);

        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", 5.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.STORAGE_UNAVAILABLE, result.status());
    }

    // -------------------------------------------------------------------------
    // SUCCESS paths
    // -------------------------------------------------------------------------

    @Test
    void adminBalance_success_returnsCorrectBalance() throws StorageException {
        TeamBankAdminService.AdminResult result = service.adminBalance("Alpha").join();
        assertEquals(TeamBankAdminService.AdminStatus.SUCCESS, result.status());
        assertEquals(20.0D, result.bankHearts());
        assertEquals("Alpha", result.teamName());
    }

    @Test
    void adminBalance_noAccountInStorage_returnsZero() throws StorageException {
        when(repo.loadAccount(teamId)).thenReturn(Optional.empty());
        TeamBankAdminService.AdminResult result = service.adminBalance("Alpha").join();
        assertEquals(TeamBankAdminService.AdminStatus.SUCCESS, result.status());
        assertEquals(0.0D, result.bankHearts());
    }

    @Test
    void adminDeposit_success_updatesBalance() throws StorageException {
        TeamBankAdminService.AdminResult result = service.adminDeposit("Alpha", 10.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.SUCCESS, result.status());
        assertEquals(30.0D, result.bankHearts());
    }

    @Test
    void adminDeposit_wouldExceedCap_returnsBankCapExceeded() throws StorageException {
        when(plugin.getTeamBankMaxHeartsForTeam(teamId)).thenReturn(25.0D);
        TeamBankAdminService.AdminResult result = service.adminDeposit("Alpha", 10.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.BANK_CAP_EXCEEDED, result.status());
        verify(repo, never()).saveAccount(any());
    }

    @Test
    void adminWithdraw_success_reducesBalance() throws StorageException {
        TeamBankAdminService.AdminResult result = service.adminWithdraw("Alpha", 5.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.SUCCESS, result.status());
        assertEquals(15.0D, result.bankHearts());
    }

    @Test
    void adminWithdraw_insufficientHearts_returnsInsufficientBankHearts() throws StorageException {
        TeamBankAdminService.AdminResult result = service.adminWithdraw("Alpha", 50.0D).join();
        assertEquals(TeamBankAdminService.AdminStatus.INSUFFICIENT_BANK_HEARTS, result.status());
        verify(repo, never()).saveAccount(any());
    }

    @Test
    void adminReset_success_setsBalanceToZero() throws StorageException {
        TeamBankAdminService.AdminResult result = service.adminReset("Alpha").join();
        assertEquals(TeamBankAdminService.AdminStatus.SUCCESS, result.status());
        assertEquals(0.0D, result.bankHearts());
    }

    @Test
    void adminTransfer_success_movesHearts() throws StorageException {
        UUID betaId = UUID.randomUUID();
        TeamsApiTeamResolver.TeamContext betaCtx = new TeamsApiTeamResolver.TeamContext(betaId, "Beta");
        when(resolver.resolveTeamByName("Beta")).thenReturn(Optional.of(betaCtx));
        when(repo.loadAccount(betaId)).thenReturn(Optional.of(new TeamBankAccount(betaId, 5.0D)));
        when(plugin.getTeamBankMaxHeartsForTeam(betaId)).thenReturn(100.0D);

        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", 10.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.SUCCESS, result.status());
        assertEquals(10.0D, result.fromHearts()); // 20 - 10
        assertEquals(15.0D, result.toHearts());   // 5 + 10
    }

    @Test
    void adminTransfer_insufficientSourceHearts_returnsInsufficientBankHearts() throws StorageException {
        UUID betaId = UUID.randomUUID();
        TeamsApiTeamResolver.TeamContext betaCtx = new TeamsApiTeamResolver.TeamContext(betaId, "Beta");
        when(resolver.resolveTeamByName("Beta")).thenReturn(Optional.of(betaCtx));
        when(repo.loadAccount(betaId)).thenReturn(Optional.of(new TeamBankAccount(betaId, 5.0D)));

        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", 100.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.INSUFFICIENT_BANK_HEARTS, result.status());
    }

    @Test
    void adminTransfer_wouldExceedDestCap_returnsBankCapExceeded() throws StorageException {
        UUID betaId = UUID.randomUUID();
        TeamsApiTeamResolver.TeamContext betaCtx = new TeamsApiTeamResolver.TeamContext(betaId, "Beta");
        when(resolver.resolveTeamByName("Beta")).thenReturn(Optional.of(betaCtx));
        when(repo.loadAccount(betaId)).thenReturn(Optional.of(new TeamBankAccount(betaId, 99.0D)));
        when(plugin.getTeamBankMaxHeartsForTeam(betaId)).thenReturn(100.0D);

        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", 5.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.BANK_CAP_EXCEEDED, result.status());
    }

    @Test
    void adminTransfer_noAccountInSource_treatsAsZero() throws StorageException {
        UUID betaId = UUID.randomUUID();
        TeamsApiTeamResolver.TeamContext betaCtx = new TeamsApiTeamResolver.TeamContext(betaId, "Beta");
        when(resolver.resolveTeamByName("Beta")).thenReturn(Optional.of(betaCtx));
        when(repo.loadAccount(teamId)).thenReturn(Optional.empty());
        when(repo.loadAccount(betaId)).thenReturn(Optional.empty());
        when(plugin.getTeamBankMaxHeartsForTeam(betaId)).thenReturn(100.0D);

        TeamBankAdminService.TransferResult result = service.adminTransfer("Alpha", "Beta", 5.0D).join();
        assertEquals(TeamBankAdminService.TransferStatus.INSUFFICIENT_BANK_HEARTS, result.status());
    }

    // -------------------------------------------------------------------------
    // StorageException propagation
    // -------------------------------------------------------------------------

    @Test
    void adminBalance_storageException_propagatesAsCompletionException() throws StorageException {
        when(repo.loadAccount(teamId)).thenThrow(new StorageException("read-fail"));
        CompletableFuture<AdminResult> future = service.adminBalance("Alpha");
        assertThrows(CompletionException.class, future::join);
    }

    @Test
    void adminDeposit_storageException_propagatesAsCompletionException() throws StorageException {
        when(repo.loadAccount(teamId)).thenThrow(new StorageException("read-fail"));
        CompletableFuture<AdminResult> future = service.adminDeposit("Alpha", 5.0D);
        assertThrows(CompletionException.class, future::join);
    }
}
