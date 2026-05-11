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
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
