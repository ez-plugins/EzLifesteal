package com.skyblockexp.ezlifesteal.storage.repository;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileRepositoryTest {

    @Test
    void saveProfilesDelegatesEachProfile() throws Exception {
        List<LifestealProfile> saved = new ArrayList<>();
        ProfileRepository repository = new CapturingProfileRepository(saved);
        LifestealProfile first = new LifestealProfile(UUID.randomUUID(), 10.0);
        LifestealProfile second = new LifestealProfile(UUID.randomUUID(), 5.0);

        repository.saveProfiles(List.of(first, second));

        assertEquals(List.of(first, second), saved);
    }

    @Test
    void saveProfilesHandlesEmptyCollection() throws Exception {
        List<LifestealProfile> saved = new ArrayList<>();
        ProfileRepository repository = new CapturingProfileRepository(saved);

        repository.saveProfiles(List.of());

        assertEquals(0, saved.size());
    }

    private record CapturingProfileRepository(List<LifestealProfile> saved) implements ProfileRepository {

        @Override
        public Optional<LifestealProfile> loadProfile(UUID uniqueId) {
            return Optional.empty();
        }

        @Override
        public void saveProfile(LifestealProfile profile) {
            saved.add(profile);
        }

        @Override
        public List<LifestealProfile> loadTopProfiles(int limit) {
            return List.of();
        }

        @Override
        public void resetAll(double defaultHearts) {
        }
    }
}
