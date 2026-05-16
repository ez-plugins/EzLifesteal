package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.integration.TeamsApiTeamResolver;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;

public final class TeamBankService {

    public enum Status {
        SUCCESS,
        DISABLED,
        INVALID_AMOUNT,
        TEAM_UNAVAILABLE,
        PROFILE_UNAVAILABLE,
        STORAGE_UNAVAILABLE,
        INSUFFICIENT_PLAYER_HEARTS,
        INSUFFICIENT_BANK_HEARTS,
        BANK_CAP_EXCEEDED
    }

    public record Result(Status status, double playerHearts, double bankHearts, String teamName) {
    }

    private final PluginAccessor plugin;

    private final TeamResolver teamResolver;

    private final Map<UUID, Object> teamLocks = new ConcurrentHashMap<>();

    public interface TeamResolver {
        Optional<TeamsApiTeamResolver.TeamContext> resolveTeam(Player player);
    }

    public TeamBankService(PluginAccessor plugin, TeamsApiTeamResolver teamResolver) {
        this(plugin, teamResolver::resolveTeam);
    }

    public TeamBankService(PluginAccessor plugin, TeamResolver teamResolver) {
        this.plugin = plugin;
        this.teamResolver = teamResolver;
    }

    public CompletableFuture<Result> balance(Player player) {
        if (!plugin.isTeamBankEnabled()) {
            return CompletableFuture.completedFuture(new Result(Status.DISABLED, 0.0D, 0.0D, ""));
        }
        return withTeamContext(player, team -> CompletableFuture.supplyAsync(() -> {
            final TeamBankRepository bankRepository = plugin.getTeamBankRepository();
            final LifestealManager manager = plugin.getLifestealManager();
            if (bankRepository == null || manager == null) {
                return new Result(Status.STORAGE_UNAVAILABLE, 0.0D, 0.0D, team.teamName());
            }
            final LifestealProfile profile = manager.getOrCreateProfile(player.getUniqueId());
            try {
                final double bankHearts = bankRepository.loadAccount(team.teamId())
                        .map(TeamBankAccount::getHearts)
                        .orElse(0.0D);
                return new Result(Status.SUCCESS, profile.getHearts(), bankHearts, team.teamName());
            }
            catch (StorageException exception) {
                throw new CompletionException(exception);
            }
        }, storageExecutor()));
    }

    public CompletableFuture<Result> deposit(Player player, double amount) {
        if (!plugin.isTeamBankEnabled()) {
            return CompletableFuture.completedFuture(new Result(Status.DISABLED, 0.0D, 0.0D, ""));
        }
        if (!isAmountValid(amount)) {
            return CompletableFuture.completedFuture(new Result(Status.INVALID_AMOUNT, 0.0D, 0.0D, ""));
        }
        return withTeamContext(player, team -> CompletableFuture.supplyAsync(
                () -> applyTransfer(player, team, amount, true),
                storageExecutor()
        ));
    }

    public CompletableFuture<Result> withdraw(Player player, double amount) {
        if (!plugin.isTeamBankEnabled()) {
            return CompletableFuture.completedFuture(new Result(Status.DISABLED, 0.0D, 0.0D, ""));
        }
        if (!isAmountValid(amount)) {
            return CompletableFuture.completedFuture(new Result(Status.INVALID_AMOUNT, 0.0D, 0.0D, ""));
        }
        return withTeamContext(player, team -> CompletableFuture.supplyAsync(
                () -> applyTransfer(player, team, amount, false),
                storageExecutor()
        ));
    }

    private CompletableFuture<Result> withTeamContext(Player player,
            java.util.function.Function<TeamsApiTeamResolver.TeamContext, CompletableFuture<Result>> action) {
        final Optional<TeamsApiTeamResolver.TeamContext> optionalTeam = teamResolver.resolveTeam(player);
        if (optionalTeam.isEmpty()) {
            return CompletableFuture.completedFuture(new Result(Status.TEAM_UNAVAILABLE, 0.0D, 0.0D, ""));
        }
        return action.apply(optionalTeam.get());
    }

    private Result applyTransfer(Player player,
                                 TeamsApiTeamResolver.TeamContext team,
                                 double amount,
                                 boolean deposit) {
        final TeamBankRepository bankRepository = plugin.getTeamBankRepository();
        final ProfileRepository profileRepository = plugin.getProfileRepository();
        final LifestealManager manager = plugin.getLifestealManager();
        if (bankRepository == null || profileRepository == null || manager == null) {
            return new Result(Status.STORAGE_UNAVAILABLE, 0.0D, 0.0D, team.teamName());
        }

        final LifestealProfile profile = manager.getOrCreateProfile(player.getUniqueId());
        final Object lock = teamLocks.computeIfAbsent(team.teamId(), ignored -> new Object());
        synchronized (lock) {
            try {
                final TeamBankAccount account = bankRepository.loadAccount(team.teamId())
                        .orElseGet(() -> new TeamBankAccount(team.teamId(), 0.0D));
                final double playerBefore = profile.getHearts();
                final double bankBefore = account.getHearts();

                if (deposit) {
                    if (playerBefore < amount) {
                        return new Result(Status.INSUFFICIENT_PLAYER_HEARTS, playerBefore, bankBefore, team.teamName());
                    }
                    final double max = plugin.getTeamBankMaxHeartsForTeam(team.teamId());
                    final double afterBank = bankBefore + amount;
                    if (afterBank > max) {
                        return new Result(Status.BANK_CAP_EXCEEDED, playerBefore, bankBefore, team.teamName());
                    }
                    profile.removeHearts(amount, manager.getMinHearts());
                    account.setHearts(afterBank);
                }
                else {
                    if (bankBefore < amount) {
                        return new Result(Status.INSUFFICIENT_BANK_HEARTS, playerBefore, bankBefore, team.teamName());
                    }
                    profile.addHearts(amount, manager.getMaxHearts());
                    account.setHearts(bankBefore - amount);
                }

                final double playerAfter = profile.getHearts();
                final double bankAfter = account.getHearts();
                profileRepository.saveProfile(new LifestealProfile(profile.getUniqueId(), playerAfter));
                bankRepository.saveAccount(account);
                profile.markPersisted(profile.getRevision());
                return new Result(Status.SUCCESS, playerAfter, bankAfter, team.teamName());
            }
            catch (StorageException exception) {
                throw new CompletionException(exception);
            }
        }
    }

    private boolean isAmountValid(double amount) {
        return Double.isFinite(amount) && amount > 0.0D;
    }

    private Executor storageExecutor() {
        if (plugin.getPlugin() instanceof EzLifestealPlugin ez && ez.getStorageExecutor() != null) {
            return ez.getStorageExecutor();
        }
        return Runnable::run;
    }
}
