package com.skyblockexp.ezlifesteal.storage.repository;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {

    Optional<LifestealProfile> loadProfile(UUID uniqueId) throws StorageException;

    void saveProfile(LifestealProfile profile) throws StorageException;

    List<LifestealProfile> loadTopProfiles(int limit) throws StorageException;

    void resetAll(double defaultHearts) throws StorageException;

    default void saveProfiles(Collection<LifestealProfile> profiles) throws StorageException {
        for (LifestealProfile profile : profiles) {
            saveProfile(profile);
        }
    }
}
