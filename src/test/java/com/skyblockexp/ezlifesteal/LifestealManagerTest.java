package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.service.HealthAttributeResolver;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifestealManagerTest {

    @Test
    void saveProfileAsyncClearsDirtyAfterSuccessfulSave() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
            when(plugin.getStorageExecutor()).thenReturn(executor);

            ProfileRepository profileRepository = mock(ProfileRepository.class);
            HealthAttributeResolver healthAttributeResolver = mock(HealthAttributeResolver.class);

            LifestealManager manager = new LifestealManager(
                    plugin,
                    profileRepository,
                    healthAttributeResolver,
                    10.0,
                    1.0,
                    40.0,
                    false,
                    0.0
            );

            LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 10.0);
            profile.addHearts(2.0, 40.0);
            assertTrue(profile.isDirty());

            manager.saveProfileAsync(profile).get(1, TimeUnit.SECONDS);

            assertFalse(profile.isDirty(), "Profile should be marked clean after successful save");
        }
        finally {
            executor.shutdownNow();
        }
    }

    @Test
    void saveProfileAsyncKeepsDirtyWhenSaveFails() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
            when(plugin.getStorageExecutor()).thenReturn(executor);

            ProfileRepository profileRepository = mock(ProfileRepository.class);
            HealthAttributeResolver healthAttributeResolver = mock(HealthAttributeResolver.class);
            doAnswer(invocation -> {
                throw new StorageException("save failed");
            }).when(profileRepository).saveProfile(org.mockito.ArgumentMatchers.any(LifestealProfile.class));

            LifestealManager manager = new LifestealManager(
                    plugin,
                    profileRepository,
                    healthAttributeResolver,
                    10.0,
                    1.0,
                    40.0,
                    false,
                    0.0
            );

            LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 10.0);
            profile.addHearts(2.0, 40.0);

            assertThrows(ExecutionException.class, () -> manager.saveProfileAsync(profile).get(1, TimeUnit.SECONDS));
            assertTrue(profile.isDirty(), "Profile should remain dirty when save fails");
        }
        finally {
            executor.shutdownNow();
        }
    }

    @Test
    void saveProfileAsyncKeepsDirtyWhenNewMutationHappensBeforeOlderSaveCompletes() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
            when(plugin.getStorageExecutor()).thenReturn(executor);

            ProfileRepository profileRepository = mock(ProfileRepository.class);
            HealthAttributeResolver healthAttributeResolver = mock(HealthAttributeResolver.class);

            CountDownLatch saveInvoked = new CountDownLatch(1);
            CountDownLatch allowSaveToFinish = new CountDownLatch(1);
            doAnswer(invocation -> {
                saveInvoked.countDown();
                if (!allowSaveToFinish.await(1, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to finish save");
                }
                return null;
            }).when(profileRepository).saveProfile(org.mockito.ArgumentMatchers.any(LifestealProfile.class));

            LifestealManager manager = new LifestealManager(
                    plugin,
                    profileRepository,
                    healthAttributeResolver,
                    10.0,
                    1.0,
                    40.0,
                    false,
                    0.0
            );

            LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 10.0);
            profile.addHearts(1.0, 40.0);
            CompletableFuture<Void> firstSave = manager.saveProfileAsync(profile);
            assertTrue(saveInvoked.await(1, TimeUnit.SECONDS), "Save should be in flight");

            profile.addHearts(1.0, 40.0);
            assertTrue(profile.isDirty(), "Profile should be dirty after second mutation");

            allowSaveToFinish.countDown();
            firstSave.get(1, TimeUnit.SECONDS);

            assertTrue(profile.isDirty(), "Older in-flight save must not clear dirty after newer mutation");
        }
        finally {
            executor.shutdownNow();
        }
    }

    @Test
    void retainsDirtyHeartsWhenKillOccursBeforeAsyncLoadCompletes() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            com.skyblockexp.ezlifesteal.EzLifestealPlugin plugin =
                    mock(com.skyblockexp.ezlifesteal.EzLifestealPlugin.class);
            when(plugin.getStorageExecutor()).thenReturn(executor);

            ProfileRepository profileRepository = mock(ProfileRepository.class);
            HealthAttributeResolver healthAttributeResolver = mock(HealthAttributeResolver.class);
            UUID uniqueId = UUID.randomUUID();
            double defaultHearts = 10.0;
            double minHearts = 1.0;
            double maxHearts = 30.0;
            double storedHearts = 5.0;
            CountDownLatch loadInvoked = new CountDownLatch(1);
            CountDownLatch allowLoadToContinue = new CountDownLatch(1);

            when(profileRepository.loadProfile(uniqueId)).thenAnswer(invocation -> {
                loadInvoked.countDown();
                try {
                    if (!allowLoadToContinue.await(1, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to resume load");
                    }
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new StorageException("Interrupted", exception);
                }
                return Optional.of(new LifestealProfile(uniqueId, storedHearts));
            });

            LifestealManager manager = new LifestealManager(
                    plugin,
                    profileRepository,
                    healthAttributeResolver,
                    defaultHearts,
                    minHearts,
                    maxHearts,
                    false,
                    0.0
            );

            CompletableFuture<LifestealProfile> future = manager.loadProfileAsync(uniqueId);
            assertTrue(loadInvoked.await(1, TimeUnit.SECONDS), "Storage load was not invoked");

            LifestealProfile inMemoryProfile = manager.getOrCreateProfile(uniqueId);
            double heartsGainedFromKill = 2.0;
            inMemoryProfile.addHearts(heartsGainedFromKill, maxHearts);
            assertTrue(inMemoryProfile.isDirty(), "Profile should be marked dirty after heart gain");

            allowLoadToContinue.countDown();
            LifestealProfile loadedProfile = future.get(1, TimeUnit.SECONDS);

            assertSame(inMemoryProfile, loadedProfile, "Loaded profile should be the cached instance");
            assertEquals(defaultHearts + heartsGainedFromKill, loadedProfile.getHearts(), 0.0001,
                    "Dirty cached hearts should be retained after load");
            assertTrue(loadedProfile.isDirty(), "Profile should remain dirty after retaining cached hearts");
        }
        finally {
            executor.shutdownNow();
        }
    }

    @Test
    void loadProfileAsyncSanitizesInvalidStoredHeartsBeforeCaching() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
            Logger logger = mock(Logger.class);
            when(plugin.getStorageExecutor()).thenReturn(executor);
            when(plugin.getLogger()).thenReturn(logger);

            ProfileRepository profileRepository = mock(ProfileRepository.class);
            HealthAttributeResolver healthAttributeResolver = mock(HealthAttributeResolver.class);

            UUID uniqueId = UUID.randomUUID();
            when(profileRepository.loadProfile(uniqueId))
                    .thenReturn(Optional.of(new LifestealProfile(uniqueId, Double.NaN)));

            LifestealManager manager = new LifestealManager(
                    plugin,
                    profileRepository,
                    healthAttributeResolver,
                    10.0,
                    1.0,
                    40.0,
                    false,
                    0.0
            );

            LifestealProfile profile = manager.loadProfileAsync(uniqueId).get(1, TimeUnit.SECONDS);
            assertEquals(10.0D, profile.getHearts());
            assertSame(profile, manager.getLoadedProfile(uniqueId).orElseThrow());
            verify(logger).warning(contains("Invalid hearts value for UUID " + uniqueId));
            verify(logger).warning(contains("from backend"));
        }
        finally {
            executor.shutdownNow();
        }
    }

    @Test
    void applyHeartsUpdatesAttributeAndHealthScaleWhenEnabled() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        ProfileRepository repository = mock(ProfileRepository.class);
        HealthAttributeResolver resolver = mock(HealthAttributeResolver.class);
        Player player = mock(Player.class);
        AttributeInstance attribute = mock(AttributeInstance.class);
        Attribute maxHealth = resolveMaxHealthAttributeConstant();

        when(resolver.resolveMaxHealthAttribute()).thenReturn(maxHealth);
        when(player.getAttribute(maxHealth)).thenReturn(attribute);
        when(player.getHealth()).thenReturn(40.0D);

        LifestealManager manager = new LifestealManager(
                plugin,
                repository,
                resolver,
                10.0,
                1.0,
                30.0,
                true,
                8.0
        );

        LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 18.0);
        manager.applyHearts(player, profile);

        verify(attribute).setBaseValue(36.0D);
        verify(player).setHealth(36.0D);
        verify(player).setHealthScaled(true);
        verify(player).setHealthScale(8.0);
    }

    @Test
    void applyHeartsDisablesHealthScaleWhenFeatureDisabled() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        ProfileRepository repository = mock(ProfileRepository.class);
        HealthAttributeResolver resolver = mock(HealthAttributeResolver.class);
        Player player = mock(Player.class);
        AttributeInstance attribute = mock(AttributeInstance.class);
        Attribute maxHealth = resolveMaxHealthAttributeConstant();

        when(resolver.resolveMaxHealthAttribute()).thenReturn(maxHealth);
        when(player.getAttribute(maxHealth)).thenReturn(attribute);
        when(player.getHealth()).thenReturn(4.0D);

        LifestealManager manager = new LifestealManager(
                plugin,
                repository,
                resolver,
                10.0,
                1.0,
                30.0,
                false,
                0.0
        );

        LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 5.0);
        manager.applyHearts(player, profile);

        verify(attribute).setBaseValue(10.0);
        verify(player).setHealthScaled(false);
    }

    @Test
    void saveAllSyncLogsErrorWhenRepositoryFails() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Logger logger = mock(Logger.class);
        ProfileRepository repository = mock(ProfileRepository.class);
        HealthAttributeResolver resolver = mock(HealthAttributeResolver.class);

        when(plugin.getLogger()).thenReturn(logger);
        doAnswer(invocation -> {
            throw new StorageException("save all failed");
        }).when(repository).saveProfiles(org.mockito.ArgumentMatchers.anyCollection());

        LifestealManager manager = new LifestealManager(
                plugin,
                repository,
                resolver,
                10.0,
                1.0,
                30.0,
                false,
                0.0
        );

        manager.getOrCreateProfile(UUID.randomUUID());
        manager.saveAllSync();

        verify(logger).severe(contains("Failed to save profiles"));
    }

    @Test
    void unloadRemovesCachedProfile() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        ProfileRepository repository = mock(ProfileRepository.class);
        HealthAttributeResolver resolver = mock(HealthAttributeResolver.class);

        LifestealManager manager = new LifestealManager(
                plugin,
                repository,
                resolver,
                10.0,
                1.0,
                30.0,
                false,
                0.0
        );

        UUID id = UUID.randomUUID();
        manager.getOrCreateProfile(id);
        assertTrue(manager.getLoadedProfile(id).isPresent());

        manager.unload(id);

        assertTrue(manager.getLoadedProfile(id).isEmpty());
    }

    private Attribute resolveMaxHealthAttributeConstant() {
        try {
            Object maxHealth = Attribute.class.getField("MAX_HEALTH").get(null);
            if (maxHealth instanceof Attribute attribute) {
                return attribute;
            }
        }
        catch (ReflectiveOperationException ignored) {
        }

        try {
            Object genericMaxHealth = Attribute.class.getField("GENERIC_MAX_HEALTH").get(null);
            if (genericMaxHealth instanceof Attribute attribute) {
                return attribute;
            }
        }
        catch (ReflectiveOperationException ignored) {
        }

        throw new IllegalStateException("No max-health attribute constant available");
    }
}
