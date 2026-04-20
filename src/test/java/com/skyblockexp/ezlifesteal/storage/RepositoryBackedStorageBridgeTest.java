package com.skyblockexp.ezlifesteal.storage;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.storage.provider.StorageProvider;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RepositoryBackedStorageBridgeTest {

    @Test
    void delegatesProfileAndBanMethodsExactlyOnce() throws Exception {
        StorageProvider provider = mock(StorageProvider.class);
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        BanRepository banRepository = mock(BanRepository.class);
        when(provider.profiles()).thenReturn(profileRepository);
        when(provider.bans()).thenReturn(banRepository);

        RepositoryBackedStorageBridge bridge = new RepositoryBackedStorageBridge(provider);

        UUID uniqueId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(uniqueId, 12.0D);
        BanRecord banRecord = new BanRecord(uniqueId, "PlayerOne", "Zero hearts", "EzLifesteal",
                Instant.parse("2026-01-02T03:04:05Z"), null, true);

        Optional<LifestealProfile> loadedProfile = Optional.of(profile);
        List<LifestealProfile> topProfiles = List.of(profile);
        Optional<BanRecord> loadedBan = Optional.of(banRecord);
        List<BanRecord> activeBans = List.of(banRecord);

        when(profileRepository.loadProfile(uniqueId)).thenReturn(loadedProfile);
        when(profileRepository.loadTopProfiles(5)).thenReturn(topProfiles);
        when(banRepository.loadBan(uniqueId)).thenReturn(loadedBan);
        when(banRepository.loadActiveBans()).thenReturn(activeBans);

        assertEquals(loadedProfile, bridge.loadProfile(uniqueId));
        bridge.saveProfile(profile);
        assertEquals(topProfiles, bridge.loadTopProfiles(5));
        bridge.resetAll(10.0D);

        bridge.saveBan(banRecord);
        bridge.removeBan(uniqueId);
        assertEquals(loadedBan, bridge.loadBan(uniqueId));
        assertEquals(activeBans, bridge.loadActiveBans());

        verify(provider, times(4)).profiles();
        verify(provider, times(4)).bans();
        verify(profileRepository).loadProfile(uniqueId);
        verify(profileRepository).saveProfile(profile);
        verify(profileRepository).loadTopProfiles(5);
        verify(profileRepository).resetAll(10.0D);
        verify(banRepository).saveBan(banRecord);
        verify(banRepository).removeBan(uniqueId);
        verify(banRepository).loadBan(uniqueId);
        verify(banRepository).loadActiveBans();
        verifyNoMoreInteractions(profileRepository, banRepository);
    }

    @Test
    void delegatesInitAndCloseExactlyOnce() throws Exception {
        StorageProvider provider = mock(StorageProvider.class);
        RepositoryBackedStorageBridge bridge = new RepositoryBackedStorageBridge(provider);

        bridge.init();
        bridge.close();

        verify(provider).init();
        verify(provider).close();
        verifyNoMoreInteractions(provider);
    }

    @Test
    void initAndClosePropagateStorageExceptions() throws Exception {
        StorageProvider initProvider = mock(StorageProvider.class);
        StorageException initFailure = new StorageException("init failed");
        doThrow(initFailure).when(initProvider).init();

        RepositoryBackedStorageBridge initBridge = new RepositoryBackedStorageBridge(initProvider);
        StorageException initThrown = assertThrows(StorageException.class, initBridge::init);
        assertSame(initFailure, initThrown);

        StorageProvider closeProvider = mock(StorageProvider.class);
        StorageException closeFailure = new StorageException("close failed");
        doThrow(closeFailure).when(closeProvider).close();

        RepositoryBackedStorageBridge closeBridge = new RepositoryBackedStorageBridge(closeProvider);
        StorageException closeThrown = assertThrows(StorageException.class, closeBridge::close);
        assertSame(closeFailure, closeThrown);
    }

    @Test
    void propagatesStorageExceptionsFromReadWriteAndDeleteOperations() throws Exception {
        StorageProvider provider = mock(StorageProvider.class);
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        BanRepository banRepository = mock(BanRepository.class);
        when(provider.profiles()).thenReturn(profileRepository);
        when(provider.bans()).thenReturn(banRepository);

        RepositoryBackedStorageBridge bridge = new RepositoryBackedStorageBridge(provider);
        UUID uniqueId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(uniqueId, 10.0D);
        BanRecord banRecord = new BanRecord(uniqueId, "PlayerOne", "reason", "source", Instant.now(), null, true);

        StorageException loadFailure = new StorageException("load profile failed");
        when(profileRepository.loadProfile(uniqueId)).thenThrow(loadFailure);
        assertSame(loadFailure, assertThrows(StorageException.class, () -> bridge.loadProfile(uniqueId)));

        StorageException saveFailure = new StorageException("save profile failed");
        doThrow(saveFailure).when(profileRepository).saveProfile(profile);
        assertSame(saveFailure, assertThrows(StorageException.class, () -> bridge.saveProfile(profile)));

        StorageException removeFailure = new StorageException("remove ban failed");
        doThrow(removeFailure).when(banRepository).removeBan(uniqueId);
        assertSame(removeFailure, assertThrows(StorageException.class, () -> bridge.removeBan(uniqueId)));

        StorageException banLoadFailure = new StorageException("load ban failed");
        when(banRepository.loadBan(uniqueId)).thenThrow(banLoadFailure);
        assertSame(banLoadFailure, assertThrows(StorageException.class, () -> bridge.loadBan(uniqueId)));

        StorageException banSaveFailure = new StorageException("save ban failed");
        doThrow(banSaveFailure).when(banRepository).saveBan(banRecord);
        assertSame(banSaveFailure, assertThrows(StorageException.class, () -> bridge.saveBan(banRecord)));
    }

    @Test
    void nullUuidAndPlayerNameInputsAreDelegatedToRepositories() throws Exception {
        StorageProvider provider = mock(StorageProvider.class);
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        BanRepository banRepository = mock(BanRepository.class);
        when(provider.profiles()).thenReturn(profileRepository);
        when(provider.bans()).thenReturn(banRepository);

        RepositoryBackedStorageBridge bridge = new RepositoryBackedStorageBridge(provider);
        BanRecord nullPlayerNameRecord = new BanRecord(
                UUID.randomUUID(),
                null,
                "reason",
                "source",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                true
        );

        bridge.loadProfile(null);
        bridge.loadBan(null);
        bridge.removeBan(null);
        bridge.saveBan(nullPlayerNameRecord);

        verify(profileRepository).loadProfile(null);
        verify(banRepository).loadBan(null);
        verify(banRepository).removeBan(null);
        verify(banRepository).saveBan(nullPlayerNameRecord);
    }

    @Test
    void resetAllDelegatesBoundaryHeartValues() throws Exception {
        StorageProvider provider = mock(StorageProvider.class);
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        when(provider.profiles()).thenReturn(profileRepository);

        RepositoryBackedStorageBridge bridge = new RepositoryBackedStorageBridge(provider);
        double maxAllowedHearts = 20.0D;
        double beyondMaxHearts = 20.0001D;

        bridge.resetAll(0.0D);
        bridge.resetAll(maxAllowedHearts);
        bridge.resetAll(beyondMaxHearts);

        verify(profileRepository).resetAll(0.0D);
        verify(profileRepository).resetAll(maxAllowedHearts);
        verify(profileRepository).resetAll(beyondMaxHearts);
    }

    @Test
    void banUnbanTransitionsDelegateEvenWhenAlreadyInDesiredState() throws Exception {
        StorageProvider provider = mock(StorageProvider.class);
        BanRepository banRepository = mock(BanRepository.class);
        when(provider.bans()).thenReturn(banRepository);

        RepositoryBackedStorageBridge bridge = new RepositoryBackedStorageBridge(provider);
        UUID uniqueId = UUID.randomUUID();
        BanRecord alreadyBanned = new BanRecord(uniqueId, "PlayerOne", "reason", "source",
                Instant.parse("2026-01-02T00:00:00Z"), null, true);
        BanRecord alreadyUnbanned = new BanRecord(uniqueId, "PlayerOne", "reason", "source",
                Instant.parse("2026-01-02T00:00:00Z"), null, false);

        bridge.saveBan(alreadyBanned);
        bridge.saveBan(alreadyBanned);
        bridge.saveBan(alreadyUnbanned);
        bridge.removeBan(uniqueId);
        bridge.removeBan(uniqueId);

        verify(banRepository, times(2)).saveBan(alreadyBanned);
        verify(banRepository).saveBan(alreadyUnbanned);
        verify(banRepository, times(2)).removeBan(uniqueId);
    }
}
