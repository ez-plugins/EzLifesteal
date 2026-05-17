package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.integration.TeamsApiTeamResolver;
import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Admin-facing team bank operations that do not require an online player.
 *
 * <p>All mutating methods are thread-safe via per-team locks. Locks for two-team
 * operations (transfer) are acquired in lexicographic UUID order to prevent deadlocks.</p>
 */
public final class TeamBankAdminService {

    public enum AdminStatus {
        SUCCESS,
        DISABLED,
        INVALID_AMOUNT,
        TEAM_NOT_FOUND,
        STORAGE_UNAVAILABLE,
        INSUFFICIENT_BANK_HEARTS,
        BANK_CAP_EXCEEDED,
        TEAMS_API_UNAVAILABLE
    }

    public enum TransferStatus {
        SUCCESS,
        DISABLED,
        INVALID_AMOUNT,
        FROM_TEAM_NOT_FOUND,
        TO_TEAM_NOT_FOUND,
        STORAGE_UNAVAILABLE,
        INSUFFICIENT_BANK_HEARTS,
        BANK_CAP_EXCEEDED,
        TEAMS_API_UNAVAILABLE
    }

    public record AdminResult(AdminStatus status, String teamName, double bankHearts) {
    }

    public record TransferResult(
            TransferStatus status,
            String fromTeam,
            String toTeam,
            double fromHearts,
            double toHearts
    ) {
    }

    private final PluginAccessor plugin;
    private final TeamsApiTeamResolver resolver;
    private final Executor storageExecutor;
    private final Map<UUID, Object> teamLocks = new ConcurrentHashMap<>();

    public TeamBankAdminService(PluginAccessor plugin, TeamsApiTeamResolver resolver, Executor storageExecutor) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.storageExecutor = storageExecutor;
    }

    /** Returns the current heart balance for the given team. */
    public CompletableFuture<AdminResult> adminBalance(String teamInput) {
        if (!plugin.isTeamBankEnabled()) {
            return CompletableFuture.completedFuture(new AdminResult(AdminStatus.DISABLED, teamInput, 0.0D));
        }
        return CompletableFuture.supplyAsync(() -> {
            final TeamsApiTeamResolver.TeamContext team = resolveTeam(teamInput);
            if (team == null) {
                return new AdminResult(AdminStatus.TEAM_NOT_FOUND, teamInput, 0.0D);
            }
            final TeamBankRepository repo = plugin.getTeamBankRepository();
            if (repo == null) {
                return new AdminResult(AdminStatus.STORAGE_UNAVAILABLE, team.teamName(), 0.0D);
            }
            try {
                final double balance = repo.loadAccount(team.teamId())
                        .map(TeamBankAccount::getHearts)
                        .orElse(0.0D);
                return new AdminResult(AdminStatus.SUCCESS, team.teamName(), balance);
            } catch (StorageException exception) {
                throw new CompletionException(exception);
            }
        }, storageExecutor);
    }

    /** Deposits {@code amount} hearts into the team bank, respecting the per-team cap. */
    public CompletableFuture<AdminResult> adminDeposit(String teamInput, double amount) {
        if (!plugin.isTeamBankEnabled()) {
            return CompletableFuture.completedFuture(new AdminResult(AdminStatus.DISABLED, teamInput, 0.0D));
        }
        if (!isAmountValid(amount)) {
            return CompletableFuture.completedFuture(new AdminResult(AdminStatus.INVALID_AMOUNT, teamInput, 0.0D));
        }
        return CompletableFuture.supplyAsync(() -> {
            final TeamsApiTeamResolver.TeamContext team = resolveTeam(teamInput);
            if (team == null) {
                return new AdminResult(AdminStatus.TEAM_NOT_FOUND, teamInput, 0.0D);
            }
            final TeamBankRepository repo = plugin.getTeamBankRepository();
            if (repo == null) {
                return new AdminResult(AdminStatus.STORAGE_UNAVAILABLE, team.teamName(), 0.0D);
            }
            final Object lock = teamLocks.computeIfAbsent(team.teamId(), ignored -> new Object());
            synchronized (lock) {
                try {
                    final TeamBankAccount account = repo.loadAccount(team.teamId())
                            .orElseGet(() -> new TeamBankAccount(team.teamId(), 0.0D));
                    final double max = plugin.getTeamBankMaxHeartsForTeam(team.teamId());
                    final double afterBank = account.getHearts() + amount;
                    if (afterBank > max) {
                        return new AdminResult(AdminStatus.BANK_CAP_EXCEEDED, team.teamName(), account.getHearts());
                    }
                    account.setHearts(afterBank);
                    repo.saveAccount(account);
                    return new AdminResult(AdminStatus.SUCCESS, team.teamName(), afterBank);
                } catch (StorageException exception) {
                    throw new CompletionException(exception);
                }
            }
        }, storageExecutor);
    }

    /** Withdraws {@code amount} hearts from the team bank. */
    public CompletableFuture<AdminResult> adminWithdraw(String teamInput, double amount) {
        if (!plugin.isTeamBankEnabled()) {
            return CompletableFuture.completedFuture(new AdminResult(AdminStatus.DISABLED, teamInput, 0.0D));
        }
        if (!isAmountValid(amount)) {
            return CompletableFuture.completedFuture(new AdminResult(AdminStatus.INVALID_AMOUNT, teamInput, 0.0D));
        }
        return CompletableFuture.supplyAsync(() -> {
            final TeamsApiTeamResolver.TeamContext team = resolveTeam(teamInput);
            if (team == null) {
                return new AdminResult(AdminStatus.TEAM_NOT_FOUND, teamInput, 0.0D);
            }
            final TeamBankRepository repo = plugin.getTeamBankRepository();
            if (repo == null) {
                return new AdminResult(AdminStatus.STORAGE_UNAVAILABLE, team.teamName(), 0.0D);
            }
            final Object lock = teamLocks.computeIfAbsent(team.teamId(), ignored -> new Object());
            synchronized (lock) {
                try {
                    final TeamBankAccount account = repo.loadAccount(team.teamId())
                            .orElseGet(() -> new TeamBankAccount(team.teamId(), 0.0D));
                    if (account.getHearts() < amount) {
                        return new AdminResult(AdminStatus.INSUFFICIENT_BANK_HEARTS, team.teamName(), account.getHearts());
                    }
                    final double afterBank = account.getHearts() - amount;
                    account.setHearts(afterBank);
                    repo.saveAccount(account);
                    return new AdminResult(AdminStatus.SUCCESS, team.teamName(), afterBank);
                } catch (StorageException exception) {
                    throw new CompletionException(exception);
                }
            }
        }, storageExecutor);
    }

    /** Resets the team bank balance to zero. */
    public CompletableFuture<AdminResult> adminReset(String teamInput) {
        if (!plugin.isTeamBankEnabled()) {
            return CompletableFuture.completedFuture(new AdminResult(AdminStatus.DISABLED, teamInput, 0.0D));
        }
        return CompletableFuture.supplyAsync(() -> {
            final TeamsApiTeamResolver.TeamContext team = resolveTeam(teamInput);
            if (team == null) {
                return new AdminResult(AdminStatus.TEAM_NOT_FOUND, teamInput, 0.0D);
            }
            final TeamBankRepository repo = plugin.getTeamBankRepository();
            if (repo == null) {
                return new AdminResult(AdminStatus.STORAGE_UNAVAILABLE, team.teamName(), 0.0D);
            }
            final Object lock = teamLocks.computeIfAbsent(team.teamId(), ignored -> new Object());
            synchronized (lock) {
                try {
                    final TeamBankAccount account = repo.loadAccount(team.teamId())
                            .orElseGet(() -> new TeamBankAccount(team.teamId(), 0.0D));
                    account.setHearts(0.0D);
                    repo.saveAccount(account);
                    return new AdminResult(AdminStatus.SUCCESS, team.teamName(), 0.0D);
                } catch (StorageException exception) {
                    throw new CompletionException(exception);
                }
            }
        }, storageExecutor);
    }

    /**
     * Transfers {@code amount} hearts from one team bank to another.
     * Locks are acquired in lexicographic UUID order to prevent deadlocks.
     */
    public CompletableFuture<TransferResult> adminTransfer(String fromInput, String toInput, double amount) {
        if (!plugin.isTeamBankEnabled()) {
            return CompletableFuture.completedFuture(
                    new TransferResult(TransferStatus.DISABLED, fromInput, toInput, 0.0D, 0.0D));
        }
        if (!isAmountValid(amount)) {
            return CompletableFuture.completedFuture(
                    new TransferResult(TransferStatus.INVALID_AMOUNT, fromInput, toInput, 0.0D, 0.0D));
        }
        return CompletableFuture.supplyAsync(() -> {
            final TeamsApiTeamResolver.TeamContext fromTeam = resolveTeam(fromInput);
            if (fromTeam == null) {
                return new TransferResult(TransferStatus.FROM_TEAM_NOT_FOUND, fromInput, toInput, 0.0D, 0.0D);
            }
            final TeamsApiTeamResolver.TeamContext toTeam = resolveTeam(toInput);
            if (toTeam == null) {
                return new TransferResult(TransferStatus.TO_TEAM_NOT_FOUND, fromTeam.teamName(), toInput, 0.0D, 0.0D);
            }
            final TeamBankRepository repo = plugin.getTeamBankRepository();
            if (repo == null) {
                return new TransferResult(TransferStatus.STORAGE_UNAVAILABLE,
                        fromTeam.teamName(), toTeam.teamName(), 0.0D, 0.0D);
            }
            // Acquire locks in lexicographic UUID order to prevent deadlocks
            final boolean fromFirst = fromTeam.teamId().compareTo(toTeam.teamId()) <= 0;
            final UUID firstId = fromFirst ? fromTeam.teamId() : toTeam.teamId();
            final UUID secondId = fromFirst ? toTeam.teamId() : fromTeam.teamId();
            final Object firstLock = teamLocks.computeIfAbsent(firstId, ignored -> new Object());
            final Object secondLock = teamLocks.computeIfAbsent(secondId, ignored -> new Object());
            synchronized (firstLock) {
                synchronized (secondLock) {
                    try {
                        final TeamBankAccount fromAccount = repo.loadAccount(fromTeam.teamId())
                                .orElseGet(() -> new TeamBankAccount(fromTeam.teamId(), 0.0D));
                        final TeamBankAccount toAccount = repo.loadAccount(toTeam.teamId())
                                .orElseGet(() -> new TeamBankAccount(toTeam.teamId(), 0.0D));
                        if (fromAccount.getHearts() < amount) {
                            return new TransferResult(TransferStatus.INSUFFICIENT_BANK_HEARTS,
                                    fromTeam.teamName(), toTeam.teamName(),
                                    fromAccount.getHearts(), toAccount.getHearts());
                        }
                        final double toMax = plugin.getTeamBankMaxHeartsForTeam(toTeam.teamId());
                        if (toAccount.getHearts() + amount > toMax) {
                            return new TransferResult(TransferStatus.BANK_CAP_EXCEEDED,
                                    fromTeam.teamName(), toTeam.teamName(),
                                    fromAccount.getHearts(), toAccount.getHearts());
                        }
                        fromAccount.setHearts(fromAccount.getHearts() - amount);
                        toAccount.setHearts(toAccount.getHearts() + amount);
                        repo.saveAccount(fromAccount);
                        repo.saveAccount(toAccount);
                        return new TransferResult(TransferStatus.SUCCESS,
                                fromTeam.teamName(), toTeam.teamName(),
                                fromAccount.getHearts(), toAccount.getHearts());
                    } catch (StorageException exception) {
                        throw new CompletionException(exception);
                    }
                }
            }
        }, storageExecutor);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a team by UUID string or name. Returns null when unresolvable.
     */
    private TeamsApiTeamResolver.TeamContext resolveTeam(String input) {
        if (resolver == null) {
            return null;
        }
        try {
            final Optional<TeamsApiTeamResolver.TeamContext> result = resolver.resolveTeamByName(input);
            return result.orElse(null);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean isAmountValid(double amount) {
        return Double.isFinite(amount) && amount > 0.0D;
    }
}
