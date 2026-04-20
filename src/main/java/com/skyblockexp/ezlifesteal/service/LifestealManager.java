package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class LifestealManager {
    private record ProfileSaveSnapshot(UUID uniqueId, double hearts, long revision) {
    }

    private final EzLifestealPlugin plugin;

    private final ProfileRepository profileRepository;

    private final HealthAttributeResolver healthAttributeResolver;

    private final Map<UUID, LifestealProfile> profiles = new ConcurrentHashMap<>();


    private final double defaultHearts;

    private final double minHearts;

    private final double maxHearts;

    private final boolean applyHealthScale;

    private final double healthScale;


    public LifestealManager(EzLifestealPlugin plugin,
                            ProfileRepository profileRepository,
                            HealthAttributeResolver healthAttributeResolver,
                            double defaultHearts,
                            double minHearts,
                            double maxHearts,
                            boolean applyHealthScale,
                            double healthScale) {
        this.plugin = plugin;
        this.profileRepository = profileRepository;
        this.healthAttributeResolver = healthAttributeResolver;
        this.defaultHearts = defaultHearts;
        this.minHearts = minHearts;
        this.maxHearts = maxHearts;
        this.applyHealthScale = applyHealthScale;
        this.healthScale = healthScale;
    }

    public CompletableFuture<LifestealProfile> loadProfileAsync(UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Optional<LifestealProfile> loaded = profileRepository.loadProfile(uniqueId);
                final double rawHearts = loaded.map(LifestealProfile::getHearts).orElse(defaultHearts);
                final double heartsFromStorage = HeartValueSanitizer.sanitize(rawHearts, minHearts, defaultHearts,
                        maxHearts);
                if (!Double.isFinite(rawHearts) || Double.compare(rawHearts, heartsFromStorage) != 0) {
                    final String backend =
                            profileRepository == null ? "unknown" : profileRepository.getClass().getSimpleName();
                    plugin.getLogger().warning("Invalid hearts value for UUID " + uniqueId + " from backend " + backend
                            + "; raw=" + rawHearts + ", sanitized=" + heartsFromStorage + '.');
                }
                return profiles.compute(uniqueId, (id, existing) -> {
                    if (existing != null) {
                        if (existing.isDirty()) {
                            return existing;
                        }
                        existing.overwriteHeartsFromStorage(heartsFromStorage);
                        return existing;
                    }
                    final LifestealProfile profile = new LifestealProfile(id, heartsFromStorage);
                    profile.overwriteHeartsFromStorage(heartsFromStorage);
                    return profile;
                });
            }
            catch (StorageException exception) {
                throw new CompletionException(exception);
            }
        }, plugin.getStorageExecutor());
    }

    public CompletableFuture<List<LifestealProfile>> loadTopProfilesAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return profileRepository.loadTopProfiles(limit);
            }
            catch (StorageException exception) {
                throw new CompletionException(exception);
            }
        }, plugin.getStorageExecutor());
    }

    public CompletableFuture<Void> saveProfileAsync(LifestealProfile profile) {
        final ProfileSaveSnapshot snapshot = captureSaveSnapshot(profile);
        return CompletableFuture.runAsync(() -> {
            try {
                profileRepository.saveProfile(new LifestealProfile(snapshot.uniqueId(), snapshot.hearts()));
                profile.markPersisted(snapshot.revision());
            }
            catch (StorageException exception) {
                throw new CompletionException(exception);
            }
        }, plugin.getStorageExecutor());
    }

    private ProfileSaveSnapshot captureSaveSnapshot(LifestealProfile profile) {
        return new ProfileSaveSnapshot(profile.getUniqueId(), profile.getHearts(), profile.getRevision());
    }

    public Optional<LifestealProfile> getLoadedProfile(UUID uniqueId) {
        return Optional.ofNullable(profiles.get(uniqueId));
    }

    public LifestealProfile getOrCreateProfile(UUID uniqueId) {
        return profiles.computeIfAbsent(uniqueId, id -> new LifestealProfile(id, defaultHearts));
    }

    public void applyHearts(Player player, LifestealProfile profile) {
        final double maxHealth = Math.max(2.0, Math.min(maxHearts * 2.0, profile.getHearts() * 2.0));
        final Attribute resolvedAttribute = healthAttributeResolver.resolveMaxHealthAttribute();
        var attribute = player.getAttribute(resolvedAttribute);
        if (attribute == null && resolvedAttribute != Attribute.MAX_HEALTH) {
            attribute = player.getAttribute(Attribute.MAX_HEALTH);
        }
        if (attribute != null) {
            attribute.setBaseValue(maxHealth);
        }
        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
        if (applyHealthScale && healthScale > 0) {
            player.setHealthScaled(true);
            player.setHealthScale(healthScale);
        }
        else {
            player.setHealthScaled(false);
        }
    }

    public double getDefaultHearts() {
        return defaultHearts;
    }

    public double getMinHearts() {
        return minHearts;
    }

    public double getMaxHearts() {
        return maxHearts;
    }

    public CompletableFuture<Void> resetAllHeartsAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                profileRepository.resetAll(defaultHearts);
                profiles.values().forEach(profile -> profile.setHearts(defaultHearts));
            }
            catch (StorageException exception) {
                throw new CompletionException(exception);
            }
        }, plugin.getStorageExecutor());
    }

    public EzLifestealPlugin getPlugin() {
        return plugin;
    }

    public void saveAllSync() {
        final Collection<LifestealProfile> snapshot = profiles.values();
        try {
            if (profileRepository == null) {
                return;
            }
            profileRepository.saveProfiles(snapshot);
        }
        catch (StorageException exception) {
            plugin.getLogger().severe("Failed to save profiles: " + exception.getMessage());
        }
    }

    public void unload(UUID uniqueId) {
        profiles.remove(uniqueId);
    }
}
