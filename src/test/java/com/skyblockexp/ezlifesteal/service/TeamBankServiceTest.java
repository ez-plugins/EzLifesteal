package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.integration.TeamsApiTeamResolver;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletionException;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamBankServiceTest {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void depositAndWithdrawSucceed() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 20.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 100.0D);
        Player player = player(playerId);

        TeamBankService.Result deposit = service.deposit(player, 5.0D).join();
        assertEquals(TeamBankService.Status.SUCCESS, deposit.status());
        assertEquals(15.0D, deposit.playerHearts());
        assertEquals(5.0D, deposit.bankHearts());

        TeamBankService.Result withdraw = service.withdraw(player, 2.0D).join();
        assertEquals(TeamBankService.Status.SUCCESS, withdraw.status());
        assertEquals(17.0D, withdraw.playerHearts());
        assertEquals(3.0D, withdraw.bankHearts());
    }

    @Test
    void depositFailsWhenCapExceeded() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 20.0D));
        bankRepository.save(new TeamBankAccount(teamId, 9.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 10.0D);
        TeamBankService.Result result = service.deposit(player(playerId), 2.0D).join();
        assertEquals(TeamBankService.Status.BANK_CAP_EXCEEDED, result.status());
    }

    @Test
    void withdrawFailsWhenBankInsufficient() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 20.0D));
        bankRepository.save(new TeamBankAccount(teamId, 1.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 50.0D);
        TeamBankService.Result result = service.withdraw(player(playerId), 2.0D).join();
        assertEquals(TeamBankService.Status.INSUFFICIENT_BANK_HEARTS, result.status());
    }

    /**
     * Regression test: deposit() used to check isAmountValid() before isTeamBankEnabled(), so
     * calling deposit with an invalid amount when the feature is disabled returned INVALID_AMOUNT
     * instead of DISABLED.
     */
    @Test
    void deposit_featureDisabledWithInvalidAmount_returnsDisabled() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 20.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", false, 100.0D);
        TeamBankService.Result result = service.deposit(player(playerId), -5.0D).join();

        assertEquals(TeamBankService.Status.DISABLED, result.status());
    }

    /**
     * Regression test: withdraw() had the same check-order bug as deposit() — isAmountValid()
     * was tested before isTeamBankEnabled(), masking the disabled state with INVALID_AMOUNT.
     */
    @Test
    void withdraw_featureDisabledWithInvalidAmount_returnsDisabled() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 20.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", false, 100.0D);
        TeamBankService.Result result = service.withdraw(player(playerId), -5.0D).join();

        assertEquals(TeamBankService.Status.DISABLED, result.status());
    }

    @Test
    void deposit_bankSaveFails_rollsBackProfile() {
        TeamBankRepository bankRepository = new TeamBankRepository() {
            @Override
            public Optional<TeamBankAccount> loadAccount(UUID teamId) throws StorageException {
                return Optional.empty();
            }

            @Override
            public void saveAccount(TeamBankAccount account) throws StorageException {
                throw new StorageException("write-fail");
            }
        };
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 20.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 100.0D);
        Player player = player(playerId);

        // Expect the deposit to fail due to bank save error and rollback the profile
        assertThrows(CompletionException.class, () -> service.deposit(player, 5.0D).join());

        // Persisted profile in repository should be rolled back to original value
        assertEquals(20.0D, profileRepository.loadProfile(playerId).orElseThrow().getHearts());
    }

    @Test
    void deposit_profileSaveFails_propagatesCompletionException() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        // Custom ProfileRepository that throws on save
        ProfileRepository profileRepository = new ProfileRepository() {
            private final Map<UUID, LifestealProfile> data = new ConcurrentHashMap<>();
            {
                data.put(playerId, new LifestealProfile(playerId, 20.0D));
            }

            @Override
            public Optional<LifestealProfile> loadProfile(UUID uniqueId) {
                return Optional.ofNullable(data.get(uniqueId));
            }

            @Override
            public void saveProfile(LifestealProfile profile) throws StorageException {
                throw new StorageException("write-fail");
            }

            @Override
            public java.util.List<LifestealProfile> loadTopProfiles(int limit) {
                return java.util.List.of();
            }

            @Override
            public void resetAll(double defaultHearts) {
            }
        };

        // Build plugin + manager manually so we can control profile manager behaviour
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        EzLifestealPlugin ez = mock(EzLifestealPlugin.class);
        when(plugin.getPlugin()).thenReturn(ez);
        when(ez.getStorageExecutor()).thenReturn(executor);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(playerId)).thenAnswer(i -> profileRepository.loadProfile(playerId).orElseGet(() -> new LifestealProfile(playerId, 20.0D)));
        when(manager.getMinHearts()).thenReturn(1.0D);
        when(manager.getMaxHearts()).thenReturn(40.0D);
        when(plugin.getProfileRepository()).thenReturn(profileRepository);
        when(plugin.getTeamBankRepository()).thenReturn(bankRepository);
        when(plugin.isTeamBankEnabled()).thenReturn(true);
        when(plugin.getTeamBankMaxHearts()).thenReturn(100.0D);
        when(plugin.getTeamBankMaxHeartsForTeam(any())).thenReturn(100.0D);

        TeamBankService.TeamResolver resolver = p -> Optional.of(new TeamsApiTeamResolver.TeamContext(teamId, "Alpha"));
        TeamBankService service = new TeamBankService(plugin, resolver);
        Player player = player(playerId);

        assertThrows(CompletionException.class, () -> service.deposit(player, 5.0D).join());
    }

    @Test
    void deposit_bankRollbackFails_logsSevereAndThrows() throws StorageException {
        // Prepare a profile repository that fails on the second save (rollback)
        UUID playerIdSeed = UUID.randomUUID();
        ProfileRepository profileRepository = new ProfileRepository() {
            private final Map<UUID, LifestealProfile> data = new ConcurrentHashMap<>();
            private int calls = 0;
            {
                data.put(playerIdSeed, new LifestealProfile(playerIdSeed, 20.0D));
            }

            @Override
            public Optional<LifestealProfile> loadProfile(UUID uniqueId) {
                return Optional.ofNullable(data.get(uniqueId));
            }

            @Override
            public void saveProfile(LifestealProfile profile) throws StorageException {
                calls++;
                if (calls == 2) {
                    throw new StorageException("rollback-fail");
                }
                data.put(profile.getUniqueId(), new LifestealProfile(profile.getUniqueId(), profile.getHearts()));
            }

            @Override
            public java.util.List<LifestealProfile> loadTopProfiles(int limit) {
                return java.util.List.of();
            }

            @Override
            public void resetAll(double defaultHearts) {
            }
        };

        // Bank repo: fail when attempting to save the updated bank value
        TeamBankRepository bankRepository = new TeamBankRepository() {
            @Override
            public Optional<TeamBankAccount> loadAccount(UUID teamId) throws StorageException {
                return Optional.of(new TeamBankAccount(teamId, 10.0D));
            }

            @Override
            public void saveAccount(TeamBankAccount account) throws StorageException {
                // Always fail to trigger rollback
                throw new StorageException("write-fail");
            }
        };

        // Build plugin mock manually so we can supply a Logger for verification
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        EzLifestealPlugin ez = mock(EzLifestealPlugin.class);
        java.util.logging.Logger logger = mock(java.util.logging.Logger.class);

        when(plugin.getPlugin()).thenReturn(ez);
        when(ez.getStorageExecutor()).thenReturn(executor);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(any())).thenAnswer(i -> profileRepository.loadProfile((UUID) i.getArgument(0)).orElseGet(() -> new LifestealProfile((UUID) i.getArgument(0), 10.0D)));
        when(manager.getMinHearts()).thenReturn(1.0D);
        when(manager.getMaxHearts()).thenReturn(40.0D);
        when(plugin.getProfileRepository()).thenReturn(profileRepository);
        when(plugin.getTeamBankRepository()).thenReturn(bankRepository);
        when(plugin.isTeamBankEnabled()).thenReturn(true);
        when(plugin.getTeamBankMaxHearts()).thenReturn(100.0D);
        when(plugin.getTeamBankMaxHeartsForTeam(any())).thenReturn(100.0D);
        when(plugin.getLogger()).thenReturn(logger);

        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        TeamBankService.TeamResolver resolver = p -> Optional.of(new TeamsApiTeamResolver.TeamContext(teamId, "Alpha"));
        TeamBankService service = new TeamBankService(plugin, resolver);

        Player player = player(playerId);

        assertThrows(CompletionException.class, () -> service.deposit(player, 5.0D).join());
        // Verify logger was used for rollback failure
        org.mockito.Mockito.verify(logger).severe(org.mockito.ArgumentMatchers.contains("Failed to rollback profile"));
    }

    @Test
    void returnsTeamUnavailableWhenNoTeam() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 20.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, null, "", true, 50.0D);
        TeamBankService.Result result = service.balance(player(playerId)).join();
        assertEquals(TeamBankService.Status.TEAM_UNAVAILABLE, result.status());
    }

    @Test
    void balance_featureDisabled_returnsDisabled() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", false, 100.0D);
        TeamBankService.Result result = service.balance(player(playerId)).join();
        assertEquals(TeamBankService.Status.DISABLED, result.status());
    }

    @Test
    void balance_storageUnavailable_returnsStorageUnavailable() {
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));

        TeamBankService service = createService(profileRepository, (TeamBankRepository) null, playerId, teamId, "Alpha", true, 100.0D);
        TeamBankService.Result result = service.balance(player(playerId)).join();
        assertEquals(TeamBankService.Status.STORAGE_UNAVAILABLE, result.status());
    }

    @Test
    void balance_success_returnsBankHearts() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));
        bankRepository.save(new TeamBankAccount(teamId, 42.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 100.0D);
        TeamBankService.Result result = service.balance(player(playerId)).join();
        assertEquals(TeamBankService.Status.SUCCESS, result.status());
        assertEquals(42.0D, result.bankHearts());
    }

    @Test
    void balance_storageException_propagatesAsCompletionException() {
        TeamBankRepository bankRepository = new TeamBankRepository() {
            @Override
            public Optional<TeamBankAccount> loadAccount(UUID teamId) throws StorageException {
                throw new StorageException("read-fail");
            }

            @Override
            public void saveAccount(TeamBankAccount account) throws StorageException {
            }
        };
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 100.0D);
        assertThrows(CompletionException.class, () -> service.balance(player(playerId)).join());
    }

    @Test
    void deposit_invalidAmount_returnsInvalidAmount() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 100.0D);
        TeamBankService.Result result = service.deposit(player(playerId), -1.0D).join();
        assertEquals(TeamBankService.Status.INVALID_AMOUNT, result.status());
    }

    @Test
    void withdraw_invalidAmount_returnsInvalidAmount() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 100.0D);
        TeamBankService.Result result = service.withdraw(player(playerId), 0.0D).join();
        assertEquals(TeamBankService.Status.INVALID_AMOUNT, result.status());
    }

    @Test
    void deposit_storageUnavailable_returnsStorageUnavailable() {
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));

        TeamBankService service = createService(profileRepository, (TeamBankRepository) null, playerId, teamId, "Alpha", true, 100.0D);
        TeamBankService.Result result = service.deposit(player(playerId), 5.0D).join();
        assertEquals(TeamBankService.Status.STORAGE_UNAVAILABLE, result.status());
    }

    @Test
    void deposit_insufficientPlayerHearts_returnsInsufficientPlayerHearts() {
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 1.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 100.0D);
        TeamBankService.Result result = service.deposit(player(playerId), 2.0D).join();
        assertEquals(TeamBankService.Status.INSUFFICIENT_PLAYER_HEARTS, result.status());
    }

    @Test
    void deposit_loadAccountThrows_propagatesAsCompletionException() {
        TeamBankRepository bankRepository = new TeamBankRepository() {
            @Override
            public Optional<TeamBankAccount> loadAccount(UUID teamId) throws StorageException {
                throw new StorageException("read-fail");
            }

            @Override
            public void saveAccount(TeamBankAccount account) throws StorageException {
            }
        };
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));

        TeamBankService service = createService(profileRepository, bankRepository, playerId, teamId, "Alpha", true, 100.0D);
        assertThrows(CompletionException.class, () -> service.deposit(player(playerId), 5.0D).join());
    }

    @Test
    void storageExecutor_default_whenEzPluginExecutorNull_doesNotThrow() {
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        InMemoryTeamBankRepository bankRepository = new InMemoryTeamBankRepository();
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        profileRepository.save(new LifestealProfile(playerId, 10.0D));
        bankRepository.save(new TeamBankAccount(teamId, 0.0D));

        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        EzLifestealPlugin ez = mock(EzLifestealPlugin.class);
        // Do not stub ez.getStorageExecutor() so it returns null (default) -> triggers Runnable::run
        when(plugin.getPlugin()).thenReturn(ez);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(playerId)).thenAnswer(i -> profileRepository.getOrCreate(playerId));
        when(manager.getMinHearts()).thenReturn(1.0D);
        when(manager.getMaxHearts()).thenReturn(40.0D);
        when(plugin.getProfileRepository()).thenReturn(profileRepository);
        when(plugin.getTeamBankRepository()).thenReturn(bankRepository);
        when(plugin.isTeamBankEnabled()).thenReturn(true);
        when(plugin.getTeamBankMaxHearts()).thenReturn(100.0D);
        when(plugin.getTeamBankMaxHeartsForTeam(any())).thenReturn(100.0D);

        TeamBankService.TeamResolver resolver = p -> Optional.of(new TeamsApiTeamResolver.TeamContext(teamId, "Alpha"));
        TeamBankService service = new TeamBankService(plugin, resolver);

        // Should not throw when storage executor is default
        TeamBankService.Result result = service.balance(player(playerId)).join();
        assertEquals(TeamBankService.Status.SUCCESS, result.status());
    }

    private TeamBankService createService(InMemoryProfileRepository profileRepository,
                                          InMemoryTeamBankRepository bankRepository,
                                          UUID playerId,
                                          UUID teamId,
                                          String teamName,
                                          boolean enabled,
                                          double maxHearts) {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        EzLifestealPlugin ez = mock(EzLifestealPlugin.class);
        when(plugin.getPlugin()).thenReturn(ez);
        when(ez.getStorageExecutor()).thenReturn(executor);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(playerId)).thenAnswer(i -> profileRepository.getOrCreate(playerId));
        when(manager.getMinHearts()).thenReturn(1.0D);
        when(manager.getMaxHearts()).thenReturn(40.0D);
        when(plugin.getProfileRepository()).thenReturn(profileRepository);
        when(plugin.getTeamBankRepository()).thenReturn(bankRepository);
        when(plugin.isTeamBankEnabled()).thenReturn(enabled);
        when(plugin.getTeamBankMaxHearts()).thenReturn(maxHearts);
        when(plugin.getTeamBankMaxHeartsForTeam(any())).thenReturn(maxHearts);

        TeamBankService.TeamResolver resolver = p -> teamId == null
                ? Optional.empty()
                : Optional.of(new TeamsApiTeamResolver.TeamContext(teamId, teamName));
        return new TeamBankService(plugin, resolver);
    }

    private TeamBankService createService(InMemoryProfileRepository profileRepository,
                                          TeamBankRepository bankRepository,
                                          UUID playerId,
                                          UUID teamId,
                                          String teamName,
                                          boolean enabled,
                                          double maxHearts) {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        EzLifestealPlugin ez = mock(EzLifestealPlugin.class);
        when(plugin.getPlugin()).thenReturn(ez);
        when(ez.getStorageExecutor()).thenReturn(executor);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(playerId)).thenAnswer(i -> profileRepository.getOrCreate(playerId));
        when(manager.getMinHearts()).thenReturn(1.0D);
        when(manager.getMaxHearts()).thenReturn(40.0D);
        when(plugin.getProfileRepository()).thenReturn(profileRepository);
        when(plugin.getTeamBankRepository()).thenReturn(bankRepository);
        when(plugin.isTeamBankEnabled()).thenReturn(enabled);
        when(plugin.getTeamBankMaxHearts()).thenReturn(maxHearts);
        when(plugin.getTeamBankMaxHeartsForTeam(any())).thenReturn(maxHearts);

        TeamBankService.TeamResolver resolver = p -> teamId == null
                ? Optional.empty()
                : Optional.of(new TeamsApiTeamResolver.TeamContext(teamId, teamName));
        return new TeamBankService(plugin, resolver);
    }

    private Player player(UUID playerId) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        return player;
    }

    private static final class InMemoryTeamBankRepository implements TeamBankRepository {
        private final Map<UUID, TeamBankAccount> data = new ConcurrentHashMap<>();

        @Override
        public Optional<TeamBankAccount> loadAccount(UUID teamId) {
            return Optional.ofNullable(data.get(teamId));
        }

        @Override
        public void saveAccount(TeamBankAccount account) {
            data.put(account.getTeamId(), new TeamBankAccount(account.getTeamId(), account.getHearts()));
        }

        public void save(TeamBankAccount account) {
            saveAccount(account);
        }
    }

    private static final class InMemoryProfileRepository implements ProfileRepository {
        private final Map<UUID, LifestealProfile> data = new ConcurrentHashMap<>();

        @Override
        public Optional<LifestealProfile> loadProfile(UUID uniqueId) {
            return Optional.ofNullable(data.get(uniqueId));
        }

        @Override
        public void saveProfile(LifestealProfile profile) {
            data.put(profile.getUniqueId(), new LifestealProfile(profile.getUniqueId(), profile.getHearts()));
        }

        @Override
        public java.util.List<LifestealProfile> loadTopProfiles(int limit) {
            return java.util.List.of();
        }

        @Override
        public void resetAll(double defaultHearts) {
            // Not used in this test.
        }

        public LifestealProfile getOrCreate(UUID uniqueId) {
            return data.computeIfAbsent(uniqueId, id -> new LifestealProfile(id, 10.0D));
        }

        public void save(LifestealProfile profile) {
            saveProfile(profile);
        }
    }
}
